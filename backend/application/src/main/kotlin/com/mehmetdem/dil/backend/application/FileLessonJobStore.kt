package com.mehmetdem.dil.backend.application

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

class FileLessonJobStore(
    root: Path,
    private val json: Json = defaultJson(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val jobsDirectory = root.resolve("jobs").also(Files::createDirectories)

    @Synchronized
    fun createOrGet(command: LessonJobCreateCommand): LessonJobRecord {
        validate(command)
        val commandHash = sha256(json.encodeToString(command))
        val existing = all().firstOrNull {
            it.ownerInstallationId == command.ownerInstallationId && it.idempotencyKey == command.idempotencyKey
        }
        if (existing != null) {
            if (existing.commandHash != commandHash) throw IdempotencyConflictException()
            return existing
        }

        val now = clock().coerceAtLeast(1)
        return LessonJobRecord(
            id = UUID.randomUUID().toString(),
            commandHash = commandHash,
            ownerInstallationId = command.ownerInstallationId,
            idempotencyKey = command.idempotencyKey,
            localLessonId = command.localLessonId,
            source = command.source,
            format = command.format,
            qualityMode = command.qualityMode,
            continuousRequests = command.continuousRequests,
            state = LessonJobState.CREATED,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        ).also(::write)
    }

    @Synchronized
    fun get(jobId: String): LessonJobRecord? = read(pathFor(jobId))

    @Synchronized
    fun findByIdempotency(ownerInstallationId: String, idempotencyKey: String): LessonJobRecord? = all().firstOrNull {
        it.ownerInstallationId == ownerInstallationId && it.idempotencyKey == idempotencyKey
    }

    @Synchronized
    fun requireOwned(jobId: String, ownerInstallationId: String): LessonJobRecord {
        val job = get(jobId) ?: throw LessonJobNotFoundException(jobId)
        if (job.ownerInstallationId != ownerInstallationId) throw LessonJobOwnershipException()
        return job
    }

    @Synchronized
    fun update(jobId: String, transform: (LessonJobRecord) -> LessonJobRecord): LessonJobRecord {
        val current = get(jobId) ?: throw LessonJobNotFoundException(jobId)
        val transformed = transform(current)
        require(transformed.id == current.id) { "Ders işi kimliği değiştirilemez." }
        val updated = transformed.copy(
            version = current.version + 1,
            updatedAtEpochMillis = maxOf(clock(), current.createdAtEpochMillis),
        )
        write(updated)
        return updated
    }

    @Synchronized
    fun recoverable(): List<LessonJobRecord> = all().filter {
        it.state in setOf(LessonJobState.CREATED, LessonJobState.INGESTING, LessonJobState.GENERATING)
    }

    @Synchronized
    fun countCreatedSince(ownerInstallationId: String, epochMillis: Long): Int = all().count {
        it.ownerInstallationId == ownerInstallationId && it.createdAtEpochMillis >= epochMillis
    }

    private fun all(): List<LessonJobRecord> = Files.list(jobsDirectory).use { paths ->
        paths.iterator().asSequence()
            .filter { it.fileName.toString().endsWith(".json") }
            .mapNotNull(::read)
            .toList()
    }

    private fun pathFor(jobId: String): Path {
        require(runCatching { UUID.fromString(jobId) }.isSuccess) { "Ders işi kimliği geçersiz." }
        return jobsDirectory.resolve("$jobId.json")
    }

    private fun read(path: Path): LessonJobRecord? {
        if (!Files.isRegularFile(path)) return null
        return runCatching { json.decodeFromString<LessonJobRecord>(Files.readString(path)) }.getOrNull()
    }

    private fun write(job: LessonJobRecord) {
        val destination = pathFor(job.id)
        val temporary = Files.createTempFile(jobsDirectory, "${job.id}-", ".tmp")
        try {
            Files.writeString(temporary, json.encodeToString(job))
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun validate(command: LessonJobCreateCommand) {
        require(command.ownerInstallationId.matches(Regex("[A-Za-z0-9_-]{8,100}"))) { "Kurulum kimliği geçersiz." }
        require(command.idempotencyKey.length in 8..160) { "Idempotency anahtarı geçersiz." }
        require(command.localLessonId.length in 1..160) { "Yerel ders kimliği geçersiz." }
        require(command.source.displayName.trim().length in 1..240) { "Kaynak adı geçersiz." }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    companion object {
        fun defaultJson() = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
