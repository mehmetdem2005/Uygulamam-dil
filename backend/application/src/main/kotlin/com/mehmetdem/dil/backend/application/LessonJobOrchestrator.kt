package com.mehmetdem.dil.backend.application

import com.mehmetdem.dil.backend.domain.FormatFieldDefinition
import com.mehmetdem.dil.backend.domain.FormatFieldKind
import com.mehmetdem.dil.backend.domain.FormatTeachingMode
import com.mehmetdem.dil.backend.domain.LessonFormatDefinition
import com.mehmetdem.dil.backend.domain.LessonGenerationPlan
import com.mehmetdem.dil.backend.domain.ModelGateway
import com.mehmetdem.dil.backend.domain.ModelStreamEvent
import com.mehmetdem.dil.backend.domain.PdfIngestionRequest
import com.mehmetdem.dil.backend.domain.SourceIngestionGateway
import com.mehmetdem.dil.backend.domain.YouTubeIngestionRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

data class LessonJobLimits(
    val maxProviderAttemptsPerBatch: Int = 3,
    val providerTimeoutSeconds: Int = 120,
    val maxTotalTokens: Long = 250_000,
    val maxEstimatedCostMicroUsd: Long = 5_000_000,
    val inputPriceMicroUsdPerMillionTokens: Long = 0,
    val outputPriceMicroUsdPerMillionTokens: Long = 0,
)

class LessonJobOrchestrator(
    private val store: FileLessonJobStore,
    private val sourceGateway: SourceIngestionGateway,
    private val modelGateway: ModelGateway,
    private val formatCompiler: FormatSchemaCompiler = FormatSchemaCompiler(),
    private val requestPlanner: BlockRequestPlanner = BlockRequestPlanner(),
    private val json: Json = FileLessonJobStore.defaultJson(),
    private val limits: LessonJobLimits = LessonJobLimits(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : AutoCloseable {
    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun create(command: LessonJobCreateCommand): LessonJobRecord = store.createOrGet(command).also(::start)

    fun getOwned(jobId: String, ownerInstallationId: String): LessonJobRecord = store.requireOwned(jobId, ownerInstallationId)

    fun recoverIncompleteJobs() {
        store.recoverable().forEach(::start)
    }

    fun pause(jobId: String, ownerInstallationId: String): LessonJobRecord = store.update(jobId) { job ->
        requireOwner(job, ownerInstallationId)
        require(job.state in setOf(LessonJobState.CREATED, LessonJobState.INGESTING, LessonJobState.GENERATING)) {
            "Bu iş duraklatılamaz."
        }
        job.copy(state = LessonJobState.PAUSED, errorCode = null, errorMessage = null)
    }

    fun resume(jobId: String, ownerInstallationId: String): LessonJobRecord = store.update(jobId) { job ->
        requireOwner(job, ownerInstallationId)
        require(job.state == LessonJobState.PAUSED) { "Yalnız duraklatılmış iş devam ettirilebilir." }
        job.copy(state = LessonJobState.GENERATING, errorCode = null, errorMessage = null)
    }.also(::start)

    fun retry(jobId: String, ownerInstallationId: String): LessonJobRecord = store.update(jobId) { job ->
        requireOwner(job, ownerInstallationId)
        require(job.state == LessonJobState.FAILED) { "Yalnız başarısız iş yeniden denenebilir." }
        job.copy(state = LessonJobState.CREATED, errorCode = null, errorMessage = null)
    }.also(::start)

    fun cancel(jobId: String, ownerInstallationId: String): LessonJobRecord = store.update(jobId) { job ->
        requireOwner(job, ownerInstallationId)
        require(job.state !in setOf(LessonJobState.READY, LessonJobState.CANCELLED)) { "Bu iş durdurulamaz." }
        job.copy(state = LessonJobState.CANCELLED, errorCode = null, errorMessage = null)
    }

    private fun start(job: LessonJobRecord) {
        if (job.state in setOf(LessonJobState.READY, LessonJobState.CANCELLED, LessonJobState.FAILED, LessonJobState.PAUSED)) return
        activeJobs.computeIfAbsent(job.id) { jobId ->
            scope.launch {
                try {
                    process(jobId)
                } catch (failure: Throwable) {
                    runCatching {
                        store.update(jobId) { current ->
                            if (current.state == LessonJobState.CANCELLED) current else current.copy(
                                state = LessonJobState.FAILED,
                                errorCode = failure.errorCode(),
                                errorMessage = failure.safeMessage(),
                            )
                        }
                    }
                } finally {
                    activeJobs.remove(jobId)
                }
            }
        }
    }

    private suspend fun process(jobId: String) {
        var job = store.get(jobId) ?: throw LessonJobNotFoundException(jobId)
        if (job.state == LessonJobState.CREATED) {
            job = store.update(jobId) { it.copy(state = LessonJobState.INGESTING, errorCode = null, errorMessage = null) }
        }
        checkControl(job)

        val sourceSegments = when (job.source.kind) {
            LessonJobSourceKind.YOUTUBE -> sourceGateway.ingestYouTube(
                YouTubeIngestionRequest(
                    videoId = job.source.locator,
                    startMillis = requireNotNull(job.source.startMillis),
                    endMillisExclusive = requireNotNull(job.source.endMillisExclusive),
                    preferredLanguages = listOf(job.format.teachingLanguage.languageCode(), job.format.targetLanguage.languageCode()),
                ),
            )
            LessonJobSourceKind.PDF -> sourceGateway.ingestPdf(
                PdfIngestionRequest(
                    filePath = job.source.locator,
                    startPage = requireNotNull(job.source.startPage),
                    endPageInclusive = requireNotNull(job.source.endPageInclusive),
                ),
            )
        }

        val format = job.format.toDomain()
        val compiled = formatCompiler.compile(format)
        val batches = requestPlanner.plan(
            sourceSegments = sourceSegments,
            plan = LessonGenerationPlan(
                lessonId = job.id,
                formatRevisionId = "${format.formatId}:${format.revision}:${compiled.sha256}",
                instruction = format.instruction,
                teachingLanguage = format.teachingLanguage,
                targetLanguage = format.targetLanguage,
                firstBlockIndex = 0,
                totalBlockCount = format.totalBlockCount,
                blocksPerRequest = format.blocksPerRequest,
                thinkingEnabled = job.qualityMode,
                outputSchemaJson = compiled.schemaJson,
            ),
        )
        store.update(jobId) { current ->
            if (current.state in setOf(LessonJobState.CANCELLED, LessonJobState.PAUSED)) current
            else current.copy(state = LessonJobState.GENERATING)
        }

        batches.forEachIndexed { batchIndex, batch ->
            awaitRunnable(jobId)
            val current = store.get(jobId) ?: throw LessonJobNotFoundException(jobId)
            val requiredIndexes = (batch.firstBlockIndex until batch.firstBlockIndex + batch.blockCount).toSet()
            if (current.blocks.map(GeneratedLessonBlock::index).containsAll(requiredIndexes)) return@forEachIndexed

            val result = generateBatch(batch.modelRequest, batch.firstBlockIndex, batch.blockCount, format)
            val afterCall = store.get(jobId) ?: throw LessonJobNotFoundException(jobId)
            val metrics = afterCall.metrics + result.metrics
            enforceCostLimits(metrics)
            if (afterCall.state == LessonJobState.CANCELLED) return

            val updated = store.update(jobId) { latest ->
                val merged = (latest.blocks + result.blocks)
                    .associateBy(GeneratedLessonBlock::index)
                    .toSortedMap()
                    .values
                    .take(format.totalBlockCount)
                latest.copy(
                    state = when {
                        latest.state == LessonJobState.CANCELLED -> LessonJobState.CANCELLED
                        merged.size == format.totalBlockCount -> LessonJobState.READY
                        latest.state == LessonJobState.PAUSED -> LessonJobState.PAUSED
                        else -> LessonJobState.GENERATING
                    },
                    blocks = merged,
                    metrics = latest.metrics + result.metrics,
                    errorCode = null,
                    errorMessage = null,
                )
            }
            if (updated.state == LessonJobState.READY) return

            val hasMore = batchIndex < batches.lastIndex
            if (hasMore && !updated.continuousRequests) {
                store.update(jobId) { it.copy(state = LessonJobState.PAUSED) }
                return
            }
            if (hasMore) delay(updated.format.requestIntervalSeconds.seconds)
        }

        store.update(jobId) { current ->
            if (current.state in setOf(LessonJobState.CANCELLED, LessonJobState.PAUSED)) current
            else current.copy(state = LessonJobState.READY)
        }
    }

    private suspend fun awaitRunnable(jobId: String) {
        while (true) {
            val job = store.get(jobId) ?: throw LessonJobNotFoundException(jobId)
            when (job.state) {
                LessonJobState.PAUSED -> delay(300)
                LessonJobState.CANCELLED -> throw JobCancelledException()
                LessonJobState.FAILED, LessonJobState.READY -> throw JobStoppedException()
                else -> return
            }
        }
    }

    private fun checkControl(job: LessonJobRecord) {
        if (job.state == LessonJobState.CANCELLED) throw JobCancelledException()
        if (job.state == LessonJobState.PAUSED) throw JobStoppedException()
    }

    private suspend fun generateBatch(
        request: com.mehmetdem.dil.backend.domain.ModelGenerationRequest,
        firstIndex: Int,
        count: Int,
        format: LessonFormatDefinition,
    ): BatchResult {
        var lastFailure: Throwable? = null
        repeat(limits.maxProviderAttemptsPerBatch) { attempt ->
            val started = System.nanoTime()
            val content = StringBuilder()
            var usage = LessonJobMetrics(providerRequestCount = 1)
            try {
                withTimeout(limits.providerTimeoutSeconds.seconds) {
                    modelGateway.stream(request).collect { event ->
                        when (event) {
                            is ModelStreamEvent.ContentDelta -> content.append(event.text)
                            is ModelStreamEvent.Usage -> usage = usage.copy(
                                inputTokens = max(usage.inputTokens, event.inputTokens),
                                outputTokens = max(usage.outputTokens, event.outputTokens),
                                totalTokens = max(usage.totalTokens, event.totalTokens),
                            )
                            is ModelStreamEvent.ReasoningDelta, ModelStreamEvent.Completed -> Unit
                        }
                    }
                }
                usage = usage.copy(
                    providerLatencyMillis = (System.nanoTime() - started) / 1_000_000,
                    estimatedCostMicroUsd = estimateCost(usage.inputTokens, usage.outputTokens),
                )
                return BatchResult(parseBlocks(content.toString(), firstIndex, count, format), usage)
            } catch (failure: Throwable) {
                lastFailure = failure
                if (attempt + 1 < limits.maxProviderAttemptsPerBatch) delay((300L shl attempt).coerceAtMost(2_000L))
            }
        }
        throw lastFailure ?: LessonJobOutputException("Model yanıtı alınamadı.")
    }

    private fun parseBlocks(payload: String, firstIndex: Int, count: Int, format: LessonFormatDefinition): List<GeneratedLessonBlock> {
        val cleaned = payload.trim().removeSurrounding("```json", "```").trim()
        val root = runCatching { json.parseToJsonElement(cleaned) as? JsonObject }.getOrNull()
            ?: throw LessonJobOutputException("Model geçerli JSON döndürmedi.")
        val rawBlocks = root["blocks"] as? JsonArray ?: throw LessonJobOutputException("Model yanıtında blocks dizisi yok.")
        if (rawBlocks.size != count) throw LessonJobOutputException("Model $count yerine ${rawBlocks.size} kart döndürdü.")
        val expectedIndexes = (firstIndex until firstIndex + count).toSet()
        val blocks = rawBlocks.map { element -> parseBlock(element, format) }
        if (blocks.map(GeneratedLessonBlock::index).toSet() != expectedIndexes) {
            throw LessonJobOutputException("Model kart sıra numaralarını yanlış döndürdü.")
        }
        return blocks.sortedBy(GeneratedLessonBlock::index)
    }

    private fun parseBlock(element: JsonElement, format: LessonFormatDefinition): GeneratedLessonBlock {
        val objectValue = element as? JsonObject ?: throw LessonJobOutputException("Kart nesne biçiminde değil.")
        val index = (objectValue["index"] as? JsonPrimitive)?.intOrNull
            ?: throw LessonJobOutputException("Kart sıra numarası eksik.")
        val values = format.fields.associate { field ->
            val raw = objectValue[field.key]
            if (field.required && raw == null) throw LessonJobOutputException("${field.label} alanı eksik.")
            field.key to raw.asDisplayText().take(8_000)
        }.filterValues(String::isNotBlank)
        val target = sequenceOf(
            values["target_text"], values["source_text"], values["translation"], values.values.firstOrNull(),
        ).filterNotNull().firstOrNull(String::isNotBlank)
            ?: throw LessonJobOutputException("Kartın gösterilecek metni boş.")
        return GeneratedLessonBlock(
            index = index,
            title = values["title"],
            sourceText = values["source_text"],
            targetText = target,
            translation = values["translation"],
            pronunciation = values["pronunciation"],
            explanation = values["explanation"],
            fieldValues = values,
        )
    }

    private fun enforceCostLimits(metrics: LessonJobMetrics) {
        require(metrics.totalTokens <= limits.maxTotalTokens) { "Ders token maliyet sınırına ulaştı." }
        require(metrics.estimatedCostMicroUsd <= limits.maxEstimatedCostMicroUsd) { "Ders maliyet sınırına ulaştı." }
    }

    private fun estimateCost(inputTokens: Long, outputTokens: Long): Long =
        inputTokens * limits.inputPriceMicroUsdPerMillionTokens / 1_000_000L +
            outputTokens * limits.outputPriceMicroUsdPerMillionTokens / 1_000_000L

    private fun requireOwner(job: LessonJobRecord, ownerInstallationId: String) {
        if (job.ownerInstallationId != ownerInstallationId) throw LessonJobOwnershipException()
    }

    override fun close() {
        scope.cancel()
    }

    private data class BatchResult(val blocks: List<GeneratedLessonBlock>, val metrics: LessonJobMetrics)

    private class JobCancelledException : RuntimeException("İş durduruldu.")
    private class JobStoppedException : RuntimeException("İş artık çalışmıyor.")
}

private operator fun LessonJobMetrics.plus(other: LessonJobMetrics) = LessonJobMetrics(
    providerRequestCount = providerRequestCount + other.providerRequestCount,
    inputTokens = inputTokens + other.inputTokens,
    outputTokens = outputTokens + other.outputTokens,
    totalTokens = totalTokens + other.totalTokens,
    providerLatencyMillis = providerLatencyMillis + other.providerLatencyMillis,
    estimatedCostMicroUsd = estimatedCostMicroUsd + other.estimatedCostMicroUsd,
)

private fun LessonJobFormatSpec.toDomain() = LessonFormatDefinition(
    formatId = formatId,
    revision = revision,
    title = title,
    instruction = instruction,
    teachingMode = FormatTeachingMode.valueOf(teachingMode.uppercase()),
    teachingLanguage = teachingLanguage,
    targetLanguage = targetLanguage,
    learnerLevel = learnerLevel,
    fields = fields.map { field ->
        FormatFieldDefinition(
            key = field.key,
            label = field.label,
            kind = FormatFieldKind.valueOf(field.kind.uppercase()),
            required = field.required,
            visible = field.visible,
            speakable = field.speakable,
            position = field.position,
        )
    },
    totalBlockCount = totalBlockCount,
    blocksPerRequest = blocksPerRequest,
    requestIntervalSeconds = requestIntervalSeconds,
    cardWidthFraction = cardWidthFraction,
)

private fun String.languageCode(): String = when (trim().lowercase()) {
    "türkçe", "turkish", "tr" -> "tr"
    "ingilizce", "english", "en" -> "en"
    else -> take(2).lowercase()
}

private fun JsonElement?.asDisplayText(): String = when (this) {
    null -> ""
    is JsonPrimitive -> contentOrNull.orEmpty()
    is JsonArray -> joinToString("\n") { item -> "• ${item.asDisplayText()}" }
    is JsonObject -> entries.joinToString("\n") { (key, value) -> "$key: ${value.asDisplayText()}" }
}

private fun Throwable.errorCode(): String = when (this) {
    is LessonJobOutputException -> "invalid_model_output"
    is kotlinx.coroutines.TimeoutCancellationException -> "provider_timeout"
    is IllegalArgumentException -> "invalid_job"
    else -> "generation_failed"
}

private fun Throwable.safeMessage(): String = message
    ?.replace(Regex("(?i)(bearer|apikey|token|secret|key)\\s*[:=]\\s*[^\\s,;]+"), "$1=[gizlendi]")
    ?.take(500)
    ?: "Ders oluşturulamadı."
