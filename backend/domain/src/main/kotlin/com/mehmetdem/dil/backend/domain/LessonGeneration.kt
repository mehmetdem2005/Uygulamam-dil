package com.mehmetdem.dil.backend.domain

data class SourceSegment(
    val revisionId: String,
    val ordinal: Int,
    val text: String,
)

data class LessonGenerationPlan(
    val lessonId: String,
    val formatRevisionId: String,
    val instruction: String,
    val teachingLanguage: String,
    val targetLanguage: String,
    val firstBlockIndex: Int,
    val totalBlockCount: Int,
    val blocksPerRequest: Int,
    val thinkingEnabled: Boolean,
)

data class GenerationBatch(
    val idempotencyKey: String,
    val firstBlockIndex: Int,
    val blockCount: Int,
    val sourceText: String,
    val modelRequest: ModelGenerationRequest,
)

