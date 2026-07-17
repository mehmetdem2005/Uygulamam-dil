begin;

set local lock_timeout = '10s';
set local statement_timeout = '120s';

create extension if not exists pgcrypto;

do $$
begin
    create type public.app_visibility as enum ('private', 'unlisted', 'public');
exception
    when duplicate_object then null;
end
$$;

do $$
begin
    create type public.source_kind as enum ('youtube', 'pdf', 'text');
exception
    when duplicate_object then null;
end
$$;

do $$
begin
    create type public.user_role as enum ('user', 'admin');
exception
    when duplicate_object then null;
end
$$;

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    display_name text not null default '',
    avatar_url text,
    role public.user_role not null default 'user',
    teaching_language text not null default 'tr',
    target_language text not null default 'en',
    timezone text not null default 'Europe/Istanbul',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint profiles_display_name_length check (char_length(display_name) <= 80)
);

create table if not exists public.devices (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    installation_id text not null,
    platform text not null default 'android' check (platform in ('android')),
    app_version text,
    push_token text,
    last_seen_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, installation_id)
);

create table if not exists public.sources (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references public.profiles(id) on delete cascade,
    kind public.source_kind not null,
    title text not null,
    source_url text,
    storage_path text,
    visibility public.app_visibility not null default 'private',
    status text not null default 'pending'
        check (status in ('pending', 'processing', 'ready', 'failed', 'archived')),
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint sources_title_length check (char_length(title) between 1 and 240),
    constraint sources_metadata_object check (jsonb_typeof(metadata) = 'object'),
    constraint sources_locator check (
        (kind = 'youtube' and source_url is not null)
        or (kind = 'pdf' and storage_path is not null)
        or kind = 'text'
    )
);

create table if not exists public.source_revisions (
    id uuid primary key default gen_random_uuid(),
    source_id uuid not null references public.sources(id) on delete cascade,
    revision_number integer not null check (revision_number > 0),
    page_start integer,
    page_end integer,
    time_start_ms bigint,
    time_end_ms bigint,
    extracted_text text not null default '',
    checksum_sha256 text,
    extraction_metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    unique (source_id, revision_number),
    constraint source_revisions_page_range check (
        (page_start is null and page_end is null)
        or (page_start > 0 and page_end >= page_start)
    ),
    constraint source_revisions_time_range check (
        (time_start_ms is null and time_end_ms is null)
        or (time_start_ms >= 0 and time_end_ms > time_start_ms)
    ),
    constraint source_revisions_metadata_object check (jsonb_typeof(extraction_metadata) = 'object')
);

create table if not exists public.lesson_formats (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references public.profiles(id) on delete cascade,
    title text not null,
    description text not null default '',
    visibility public.app_visibility not null default 'private',
    forked_from_id uuid references public.lesson_formats(id) on delete set null,
    current_revision_number integer not null default 1 check (current_revision_number > 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint lesson_formats_title_length check (char_length(title) between 1 and 120),
    constraint lesson_formats_description_length check (char_length(description) <= 1000)
);

create table if not exists public.format_revisions (
    id uuid primary key default gen_random_uuid(),
    format_id uuid not null references public.lesson_formats(id) on delete cascade,
    revision_number integer not null check (revision_number > 0),
    instruction text not null,
    teaching_language text not null default 'tr',
    target_language text not null default 'en',
    proficiency_level text,
    card_fields jsonb not null default '[]'::jsonb,
    generation_config jsonb not null default '{}'::jsonb,
    playback_config jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    unique (format_id, revision_number),
    constraint format_revisions_instruction_length check (char_length(instruction) between 10 and 4000),
    constraint format_revisions_card_fields_array check (jsonb_typeof(card_fields) = 'array'),
    constraint format_revisions_generation_object check (jsonb_typeof(generation_config) = 'object'),
    constraint format_revisions_playback_object check (jsonb_typeof(playback_config) = 'object')
);

create table if not exists public.lessons (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references public.profiles(id) on delete cascade,
    source_revision_id uuid not null references public.source_revisions(id) on delete restrict,
    format_revision_id uuid not null references public.format_revisions(id) on delete restrict,
    title text not null,
    visibility public.app_visibility not null default 'private',
    status text not null default 'draft'
        check (status in ('draft', 'queued', 'generating', 'ready', 'published', 'failed', 'archived')),
    total_block_count integer not null default 0 check (total_block_count >= 0),
    generated_block_count integer not null default 0 check (generated_block_count >= 0),
    published_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint lessons_title_length check (char_length(title) between 1 and 240),
    constraint lessons_block_count check (generated_block_count <= total_block_count or total_block_count = 0),
    constraint lessons_publish_state check (
        (status = 'published' and visibility = 'public' and published_at is not null)
        or status <> 'published'
    )
);

create table if not exists public.lesson_blocks (
    id uuid primary key default gen_random_uuid(),
    lesson_id uuid not null references public.lessons(id) on delete cascade,
    block_index integer not null check (block_index >= 0),
    target_text text not null default '',
    translation text,
    pronunciation text,
    explanation text,
    content jsonb not null default '{}'::jsonb,
    content_hash text,
    generation_status text not null default 'ready'
        check (generation_status in ('pending', 'generating', 'ready', 'failed')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (lesson_id, block_index),
    constraint lesson_blocks_content_object check (jsonb_typeof(content) = 'object')
);

create table if not exists public.lesson_progress (
    user_id uuid not null references public.profiles(id) on delete cascade,
    lesson_id uuid not null references public.lessons(id) on delete cascade,
    last_block_index integer not null default 0 check (last_block_index >= 0),
    completed_block_indexes integer[] not null default '{}'::integer[],
    playback_position_ms bigint not null default 0 check (playback_position_ms >= 0),
    completed_at timestamptz,
    updated_at timestamptz not null default now(),
    primary key (user_id, lesson_id)
);

create table if not exists public.audio_assets (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references public.profiles(id) on delete cascade,
    lesson_block_id uuid references public.lesson_blocks(id) on delete cascade,
    voice_id text not null,
    speech_rate numeric(5,2) not null default 1.00 check (speech_rate between 0.25 and 3.00),
    storage_path text not null,
    content_hash text not null,
    character_count integer not null default 0 check (character_count >= 0),
    duration_ms bigint check (duration_ms is null or duration_ms >= 0),
    status text not null default 'pending'
        check (status in ('pending', 'generating', 'ready', 'failed')),
    created_at timestamptz not null default now(),
    unique (content_hash, voice_id, speech_rate)
);

create table if not exists public.download_entries (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    device_id uuid references public.devices(id) on delete cascade,
    lesson_id uuid not null references public.lessons(id) on delete cascade,
    state text not null default 'queued'
        check (state in ('queued', 'downloading', 'ready', 'failed', 'removed')),
    bytes_downloaded bigint not null default 0 check (bytes_downloaded >= 0),
    total_bytes bigint check (total_bytes is null or total_bytes >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, device_id, lesson_id)
);

create table if not exists public.job_runs (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    lesson_id uuid references public.lessons(id) on delete cascade,
    job_type text not null check (job_type in ('extract_source', 'generate_lesson', 'generate_audio', 'publish_lesson')),
    status text not null default 'queued'
        check (status in ('queued', 'running', 'succeeded', 'failed', 'cancel_requested', 'canceled')),
    idempotency_key text not null unique,
    input jsonb not null default '{}'::jsonb,
    progress jsonb not null default '{}'::jsonb,
    error_code text,
    error_message text,
    requested_at timestamptz not null default now(),
    started_at timestamptz,
    finished_at timestamptz,
    updated_at timestamptz not null default now(),
    constraint job_runs_input_object check (jsonb_typeof(input) = 'object'),
    constraint job_runs_progress_object check (jsonb_typeof(progress) = 'object')
);

create table if not exists public.provider_usage (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references public.profiles(id) on delete set null,
    job_id uuid references public.job_runs(id) on delete set null,
    provider text not null check (provider in ('deepseek', 'edge_tts', 'youtube', 'pdf')),
    model text,
    provider_request_id text,
    input_tokens bigint not null default 0 check (input_tokens >= 0),
    output_tokens bigint not null default 0 check (output_tokens >= 0),
    tts_characters bigint not null default 0 check (tts_characters >= 0),
    latency_ms bigint check (latency_ms is null or latency_ms >= 0),
    success boolean not null default true,
    error_code text,
    estimated_cost_micro_usd bigint not null default 0 check (estimated_cost_micro_usd >= 0),
    created_at timestamptz not null default now()
);

create table if not exists public.cost_ledger (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references public.profiles(id) on delete set null,
    usage_id uuid references public.provider_usage(id) on delete set null,
    category text not null check (category in ('llm_input', 'llm_output', 'tts', 'storage', 'other')),
    amount_micro_usd bigint not null check (amount_micro_usd >= 0),
    currency text not null default 'USD' check (currency = 'USD'),
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    constraint cost_ledger_metadata_object check (jsonb_typeof(metadata) = 'object')
);

create table if not exists public.subscriptions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    store text not null default 'google_play' check (store = 'google_play'),
    product_id text not null,
    purchase_token_hash text not null unique,
    status text not null
        check (status in ('pending', 'trial', 'active', 'grace_period', 'paused', 'canceled', 'expired', 'revoked')),
    started_at timestamptz,
    expires_at timestamptz,
    auto_renewing boolean not null default false,
    last_verified_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.entitlements (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    code text not null,
    source text not null check (source in ('default', 'subscription', 'promotion', 'admin')),
    quota jsonb not null default '{}'::jsonb,
    starts_at timestamptz not null default now(),
    expires_at timestamptz,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint entitlements_quota_object check (jsonb_typeof(quota) = 'object'),
    unique (user_id, code, starts_at)
);

create table if not exists public.analytics_daily (
    metric_date date not null,
    metric_key text not null,
    dimension_key text not null default 'all',
    value numeric(24,4) not null default 0,
    metadata jsonb not null default '{}'::jsonb,
    updated_at timestamptz not null default now(),
    primary key (metric_date, metric_key, dimension_key),
    constraint analytics_daily_metadata_object check (jsonb_typeof(metadata) = 'object')
);

create table if not exists public.admin_audit_log (
    id bigint generated always as identity primary key,
    actor_id uuid references public.profiles(id) on delete set null,
    action text not null,
    target_type text,
    target_id text,
    metadata jsonb not null default '{}'::jsonb,
    ip_address inet,
    created_at timestamptz not null default now(),
    constraint admin_audit_log_metadata_object check (jsonb_typeof(metadata) = 'object')
);

create table if not exists public.app_schema_versions (
    version text primary key,
    description text not null,
    applied_at timestamptz not null default now()
);

create index if not exists devices_user_last_seen_idx on public.devices (user_id, last_seen_at desc);
create index if not exists sources_owner_updated_idx on public.sources (owner_id, updated_at desc);
create index if not exists sources_public_updated_idx on public.sources (updated_at desc) where visibility = 'public';
create index if not exists source_revisions_source_idx on public.source_revisions (source_id, revision_number desc);
create index if not exists lesson_formats_owner_updated_idx on public.lesson_formats (owner_id, updated_at desc);
create index if not exists lesson_formats_public_updated_idx on public.lesson_formats (updated_at desc) where visibility = 'public';
create index if not exists format_revisions_format_idx on public.format_revisions (format_id, revision_number desc);
create index if not exists lessons_owner_updated_idx on public.lessons (owner_id, updated_at desc);
create index if not exists lessons_public_published_idx on public.lessons (published_at desc) where status = 'published' and visibility = 'public';
create index if not exists lesson_blocks_lesson_idx on public.lesson_blocks (lesson_id, block_index);
create index if not exists lesson_progress_user_updated_idx on public.lesson_progress (user_id, updated_at desc);
create index if not exists audio_assets_owner_created_idx on public.audio_assets (owner_id, created_at desc);
create index if not exists downloads_user_state_idx on public.download_entries (user_id, state, updated_at desc);
create index if not exists job_runs_user_status_idx on public.job_runs (user_id, status, requested_at desc);
create index if not exists provider_usage_created_idx on public.provider_usage (created_at desc);
create index if not exists provider_usage_user_created_idx on public.provider_usage (user_id, created_at desc);
create index if not exists cost_ledger_created_idx on public.cost_ledger (created_at desc);
create index if not exists subscriptions_user_status_idx on public.subscriptions (user_id, status, expires_at desc);
create index if not exists entitlements_user_active_idx on public.entitlements (user_id, active, expires_at);
create index if not exists admin_audit_log_created_idx on public.admin_audit_log (created_at desc);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.profiles
        where id = auth.uid() and role = 'admin'
    );
$$;

create or replace function public.can_read_source(p_source_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.sources
        where id = p_source_id
          and (owner_id = auth.uid() or visibility = 'public' or public.is_admin())
    );
$$;

create or replace function public.can_read_format(p_format_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.lesson_formats
        where id = p_format_id
          and (owner_id = auth.uid() or visibility = 'public' or public.is_admin())
    );
$$;

create or replace function public.can_read_lesson(p_lesson_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.lessons
        where id = p_lesson_id
          and (
              owner_id = auth.uid()
              or (visibility = 'public' and status = 'published')
              or public.is_admin()
          )
    );
$$;

create or replace function public.owns_lesson(p_lesson_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.lessons
        where id = p_lesson_id
          and (owner_id = auth.uid() or public.is_admin())
    );
$$;

create or replace function public.protect_profile_role()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    if new.role is distinct from old.role and not public.is_admin() then
        raise exception 'Only administrators can change profile roles';
    end if;
    return new;
end;
$$;

create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.profiles (id, display_name)
    values (
        new.id,
        left(coalesce(new.raw_user_meta_data ->> 'display_name', new.raw_user_meta_data ->> 'full_name', ''), 80)
    )
    on conflict (id) do nothing;

    insert into public.entitlements (user_id, code, source, quota)
    values (
        new.id,
        'free',
        'default',
        jsonb_build_object('monthly_blocks', 40, 'monthly_tts_characters', 20000)
    )
    on conflict (user_id, code, starts_at) do nothing;

    return new;
end;
$$;

create or replace function public.app_health()
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
    select jsonb_build_object(
        'status', 'ok',
        'schema_version', '20260717190000',
        'checked_at', now()
    );
$$;

revoke all on function public.is_admin() from public;
revoke all on function public.can_read_source(uuid) from public;
revoke all on function public.can_read_format(uuid) from public;
revoke all on function public.can_read_lesson(uuid) from public;
revoke all on function public.owns_lesson(uuid) from public;
revoke all on function public.app_health() from public;

grant execute on function public.is_admin() to authenticated, service_role;
grant execute on function public.can_read_source(uuid) to anon, authenticated, service_role;
grant execute on function public.can_read_format(uuid) to anon, authenticated, service_role;
grant execute on function public.can_read_lesson(uuid) to anon, authenticated, service_role;
grant execute on function public.owns_lesson(uuid) to authenticated, service_role;
grant execute on function public.app_health() to anon, authenticated, service_role;

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at before update on public.profiles
for each row execute function public.set_updated_at();

drop trigger if exists profiles_protect_role on public.profiles;
create trigger profiles_protect_role before update on public.profiles
for each row execute function public.protect_profile_role();

drop trigger if exists devices_set_updated_at on public.devices;
create trigger devices_set_updated_at before update on public.devices
for each row execute function public.set_updated_at();

drop trigger if exists sources_set_updated_at on public.sources;
create trigger sources_set_updated_at before update on public.sources
for each row execute function public.set_updated_at();

drop trigger if exists lesson_formats_set_updated_at on public.lesson_formats;
create trigger lesson_formats_set_updated_at before update on public.lesson_formats
for each row execute function public.set_updated_at();

drop trigger if exists lessons_set_updated_at on public.lessons;
create trigger lessons_set_updated_at before update on public.lessons
for each row execute function public.set_updated_at();

drop trigger if exists lesson_blocks_set_updated_at on public.lesson_blocks;
create trigger lesson_blocks_set_updated_at before update on public.lesson_blocks
for each row execute function public.set_updated_at();

drop trigger if exists lesson_progress_set_updated_at on public.lesson_progress;
create trigger lesson_progress_set_updated_at before update on public.lesson_progress
for each row execute function public.set_updated_at();

drop trigger if exists download_entries_set_updated_at on public.download_entries;
create trigger download_entries_set_updated_at before update on public.download_entries
for each row execute function public.set_updated_at();

drop trigger if exists job_runs_set_updated_at on public.job_runs;
create trigger job_runs_set_updated_at before update on public.job_runs
for each row execute function public.set_updated_at();

drop trigger if exists subscriptions_set_updated_at on public.subscriptions;
create trigger subscriptions_set_updated_at before update on public.subscriptions
for each row execute function public.set_updated_at();

drop trigger if exists entitlements_set_updated_at on public.entitlements;
create trigger entitlements_set_updated_at before update on public.entitlements
for each row execute function public.set_updated_at();

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_auth_user();

alter table public.profiles enable row level security;
alter table public.devices enable row level security;
alter table public.sources enable row level security;
alter table public.source_revisions enable row level security;
alter table public.lesson_formats enable row level security;
alter table public.format_revisions enable row level security;
alter table public.lessons enable row level security;
alter table public.lesson_blocks enable row level security;
alter table public.lesson_progress enable row level security;
alter table public.audio_assets enable row level security;
alter table public.download_entries enable row level security;
alter table public.job_runs enable row level security;
alter table public.provider_usage enable row level security;
alter table public.cost_ledger enable row level security;
alter table public.subscriptions enable row level security;
alter table public.entitlements enable row level security;
alter table public.analytics_daily enable row level security;
alter table public.admin_audit_log enable row level security;
alter table public.app_schema_versions enable row level security;

drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles for select to authenticated
using (
    id = auth.uid()
    or public.is_admin()
    or exists (
        select 1 from public.lessons
        where owner_id = profiles.id and visibility = 'public' and status = 'published'
    )
);

drop policy if exists profiles_update on public.profiles;
create policy profiles_update on public.profiles for update to authenticated
using (id = auth.uid() or public.is_admin())
with check (id = auth.uid() or public.is_admin());

drop policy if exists devices_owner_all on public.devices;
create policy devices_owner_all on public.devices for all to authenticated
using (user_id = auth.uid() or public.is_admin())
with check (user_id = auth.uid() or public.is_admin());

drop policy if exists sources_select on public.sources;
create policy sources_select on public.sources for select to anon, authenticated
using (owner_id = auth.uid() or visibility = 'public' or public.is_admin());

drop policy if exists sources_insert on public.sources;
create policy sources_insert on public.sources for insert to authenticated
with check (owner_id = auth.uid() or public.is_admin());

drop policy if exists sources_update on public.sources;
create policy sources_update on public.sources for update to authenticated
using (owner_id = auth.uid() or public.is_admin())
with check (owner_id = auth.uid() or public.is_admin());

drop policy if exists sources_delete on public.sources;
create policy sources_delete on public.sources for delete to authenticated
using (owner_id = auth.uid() or public.is_admin());

drop policy if exists source_revisions_select on public.source_revisions;
create policy source_revisions_select on public.source_revisions for select to anon, authenticated
using (public.can_read_source(source_id));

drop policy if exists source_revisions_owner_write on public.source_revisions;
create policy source_revisions_owner_write on public.source_revisions for all to authenticated
using (
    exists (select 1 from public.sources where id = source_id and (owner_id = auth.uid() or public.is_admin()))
)
with check (
    exists (select 1 from public.sources where id = source_id and (owner_id = auth.uid() or public.is_admin()))
);

drop policy if exists lesson_formats_select on public.lesson_formats;
create policy lesson_formats_select on public.lesson_formats for select to anon, authenticated
using (owner_id = auth.uid() or visibility = 'public' or public.is_admin());

drop policy if exists lesson_formats_insert on public.lesson_formats;
create policy lesson_formats_insert on public.lesson_formats for insert to authenticated
with check (owner_id = auth.uid() or public.is_admin());

drop policy if exists lesson_formats_update on public.lesson_formats;
create policy lesson_formats_update on public.lesson_formats for update to authenticated
using (owner_id = auth.uid() or public.is_admin())
with check (owner_id = auth.uid() or public.is_admin());

drop policy if exists lesson_formats_delete on public.lesson_formats;
create policy lesson_formats_delete on public.lesson_formats for delete to authenticated
using (owner_id = auth.uid() or public.is_admin());

drop policy if exists format_revisions_select on public.format_revisions;
create policy format_revisions_select on public.format_revisions for select to anon, authenticated
using (public.can_read_format(format_id));

drop policy if exists format_revisions_owner_write on public.format_revisions;
create policy format_revisions_owner_write on public.format_revisions for all to authenticated
using (
    exists (select 1 from public.lesson_formats where id = format_id and (owner_id = auth.uid() or public.is_admin()))
)
with check (
    exists (select 1 from public.lesson_formats where id = format_id and (owner_id = auth.uid() or public.is_admin()))
);

drop policy if exists lessons_select on public.lessons;
create policy lessons_select on public.lessons for select to anon, authenticated
using (
    owner_id = auth.uid()
    or (visibility = 'public' and status = 'published')
    or public.is_admin()
);

drop policy if exists lessons_insert on public.lessons;
create policy lessons_insert on public.lessons for insert to authenticated
with check (owner_id = auth.uid() or public.is_admin());

drop policy if exists lessons_update on public.lessons;
create policy lessons_update on public.lessons for update to authenticated
using (owner_id = auth.uid() or public.is_admin())
with check (owner_id = auth.uid() or public.is_admin());

drop policy if exists lessons_delete on public.lessons;
create policy lessons_delete on public.lessons for delete to authenticated
using (owner_id = auth.uid() or public.is_admin());

drop policy if exists lesson_blocks_select on public.lesson_blocks;
create policy lesson_blocks_select on public.lesson_blocks for select to anon, authenticated
using (public.can_read_lesson(lesson_id));

drop policy if exists lesson_blocks_owner_write on public.lesson_blocks;
create policy lesson_blocks_owner_write on public.lesson_blocks for all to authenticated
using (public.owns_lesson(lesson_id))
with check (public.owns_lesson(lesson_id));

drop policy if exists lesson_progress_owner_all on public.lesson_progress;
create policy lesson_progress_owner_all on public.lesson_progress for all to authenticated
using (user_id = auth.uid() or public.is_admin())
with check (
    (user_id = auth.uid() and public.can_read_lesson(lesson_id))
    or public.is_admin()
);

drop policy if exists audio_assets_select on public.audio_assets;
create policy audio_assets_select on public.audio_assets for select to authenticated
using (
    owner_id = auth.uid()
    or public.is_admin()
    or exists (
        select 1 from public.lesson_blocks
        where id = lesson_block_id and public.can_read_lesson(lesson_id)
    )
);

drop policy if exists audio_assets_owner_write on public.audio_assets;
create policy audio_assets_owner_write on public.audio_assets for all to authenticated
using (owner_id = auth.uid() or public.is_admin())
with check (owner_id = auth.uid() or public.is_admin());

drop policy if exists downloads_owner_all on public.download_entries;
create policy downloads_owner_all on public.download_entries for all to authenticated
using (user_id = auth.uid() or public.is_admin())
with check (
    (user_id = auth.uid() and public.can_read_lesson(lesson_id))
    or public.is_admin()
);

drop policy if exists job_runs_owner_select on public.job_runs;
create policy job_runs_owner_select on public.job_runs for select to authenticated
using (user_id = auth.uid() or public.is_admin());

drop policy if exists job_runs_owner_insert on public.job_runs;
create policy job_runs_owner_insert on public.job_runs for insert to authenticated
with check (user_id = auth.uid() or public.is_admin());

drop policy if exists job_runs_owner_update on public.job_runs;
create policy job_runs_owner_update on public.job_runs for update to authenticated
using (user_id = auth.uid() or public.is_admin())
with check (user_id = auth.uid() or public.is_admin());

drop policy if exists provider_usage_owner_select on public.provider_usage;
create policy provider_usage_owner_select on public.provider_usage for select to authenticated
using (user_id = auth.uid() or public.is_admin());

drop policy if exists cost_ledger_owner_select on public.cost_ledger;
create policy cost_ledger_owner_select on public.cost_ledger for select to authenticated
using (user_id = auth.uid() or public.is_admin());

drop policy if exists subscriptions_owner_select on public.subscriptions;
create policy subscriptions_owner_select on public.subscriptions for select to authenticated
using (user_id = auth.uid() or public.is_admin());

drop policy if exists entitlements_owner_select on public.entitlements;
create policy entitlements_owner_select on public.entitlements for select to authenticated
using (user_id = auth.uid() or public.is_admin());

drop policy if exists analytics_admin_all on public.analytics_daily;
create policy analytics_admin_all on public.analytics_daily for all to authenticated
using (public.is_admin())
with check (public.is_admin());

drop policy if exists admin_audit_admin_select on public.admin_audit_log;
create policy admin_audit_admin_select on public.admin_audit_log for select to authenticated
using (public.is_admin());

drop policy if exists schema_versions_admin_select on public.app_schema_versions;
create policy schema_versions_admin_select on public.app_schema_versions for select to authenticated
using (public.is_admin());

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'source-files',
    'source-files',
    false,
    52428800,
    array['application/pdf']::text[]
)
on conflict (id) do update set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'lesson-audio',
    'lesson-audio',
    false,
    20971520,
    array['audio/mpeg', 'audio/ogg', 'audio/wav', 'audio/webm']::text[]
)
on conflict (id) do update set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists source_files_owner_select on storage.objects;
create policy source_files_owner_select on storage.objects for select to authenticated
using (
    bucket_id = 'source-files'
    and ((storage.foldername(name))[1] = auth.uid()::text or public.is_admin())
);

drop policy if exists source_files_owner_insert on storage.objects;
create policy source_files_owner_insert on storage.objects for insert to authenticated
with check (
    bucket_id = 'source-files'
    and ((storage.foldername(name))[1] = auth.uid()::text or public.is_admin())
);

drop policy if exists source_files_owner_update on storage.objects;
create policy source_files_owner_update on storage.objects for update to authenticated
using (
    bucket_id = 'source-files'
    and ((storage.foldername(name))[1] = auth.uid()::text or public.is_admin())
)
with check (
    bucket_id = 'source-files'
    and ((storage.foldername(name))[1] = auth.uid()::text or public.is_admin())
);

drop policy if exists source_files_owner_delete on storage.objects;
create policy source_files_owner_delete on storage.objects for delete to authenticated
using (
    bucket_id = 'source-files'
    and ((storage.foldername(name))[1] = auth.uid()::text or public.is_admin())
);

drop policy if exists lesson_audio_owner_select on storage.objects;
create policy lesson_audio_owner_select on storage.objects for select to authenticated
using (
    bucket_id = 'lesson-audio'
    and ((storage.foldername(name))[1] = auth.uid()::text or public.is_admin())
);

drop policy if exists lesson_audio_owner_insert on storage.objects;
create policy lesson_audio_owner_insert on storage.objects for insert to authenticated
with check (
    bucket_id = 'lesson-audio'
    and ((storage.foldername(name))[1] = auth.uid()::text or public.is_admin())
);

drop policy if exists lesson_audio_owner_update on storage.objects;
create policy lesson_audio_owner_update on storage.objects for update to authenticated
using (
    bucket_id = 'lesson-audio'
    and ((storage.foldername(name))[1] = auth.uid()::text or public.is_admin())
)
with check (
    bucket_id = 'lesson-audio'
    and ((storage.foldername(name))[1] = auth.uid()::text or public.is_admin())
);

drop policy if exists lesson_audio_owner_delete on storage.objects;
create policy lesson_audio_owner_delete on storage.objects for delete to authenticated
using (
    bucket_id = 'lesson-audio'
    and ((storage.foldername(name))[1] = auth.uid()::text or public.is_admin())
);

insert into public.app_schema_versions (version, description)
values ('20260717190000', 'Initial application schema, RLS policies, storage buckets, and health RPC')
on conflict (version) do nothing;

commit;
