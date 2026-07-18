package com.mehmetdem.dil.backend.api

import com.mehmetdem.dil.backend.application.FileLessonJobStore
import com.mehmetdem.dil.backend.application.GeneratedLessonBlock
import com.mehmetdem.dil.backend.application.LessonJobCreateCommand
import com.mehmetdem.dil.backend.application.LessonJobFormatFieldSpec
import com.mehmetdem.dil.backend.application.LessonJobFormatSpec
import com.mehmetdem.dil.backend.application.LessonJobOrchestrator
import com.mehmetdem.dil.backend.application.LessonJobRecord
import com.mehmetdem.dil.backend.application.LessonJobSourceKind
import com.mehmetdem.dil.backend.application.LessonJobSourceSpec
import com.mehmetdem.dil.backend.application.LessonJobState
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.cacheControl
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Route.previewJobRoutes(
    json: Json,
    tokenService: PreviewTokenService,
    uploadStore: SourceUploadStore,
    jobStore: FileLessonJobStore,
    orchestrator: LessonJobOrchestrator,
    dailyJobLimit: Int,
    maxPdfBytes: Long,
) {
    post("/v1/preview/sessions") {
        val request = call.receive<PreviewSessionRequest>()
        val session = tokenService.issue(request.installationId)
        call.respond(PreviewSessionResponse(session.accessToken, session.expiresAtEpochSeconds))
    }

    post("/v1/preview/pdf-uploads") {
        val owner = call.previewOwner(tokenService)
        var uploaded: UploadedPdf? = null
        call.receiveMultipart(formFieldLimit = maxPdfBytes + 1_024L).forEachPart { part ->
            try {
                if (part is PartData.FileItem && part.name == "file" && uploaded == null) {
                    part.provider().toInputStream().use { input ->
                        uploaded = uploadStore.storePdf(owner, part.originalFileName ?: "belge.pdf", input, maxPdfBytes)
                    }
                }
            } finally {
                part.dispose()
            }
        }
        val result = requireNotNull(uploaded) { "PDF dosyası gereklidir." }
        call.respond(HttpStatusCode.Created, PdfUploadResponse(result.id, result.originalFileName, result.sizeBytes))
    }

    post("/v1/preview/lesson-jobs") {
        val owner = call.previewOwner(tokenService)
        val idempotencyKey = call.request.header("Idempotency-Key")?.trim()
            ?: throw IllegalArgumentException("Idempotency-Key başlığı gereklidir.")
        val request = call.receive<CreateLessonJobRequest>()
        val existing = jobStore.findByIdempotency(owner, idempotencyKey)
        if (existing == null) {
            val dayStart = System.currentTimeMillis() - 24 * 60 * 60 * 1_000L
            if (jobStore.countCreatedSince(owner, dayStart) >= dailyJobLimit) {
                throw PreviewRateLimitException("24 saatlik önizleme ders sınırına ulaştınız.")
            }
        }
        val command = request.toCommand(owner, idempotencyKey, uploadStore)
        val job = orchestrator.create(command)
        call.respond(if (existing == null) HttpStatusCode.Accepted else HttpStatusCode.OK, job.toResponse())
    }

    get("/v1/preview/lesson-jobs/{jobId}") {
        val owner = call.previewOwner(tokenService)
        val job = orchestrator.getOwned(call.parameters["jobId"].orEmpty(), owner)
        call.respond(job.toResponse())
    }

    get("/v1/preview/lesson-jobs/{jobId}/events") {
        val owner = call.previewOwner(tokenService)
        val jobId = call.parameters["jobId"].orEmpty()
        orchestrator.getOwned(jobId, owner)
        call.response.cacheControl(CacheControl.NoCache(null))
        call.response.header("X-Accel-Buffering", "no")
        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
            var lastVersion = -1L
            while (true) {
                val job = orchestrator.getOwned(jobId, owner)
                if (job.version != lastVersion) {
                    write("id: ${job.version}\n")
                    write("data: ${json.encodeToString(job.toResponse())}\n\n")
                    flush()
                    lastVersion = job.version
                }
                if (job.state in terminalStates) break
                delay(500)
            }
        }
    }

    post("/v1/preview/lesson-jobs/{jobId}/pause") {
        val owner = call.previewOwner(tokenService)
        call.respond(orchestrator.pause(call.parameters["jobId"].orEmpty(), owner).toResponse())
    }
    post("/v1/preview/lesson-jobs/{jobId}/resume") {
        val owner = call.previewOwner(tokenService)
        call.respond(orchestrator.resume(call.parameters["jobId"].orEmpty(), owner).toResponse())
    }
    post("/v1/preview/lesson-jobs/{jobId}/retry") {
        val owner = call.previewOwner(tokenService)
        call.respond(orchestrator.retry(call.parameters["jobId"].orEmpty(), owner).toResponse())
    }
    post("/v1/preview/lesson-jobs/{jobId}/cancel") {
        val owner = call.previewOwner(tokenService)
        call.respond(orchestrator.cancel(call.parameters["jobId"].orEmpty(), owner).toResponse())
    }
}

private fun io.ktor.server.application.ApplicationCall.previewOwner(tokenService: PreviewTokenService): String {
    val header = request.header(HttpHeaders.Authorization) ?: throw PreviewUnauthorizedException()
    if (!header.startsWith("Bearer ", ignoreCase = true)) throw PreviewUnauthorizedException()
    return tokenService.verify(header.substringAfter(' ').trim())
}

@Serializable
private data class PreviewSessionRequest(val installationId: String, val appVersion: String? = null)

@Serializable
private data class PreviewSessionResponse(val accessToken: String, val expiresAtEpochSeconds: Long)

@Serializable
private data class PdfUploadResponse(val uploadId: String, val fileName: String, val sizeBytes: Long)

@Serializable
private data class CreateLessonJobRequest(
    val localLessonId: String,
    val source: CreateSourceRequest,
    val format: CreateFormatRequest,
    val qualityMode: Boolean = false,
    val continuousRequests: Boolean = true,
) {
    fun toCommand(owner: String, idempotencyKey: String, uploadStore: SourceUploadStore): LessonJobCreateCommand {
        val kind = LessonJobSourceKind.valueOf(source.kind.uppercase())
        val sourceSpec = when (kind) {
            LessonJobSourceKind.YOUTUBE -> LessonJobSourceSpec(
                kind = kind,
                locator = requireNotNull(source.videoId) { "YouTube video kimliği gereklidir." },
                displayName = source.displayName,
                startMillis = requireNotNull(source.startMillis) { "Başlangıç zamanı gereklidir." },
                endMillisExclusive = requireNotNull(source.endMillisExclusive) { "Bitiş zamanı gereklidir." },
            )
            LessonJobSourceKind.PDF -> {
                val upload = uploadStore.requireOwned(requireNotNull(source.uploadId) { "PDF yüklemesi gereklidir." }, owner)
                LessonJobSourceSpec(
                    kind = kind,
                    locator = upload.filePath,
                    displayName = source.displayName.ifBlank { upload.originalFileName },
                    startPage = requireNotNull(source.startPage) { "Başlangıç sayfası gereklidir." },
                    endPageInclusive = requireNotNull(source.endPageInclusive) { "Bitiş sayfası gereklidir." },
                )
            }
        }
        return LessonJobCreateCommand(
            ownerInstallationId = owner,
            idempotencyKey = idempotencyKey,
            localLessonId = localLessonId,
            source = sourceSpec,
            format = format.toSpec(),
            qualityMode = qualityMode,
            continuousRequests = continuousRequests,
        )
    }
}

@Serializable
private data class CreateSourceRequest(
    val kind: String,
    val displayName: String,
    val videoId: String? = null,
    val uploadId: String? = null,
    val startMillis: Long? = null,
    val endMillisExclusive: Long? = null,
    val startPage: Int? = null,
    val endPageInclusive: Int? = null,
)

@Serializable
private data class CreateFormatRequest(
    val formatId: String,
    val revision: Int,
    val title: String,
    val instruction: String,
    val teachingMode: String,
    val teachingLanguage: String,
    val targetLanguage: String,
    val learnerLevel: String,
    val fields: List<CreateFormatFieldRequest>,
    val totalBlockCount: Int,
    val blocksPerRequest: Int,
    val requestIntervalSeconds: Int,
    val cardWidthFraction: Float,
) {
    fun toSpec() = LessonJobFormatSpec(
        formatId, revision, title, instruction, teachingMode, teachingLanguage, targetLanguage,
        learnerLevel, fields.map(CreateFormatFieldRequest::toSpec), totalBlockCount, blocksPerRequest,
        requestIntervalSeconds, cardWidthFraction,
    )
}

@Serializable
private data class CreateFormatFieldRequest(
    val key: String,
    val label: String,
    val kind: String,
    val required: Boolean,
    val visible: Boolean,
    val speakable: Boolean,
    val position: Int,
) {
    fun toSpec() = LessonJobFormatFieldSpec(key, label, kind, required, visible, speakable, position)
}

@Serializable
private data class LessonJobResponse(
    val jobId: String,
    val localLessonId: String,
    val state: String,
    val completedBlockCount: Int,
    val totalBlockCount: Int,
    val blocks: List<LessonBlockResponse>,
    val metrics: LessonMetricsResponse,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val version: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Serializable
private data class LessonBlockResponse(
    val index: Int,
    val title: String? = null,
    val sourceText: String? = null,
    val targetText: String,
    val translation: String? = null,
    val pronunciation: String? = null,
    val explanation: String? = null,
    val fieldValues: Map<String, String>,
)

@Serializable
private data class LessonMetricsResponse(
    val providerRequestCount: Int,
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long,
    val providerLatencyMillis: Long,
    val estimatedCostMicroUsd: Long,
)

private fun LessonJobRecord.toResponse() = LessonJobResponse(
    jobId = id,
    localLessonId = localLessonId,
    state = state.name.lowercase(),
    completedBlockCount = blocks.size,
    totalBlockCount = format.totalBlockCount,
    blocks = blocks.map(GeneratedLessonBlock::toResponse),
    metrics = LessonMetricsResponse(
        metrics.providerRequestCount, metrics.inputTokens, metrics.outputTokens, metrics.totalTokens,
        metrics.providerLatencyMillis, metrics.estimatedCostMicroUsd,
    ),
    errorCode = errorCode,
    errorMessage = errorMessage,
    version = version,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private fun GeneratedLessonBlock.toResponse() = LessonBlockResponse(
    index, title, sourceText, targetText, translation, pronunciation, explanation, fieldValues,
)

private val terminalStates = setOf(LessonJobState.READY, LessonJobState.FAILED, LessonJobState.CANCELLED)
