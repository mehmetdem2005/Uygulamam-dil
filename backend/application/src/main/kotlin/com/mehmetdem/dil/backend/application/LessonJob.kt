package com.mehmetdem.dil.backend.application

import kotlinx.serialization.Serializable

@Serializable
enum class LessonJobSourceKind {
    YOUTUBE,
    PDF,
}

@Serializable
enum class LessonJobState {
    CREATED,
    INGESTING,
    GENERATING,
    PAUSED,
    READY,
    FAILED,
    CANCELLED,
}

@Serializable
data class LessonJobSourceSpec(
    val kind: LessonJobSourceKind,
    val locator: String,
    val displayName: String,
    val startMillis: Long? = null,
    val endMillisExclusive: Long? = null,
    val startPage: Int? = null,
    val endPageInclusive: Int? = null,
)

@Serializable
data class LessonJobFormatFieldSpec(
    val key: String,
    val label: String,
    val kind: String,
    val required: Boolean,
    val visible: Boolean,
    val speakable: Boolean,
    val position: Int,
)

@Serializable
data class LessonJobFormatSpec(
    val formatId: String,
    val revision: Int,
    val title: String,
    val instruction: String,
    val teachingMode: String,
    val teachingLanguage: String,
    val targetLanguage: String,
    val learnerLevel: String,
    val fields: List<LessonJobFormatFieldSpec>,
    val totalBlockCount: Int,
    val blocksPerRequest: Int,
    val requestIntervalSeconds: Int,
    val cardWidthFraction: Float,
)

@Serializable
data class LessonJobCreateCommand(
    val ownerInstallationId: String,
    val idempotencyKey: String,
    val localLessonId: String,
    val source: LessonJobSourceSpec,
    val format: LessonJobFormatSpec,
    val qualityMode: Boolean,
    val continuousRequests: Boolean,
)

@Serializable
data class GeneratedLessonBlock(
    val index: Int,
    val title: String? = null,
    val sourceText: String? = null,
    val targetText: String,
    val translation: String? = null,
    val pronunciation: String? = null,
    val explanation: String? = null,
    val fieldValues: Map<String, String> = emptyMap(),
)

@Serializable
data class LessonJobMetrics(
    val providerRequestCount: Int = 0,
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val totalTokens: Long = 0,
    val providerLatencyMillis: Long = 0,
    val estimatedCostMicroUsd: Long = 0,
)

@Serializable
data class LessonJobRecord(
    val id: String,
    val commandHash: String,
    val ownerInstallationId: String,
    val idempotencyKey: String,
    val localLessonId: String,
    val source: LessonJobSourceSpec,
    val format: LessonJobFormatSpec,
    val qualityMode: Boolean,
    val continuousRequests: Boolean,
    val state: LessonJobState,
    val blocks: List<GeneratedLessonBlock> = emptyList(),
    val metrics: LessonJobMetrics = LessonJobMetrics(),
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val version: Long = 1,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

class LessonJobNotFoundException(jobId: String) : RuntimeException("Ders işi bulunamadı: $jobId")

class LessonJobOwnershipException : RuntimeException("Bu ders işine erişim izniniz yok.")

class IdempotencyConflictException : RuntimeException("Aynı idempotency anahtarı farklı bir ders için kullanılamaz.")

class LessonJobOutputException(message: String) : RuntimeException(message)
