begin;

set local lock_timeout = '10s';
set local statement_timeout = '120s';

alter table public.format_revisions
    add column if not exists teaching_mode text not null default 'language',
    add column if not exists compiled_schema jsonb,
    add column if not exists schema_sha256 text,
    add column if not exists copied_from_revision_id uuid references public.format_revisions(id) on delete set null;

alter table public.format_revisions
    drop constraint if exists format_revisions_teaching_mode_check;

alter table public.format_revisions
    add constraint format_revisions_teaching_mode_check
    check (teaching_mode in ('language', 'explain', 'summary', 'translate', 'custom'));

alter table public.format_revisions
    drop constraint if exists format_revisions_compiled_schema_object;

alter table public.format_revisions
    add constraint format_revisions_compiled_schema_object
    check (compiled_schema is null or jsonb_typeof(compiled_schema) = 'object');

alter table public.format_revisions
    drop constraint if exists format_revisions_schema_sha256_check;

alter table public.format_revisions
    add constraint format_revisions_schema_sha256_check
    check (schema_sha256 is null or schema_sha256 ~ '^[0-9a-f]{64}$');

create or replace function public.prevent_format_revision_mutation()
returns trigger
language plpgsql
security invoker
set search_path = public
as $$
begin
    raise exception 'Format revisions are immutable; create a new revision instead'
        using errcode = '55000';
end;
$$;

drop trigger if exists format_revisions_immutable on public.format_revisions;
create trigger format_revisions_immutable
before update on public.format_revisions
for each row execute function public.prevent_format_revision_mutation();

create or replace function public.copy_lesson_format(
    source_format_id uuid,
    new_title text
)
returns uuid
language plpgsql
security invoker
set search_path = public
as $$
declare
    source_format public.lesson_formats%rowtype;
    source_revision public.format_revisions%rowtype;
    new_format_id uuid;
begin
    select * into source_format
    from public.lesson_formats
    where id = source_format_id
      and (owner_id = auth.uid() or visibility = 'public' or public.is_admin());

    if not found then
        raise exception 'Format not found or inaccessible' using errcode = '42501';
    end if;

    select * into source_revision
    from public.format_revisions
    where format_id = source_format_id
      and revision_number = source_format.current_revision_number;

    insert into public.lesson_formats (owner_id, title, description, visibility, forked_from_id, current_revision_number)
    values (auth.uid(), left(trim(new_title), 120), source_format.description, 'private', source_format.id, 1)
    returning id into new_format_id;

    insert into public.format_revisions (
        format_id, revision_number, instruction, teaching_language, target_language,
        proficiency_level, card_fields, generation_config, playback_config,
        teaching_mode, compiled_schema, schema_sha256, copied_from_revision_id
    ) values (
        new_format_id, 1, source_revision.instruction, source_revision.teaching_language,
        source_revision.target_language, source_revision.proficiency_level,
        source_revision.card_fields, source_revision.generation_config,
        source_revision.playback_config, source_revision.teaching_mode,
        source_revision.compiled_schema, source_revision.schema_sha256, source_revision.id
    );

    return new_format_id;
end;
$$;

revoke all on function public.copy_lesson_format(uuid, text) from public;
grant execute on function public.copy_lesson_format(uuid, text) to authenticated;

insert into public.app_schema_versions (version, description)
values ('20260718090000', 'Immutable format revisions, compiled JSON Schema identity, and safe format copy function')
on conflict (version) do nothing;

create or replace function public.app_health()
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
    select jsonb_build_object(
        'status', 'ok',
        'schema_version', coalesce((select max(version) from public.app_schema_versions), 'unknown'),
        'checked_at', now()
    );
$$;

commit;
