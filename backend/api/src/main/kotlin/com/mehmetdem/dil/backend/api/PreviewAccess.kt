package com.mehmetdem.dil.backend.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PreviewUnauthorizedException(message: String = "Önizleme oturumu geçersiz veya süresi dolmuş.") : RuntimeException(message)
class PreviewRateLimitException(message: String) : RuntimeException(message)

class PreviewTokenService(
    secret: String,
    private val json: Json,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sessionDurationSeconds: Long = 12 * 60 * 60,
) {
    private val key = secret.toByteArray(Charsets.UTF_8)

    init {
        require(key.size >= 32) { "PREVIEW_TOKEN_SECRET en az 32 karakter olmalıdır." }
    }

    fun issue(installationId: String): PreviewSession {
        validateInstallationId(installationId)
        val expiresAt = clock() / 1_000 + sessionDurationSeconds
        val payload = PreviewTokenPayload(installationId, expiresAt)
        val encodedPayload = encoder.encodeToString(json.encodeToString(payload).toByteArray())
        val signature = encoder.encodeToString(sign(encodedPayload))
        return PreviewSession("$encodedPayload.$signature", expiresAt)
    }

    fun verify(token: String): String {
        val parts = token.split('.')
        if (parts.size != 2) throw PreviewUnauthorizedException()
        val actual = runCatching { decoder.decode(parts[1]) }.getOrElse { throw PreviewUnauthorizedException() }
        val expected = sign(parts[0])
        if (!MessageDigest.isEqual(actual, expected)) throw PreviewUnauthorizedException()
        val payload = runCatching {
            json.decodeFromString<PreviewTokenPayload>(String(decoder.decode(parts[0]), Charsets.UTF_8))
        }.getOrElse { throw PreviewUnauthorizedException() }
        validateInstallationId(payload.installationId)
        if (payload.expiresAtEpochSeconds <= clock() / 1_000) throw PreviewUnauthorizedException()
        return payload.installationId
    }

    private fun sign(value: String): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(value.toByteArray(Charsets.UTF_8))
    }

    private fun validateInstallationId(value: String) {
        if (!value.matches(Regex("[A-Za-z0-9_-]{8,100}"))) throw PreviewUnauthorizedException("Kurulum kimliği geçersiz.")
    }

    private companion object {
        val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
        val decoder: Base64.Decoder = Base64.getUrlDecoder()
    }
}

@Serializable
private data class PreviewTokenPayload(val installationId: String, val expiresAtEpochSeconds: Long)

data class PreviewSession(val accessToken: String, val expiresAtEpochSeconds: Long)

@Serializable
data class UploadedPdf(
    val id: String,
    val ownerInstallationId: String,
    val originalFileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val createdAtEpochMillis: Long,
)

class SourceUploadStore(
    root: Path,
    private val json: Json,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val directory = root.resolve("uploads").also(Files::createDirectories)

    @Synchronized
    fun storePdf(ownerInstallationId: String, originalFileName: String, input: InputStream, maxBytes: Long): UploadedPdf {
        val id = UUID.randomUUID().toString()
        val destination = directory.resolve("$id.pdf")
        val temporary = Files.createTempFile(directory, "$id-", ".upload")
        var size = 0L
        val header = ByteArray(5)
        var headerBytes = 0
        try {
            Files.newOutputStream(temporary).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    size += read
                    if (size > maxBytes) throw IllegalArgumentException("PDF dosyası ${maxBytes / 1024 / 1024} MB sınırını aşıyor.")
                    if (headerBytes < header.size) {
                        val copied = minOf(read, header.size - headerBytes)
                        buffer.copyInto(header, headerBytes, 0, copied)
                        headerBytes += copied
                    }
                    output.write(buffer, 0, read)
                }
            }
            require(size > 0 && headerBytes == header.size && String(header, Charsets.US_ASCII) == "%PDF-") {
                "Yüklenen dosya geçerli bir PDF değil."
            }
            moveAtomically(temporary, destination)
            val upload = UploadedPdf(
                id = id,
                ownerInstallationId = ownerInstallationId,
                originalFileName = originalFileName.sanitizedFileName(),
                filePath = destination.toString(),
                sizeBytes = size,
                createdAtEpochMillis = clock().coerceAtLeast(1),
            )
            Files.writeString(directory.resolve("$id.json"), json.encodeToString(upload))
            return upload
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    @Synchronized
    fun requireOwned(uploadId: String, ownerInstallationId: String): UploadedPdf {
        require(runCatching { UUID.fromString(uploadId) }.isSuccess) { "PDF yükleme kimliği geçersiz." }
        val metadata = directory.resolve("$uploadId.json")
        val upload = if (Files.isRegularFile(metadata)) {
            runCatching { json.decodeFromString<UploadedPdf>(Files.readString(metadata)) }.getOrNull()
        } else null
        if (upload == null || upload.ownerInstallationId != ownerInstallationId || !Files.isRegularFile(Path.of(upload.filePath))) {
            throw PreviewUnauthorizedException("PDF yüklemesi bulunamadı.")
        }
        return upload
    }

    private fun moveAtomically(source: Path, destination: Path) {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

private fun String.sanitizedFileName(): String = substringAfterLast('/').substringAfterLast('\\')
    .replace(Regex("[^A-Za-z0-9._() -]"), "_")
    .take(180)
    .ifBlank { "belge.pdf" }
