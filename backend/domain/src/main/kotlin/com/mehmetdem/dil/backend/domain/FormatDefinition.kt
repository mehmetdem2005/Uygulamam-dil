package com.mehmetdem.dil.backend.domain

enum class FormatTeachingMode {
    LANGUAGE,
    EXPLAIN,
    SUMMARY,
    TRANSLATE,
    CUSTOM,
}

enum class FormatFieldKind {
    SHORT_TEXT,
    LONG_TEXT,
    PRONUNCIATION,
    EXAMPLES,
    QUIZ,
}

data class FormatFieldDefinition(
    val key: String,
    val label: String,
    val kind: FormatFieldKind,
    val required: Boolean,
    val visible: Boolean,
    val speakable: Boolean,
    val position: Int,
)

data class LessonFormatDefinition(
    val formatId: String,
    val revision: Int,
    val title: String,
    val instruction: String,
    val teachingMode: FormatTeachingMode,
    val teachingLanguage: String,
    val targetLanguage: String,
    val learnerLevel: String,
    val fields: List<FormatFieldDefinition>,
    val totalBlockCount: Int,
    val blocksPerRequest: Int,
    val requestIntervalSeconds: Int,
    val cardWidthFraction: Float,
)

data class CompiledFormatSchema(
    val formatId: String,
    val revision: Int,
    val schemaJson: String,
    val sha256: String,
)
