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
    val title: String,
    val instruction: String,
    val teachingLanguage: String = "Türkçe",
    val targetLanguage: String = "İngilizce",
    val learnerLevel: String = "B2",
    val totalBlockCount: Int = 10,
    val blocksPerRequest: Int = 1,
    val cardWidthFraction: Float = 1f,
    val playback: PlaybackPreferences = PlaybackPreferences(),
)

data class LessonSessionConfig(
    val source: SourceSelection,
    val format: LessonFormat,
    val visibility: LessonVisibility = LessonVisibility.PRIVATE,
)

data class LessonBlock(
    val index: Int,
    val title: String?,
    val sourceText: String?,
    val targetText: String,
    val translation: String?,
    val pronunciation: String?,
    val explanation: String?,
    val audioAssetId: String? = null,
) {
    init {
        require(index >= 0) { "Blok sırası negatif olamaz." }
        require(targetText.isNotBlank()) { "Öğretim kartının ana metni boş olamaz." }
    }
}

