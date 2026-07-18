package com.mehmetdem.dil.core.model

enum class SourceKind {
    YOUTUBE,
    PDF,
}

enum class LessonVisibility {
    PRIVATE,
    UNLISTED,
    PUBLIC,
}

enum class LessonJobState {
    DRAFT,
    CREATED,
    INGESTING,
    GENERATING,
    PAUSED,
    READY,
    FAILED,
    CANCELLED,
}

enum class TeachingMode {
    LANGUAGE,
    EXPLAIN,
    SUMMARY,
    TRANSLATE,
    CUSTOM,
}

enum class FormatFieldType {
    SHORT_TEXT,
    LONG_TEXT,
    PRONUNCIATION,
    EXAMPLES,
    QUIZ,
}

data class LessonFormatField(
    val id: String,
    val key: String,
    val label: String,
    val type: FormatFieldType = FormatFieldType.SHORT_TEXT,
    val required: Boolean = true,
    val visible: Boolean = true,
    val speakable: Boolean = false,
    val position: Int,
) {
    init {
        require(id.isNotBlank()) { "Alan kimliği boş olamaz." }
        require(key.matches(Regex("[a-z][a-z0-9_]{1,39}"))) { "Alan anahtarı geçersiz." }
        require(label.trim().length in 1..40) { "Alan adı 1 ile 40 karakter arasında olmalıdır." }
        require(position >= 0) { "Alan sırası negatif olamaz." }
    }
}

sealed interface ContentRange {
    data class Time(
        val startMillis: Long,
        val endMillisExclusive: Long,
    ) : ContentRange {
        init {
            require(startMillis >= 0) { "Başlangıç zamanı negatif olamaz." }
            require(endMillisExclusive > startMillis) { "Bitiş zamanı başlangıçtan büyük olmalıdır." }
        }
    }

    data class Pages(
        val startPage: Int,
        val endPageInclusive: Int,
    ) : ContentRange {
        init {
            require(startPage >= 1) { "PDF sayfaları 1'den başlar." }
            require(endPageInclusive >= startPage) { "Bitiş sayfası başlangıçtan küçük olamaz." }
        }
    }
}

data class SourceSelection(
    val kind: SourceKind,
    val locator: String,
    val displayName: String,
    val range: ContentRange,
)

data class PlaybackPreferences(
    val ttsEnabled: Boolean = true,
    val voiceCommandsEnabled: Boolean = false,
    val continueWhenScreenOff: Boolean = false,
    val autoAdvance: Boolean = true,
)

data class LessonFormat(
    val formatId: String = "draft",
    val revision: Int = 1,
    val title: String,
    val instruction: String,
    val teachingMode: TeachingMode = TeachingMode.LANGUAGE,
    val teachingLanguage: String = "Türkçe",
    val targetLanguage: String = "İngilizce",
    val learnerLevel: String = "B2",
    val fields: List<LessonFormatField> = defaultLanguageFields(),
    val totalBlockCount: Int = 10,
    val blocksPerRequest: Int = 1,
    val requestIntervalSeconds: Int = 15,
    val continuousRequests: Boolean = true,
    val cardWidthFraction: Float = 1f,
    val playback: PlaybackPreferences = PlaybackPreferences(),
) {
    init {
        require(revision >= 1) { "Format sürümü 1'den başlamalıdır." }
    }

    fun orderedFields(): List<LessonFormatField> = fields.sortedWith(
        compareBy<LessonFormatField> { it.position }.thenBy { it.key },
    )

    fun copyAsNew(newFormatId: String): LessonFormat = copy(
        formatId = newFormatId,
        revision = 1,
        title = "$title — Kopya",
        fields = orderedFields().mapIndexed { index, field ->
            field.copy(id = "${field.key}-$index", position = index)
        },
    )

    companion object {
        fun defaultLanguageFields(): List<LessonFormatField> = listOf(
            LessonFormatField(
                id = "source-0",
                key = "source_text",
                label = "İngilizce",
                speakable = true,
                position = 0,
            ),
            LessonFormatField(
                id = "translation-1",
                key = "translation",
                label = "Türkçe",
                speakable = true,
                position = 1,
            ),
        )
    }
}

data class LessonSessionConfig(
    val source: SourceSelection,
    val format: LessonFormat,
    val visibility: LessonVisibility = LessonVisibility.PRIVATE,
)

data class StoredLesson(
    val id: String,
    val config: LessonSessionConfig,
    val state: LessonJobState,
    val completedBlockCount: Int,
    val blocks: List<LessonBlock> = emptyList(),
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    init {
        require(id.isNotBlank()) { "Ders kimliği boş olamaz." }
        require(completedBlockCount in 0..config.format.totalBlockCount) { "Tamamlanan kart sayısı geçersiz." }
        require(blocks.size <= config.format.totalBlockCount) { "Kaydedilen kart sayısı format sınırını aşamaz." }
        require(createdAtEpochMillis > 0 && updatedAtEpochMillis >= createdAtEpochMillis) { "Ders zamanı geçersiz." }
    }
}

data class LessonBlock(
    val index: Int,
    val title: String?,
    val sourceText: String?,
    val targetText: String,
    val translation: String?,
    val pronunciation: String?,
    val explanation: String?,
    val audioAssetId: String? = null,
    val fieldValues: Map<String, String> = emptyMap(),
) {
    init {
        require(index >= 0) { "Blok sırası negatif olamaz." }
        require(targetText.isNotBlank()) { "Öğretim kartının ana metni boş olamaz." }
    }
}
