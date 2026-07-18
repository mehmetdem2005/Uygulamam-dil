package com.mehmetdem.dil.core.network

import com.mehmetdem.dil.core.model.ContentRange
import com.mehmetdem.dil.core.model.LessonBlock
import com.mehmetdem.dil.core.model.LessonGenerationMetrics
import com.mehmetdem.dil.core.model.LessonJobState
import com.mehmetdem.dil.core.model.SourceKind
import com.mehmetdem.dil.core.model.StoredLesson
import com.mehmetdem.dil.core.model.YouTubeVideoIdParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class LessonApiException(
    val statusCode: Int,
    val errorCode: String,
    override val message: String,
) : RuntimeException(message)

data class RemoteLessonJob(
    val jobId: String,
    val localLessonId: String,
    val state: LessonJobState,
    val completedBlockCount: Int,
    val totalBlockCount: Int,
    val blocks: List<LessonBlock>,
    val metrics: LessonGenerationMetrics,
    val errorCode: String?,
    val errorMessage: String?,
    val version: Long,
)

class LessonJobApi(
    baseUrl: String,
    private val json: Json = defaultJson(),
    private val client: HttpClient = defaultClient(json),
    private val clock: () -> Long = System::currentTimeMillis,
) : AutoCloseable {
    private val endpoint = baseUrl.trimEnd('/')
    private val sessionMutex = Mutex()
    @Volatile private var session: SessionResponse? = null

    suspend fun uploadPdf(
        installationId: String,
        fileName: String,
        bytes: ByteArray,
    ): String = authorized(installationId) { token ->
        client.post("$endpoint/v1/preview/pdf-uploads") {
            bearerAuth(token)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                                val safeName = fileName.replace(Regex("[^A-Za-z0-9._() -]"), "_").take(180).ifBlank { "belge.pdf" }
                                append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$safeName\"")
                            },
                        )
                    },
                ),
            )
        }.decode<PdfUploadResponse>().uploadId
    }

    suspend fun createJob(
        installationId: String,
        lesson: StoredLesson,
        pdfUploadId: String? = null,
    ): RemoteLessonJob = authorized(installationId) { token ->
        client.post("$endpoint/v1/preview/lesson-jobs") {
            bearerAuth(token)
            header("Idempotency-Key", lesson.id)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(CreateLessonJobRequest.fromLesson(lesson, pdfUploadId))
        }.decode<LessonJobResponse>().toDomain()
    }

    suspend fun getJob(installationId: String, jobId: String): RemoteLessonJob = authorized(installationId) { token ->
        client.get("$endpoint/v1/preview/lesson-jobs/$jobId") { bearerAuth(token) }
            .decode<LessonJobResponse>()
            .toDomain()
    }

    suspend fun pause(installationId: String, jobId: String) = control(installationId, jobId, "pause")
    suspend fun resume(installationId: String, jobId: String) = control(installationId, jobId, "resume")
    suspend fun retry(installationId: String, jobId: String) = control(installationId, jobId, "retry")
    suspend fun cancel(installationId: String, jobId: String) = control(installationId, jobId, "cancel")

    private suspend fun control(installationId: String, jobId: String, action: String): RemoteLessonJob =
        authorized(installationId) { token ->
            client.post("$endpoint/v1/preview/lesson-jobs/$jobId/$action") { bearerAuth(token) }
                .decode<LessonJobResponse>()
                .toDomain()
        }

    private suspend fun <T> authorized(installationId: String, request: suspend (String) -> T): T {
        val token = sessionToken(installationId)
        return try {
            request(token)
        } catch (failure: LessonApiException) {
            if (failure.statusCode != HttpStatusCode.Unauthorized.value) throw failure
            session = null
            request(sessionToken(installationId, force = true))
        }
    }

    private suspend fun sessionToken(installationId: String, force: Boolean = false): String = sessionMutex.withLock {
        val current = session
        if (!force && current != null && current.expiresAtEpochSeconds * 1_000 > clock() + 60_000) return current.accessToken
        client.post("$endpoint/v1/preview/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(SessionRequest(installationId, "0.4.0"))
        }.decode<SessionResponse>().also { session = it }.accessToken
    }

    private suspend inline fun <reified T> HttpResponse.decode(): T {
        val payload = bodyAsText()
        if (!status.isSuccess()) {
            val error = runCatching { json.decodeFromString<ApiErrorResponse>(payload) }.getOrNull()
            throw LessonApiException(status.value, error?.code ?: "http_${status.value}", error?.message ?: "Sunucu isteği başarısız oldu.")
        }
        return runCatching { json.decodeFromString<T>(payload) }.getOrElse {
            throw LessonApiException(status.value, "invalid_server_response", "Sunucu yanıtı okunamadı.")
        }
    }

    override fun close() = client.close()

    companion object {
        private fun defaultJson() = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

        private fun defaultClient(json: Json) = HttpClient(CIO) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 20_000
                requestTimeoutMillis = 150_000
                socketTimeoutMillis = 150_000
            }
            expectSuccess = false
        }
    }
}

@Serializable
private data class SessionRequest(val installationId: String, val appVersion: String)

@Serializable
private data class SessionResponse(val accessToken: String, val expiresAtEpochSeconds: Long)

@Serializable
private data class PdfUploadResponse(val uploadId: String, val fileName: String, val sizeBytes: Long)

@Serializable
private data class ApiErrorResponse(val code: String, val message: String)

@Serializable
private data class CreateLessonJobRequest(
    val localLessonId: String,
    val source: CreateSourceRequest,
    val format: CreateFormatRequest,
    val qualityMode: Boolean,
    val continuousRequests: Boolean,
) {
    companion object {
        fun fromLesson(lesson: StoredLesson, pdfUploadId: String?): CreateLessonJobRequest {
            val selection = lesson.config.source
            val range = selection.range
            val source = when (selection.kind) {
                SourceKind.YOUTUBE -> CreateSourceRequest(
                    kind = "youtube",
                    displayName = selection.displayName,
                    videoId = requireNotNull(YouTubeVideoIdParser.parse(selection.locator)) { "YouTube bağlantısı geçersiz." },
                    startMillis = (range as ContentRange.Time).startMillis,
                    endMillisExclusive = range.endMillisExclusive,
                )
                SourceKind.PDF -> CreateSourceRequest(
                    kind = "pdf",
                    displayName = selection.displayName,
                    uploadId = requireNotNull(pdfUploadId) { "PDF yükleme kimliği eksik." },
                    startPage = (range as ContentRange.Pages).startPage,
                    endPageInclusive = range.endPageInclusive,
                )
            }
            val format = lesson.config.format
            return CreateLessonJobRequest(
                localLessonId = lesson.id,
                source = source,
                format = CreateFormatRequest(
                    formatId = format.formatId,
                    revision = format.revision,
                    title = format.title,
                    instruction = format.instruction,
                    teachingMode = format.teachingMode.name.lowercase(),
                    teachingLanguage = format.teachingLanguage,
                    targetLanguage = format.targetLanguage,
                    learnerLevel = format.learnerLevel,
                    fields = format.orderedFields().map { field ->
                        CreateFormatFieldRequest(
                            key = field.key,
                            label = field.label,
                            kind = field.type.name.lowercase(),
                            required = field.required,
                            visible = field.visible,
                            speakable = field.speakable,
                            position = field.position,
                        )
                    },
                    totalBlockCount = format.totalBlockCount,
                    blocksPerRequest = format.blocksPerRequest,
                    requestIntervalSeconds = format.requestIntervalSeconds,
                    cardWidthFraction = format.cardWidthFraction,
                ),
                qualityMode = format.teachingMode.name.lowercase() in setOf("explain", "summary", "custom"),
                continuousRequests = format.continuousRequests,
            )
        }
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
)

@Serializable
private data class CreateFormatFieldRequest(
    val key: String,
    val label: String,
    val kind: String,
    val required: Boolean,
    val visible: Boolean,
    val speakable: Boolean,
    val position: Int,
)

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
) {
    fun toDomain() = RemoteLessonJob(
        jobId = jobId,
        localLessonId = localLessonId,
        state = when (state.lowercase()) {
            "created" -> LessonJobState.CREATED
            "ingesting" -> LessonJobState.INGESTING
            "generating" -> LessonJobState.GENERATING
            "paused" -> LessonJobState.PAUSED
            "ready" -> LessonJobState.READY
            "failed" -> LessonJobState.FAILED
            "cancelled" -> LessonJobState.CANCELLED
            else -> LessonJobState.FAILED
        },
        completedBlockCount = completedBlockCount,
        totalBlockCount = totalBlockCount,
        blocks = blocks.map(LessonBlockResponse::toDomain),
        metrics = metrics.toDomain(),
        errorCode = errorCode,
        errorMessage = errorMessage,
        version = version,
    )
}

@Serializable
private data class LessonBlockResponse(
    val index: Int,
    val title: String? = null,
    val sourceText: String? = null,
    val targetText: String,
    val translation: String? = null,
    val pronunciation: String? = null,
    val explanation: String? = null,
    val fieldValues: Map<String, String> = emptyMap(),
) {
    fun toDomain() = LessonBlock(index, title, sourceText, targetText, translation, pronunciation, explanation, fieldValues = fieldValues)
}

@Serializable
private data class LessonMetricsResponse(
    val providerRequestCount: Int = 0,
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val totalTokens: Long = 0,
    val providerLatencyMillis: Long = 0,
    val estimatedCostMicroUsd: Long = 0,
) {
    fun toDomain() = LessonGenerationMetrics(providerRequestCount, inputTokens, outputTokens, totalTokens, providerLatencyMillis, estimatedCostMicroUsd)
}
