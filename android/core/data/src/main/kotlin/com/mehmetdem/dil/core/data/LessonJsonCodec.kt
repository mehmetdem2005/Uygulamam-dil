package com.mehmetdem.dil.core.data

import com.mehmetdem.dil.core.model.ContentRange
import com.mehmetdem.dil.core.model.FormatFieldType
import com.mehmetdem.dil.core.model.LessonFormat
import com.mehmetdem.dil.core.model.LessonFormatField
import com.mehmetdem.dil.core.model.LessonBlock
import com.mehmetdem.dil.core.model.LessonJobState
import com.mehmetdem.dil.core.model.LessonSessionConfig
import com.mehmetdem.dil.core.model.LessonVisibility
import com.mehmetdem.dil.core.model.PlaybackPreferences
import com.mehmetdem.dil.core.model.SourceKind
import com.mehmetdem.dil.core.model.SourceSelection
import com.mehmetdem.dil.core.model.StoredLesson
import com.mehmetdem.dil.core.model.TeachingMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object LessonJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(lessons: List<StoredLesson>): String = json.encodeToString(lessons.map(StoredLessonDto::fromDomain))

    fun decode(payload: String): List<StoredLesson> = json.decodeFromString<List<StoredLessonDto>>(payload).map(StoredLessonDto::toDomain)
}

@Serializable
private data class StoredLessonDto(
    val id: String,
    val state: String,
    val completedBlockCount: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val visibility: String,
    val source: SourceDto,
    val format: FormatDto,
    val blocks: List<BlockDto> = emptyList(),
) {
    fun toDomain() = StoredLesson(
        id = id,
        config = LessonSessionConfig(source.toDomain(), format.toDomain(), LessonVisibility.valueOf(visibility)),
        state = LessonJobState.valueOf(state),
        completedBlockCount = completedBlockCount,
        blocks = blocks.map(BlockDto::toDomain),
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

    companion object {
        fun fromDomain(lesson: StoredLesson) = StoredLessonDto(
            id = lesson.id,
            state = lesson.state.name,
            completedBlockCount = lesson.completedBlockCount,
            createdAtEpochMillis = lesson.createdAtEpochMillis,
            updatedAtEpochMillis = lesson.updatedAtEpochMillis,
            visibility = lesson.config.visibility.name,
            source = SourceDto.fromDomain(lesson.config.source),
            format = FormatDto.fromDomain(lesson.config.format),
            blocks = lesson.blocks.map(BlockDto::fromDomain),
        )
    }
}

@Serializable
private data class BlockDto(
    val index: Int,
    val title: String? = null,
    val sourceText: String? = null,
    val targetText: String,
    val translation: String? = null,
    val pronunciation: String? = null,
    val explanation: String? = null,
    val audioAssetId: String? = null,
    val fieldValues: Map<String, String> = emptyMap(),
) {
    fun toDomain() = LessonBlock(
        index = index,
        title = title,
        sourceText = sourceText,
        targetText = targetText,
        translation = translation,
        pronunciation = pronunciation,
        explanation = explanation,
        audioAssetId = audioAssetId,
        fieldValues = fieldValues,
    )

    companion object {
        fun fromDomain(block: LessonBlock) = BlockDto(
            index = block.index,
            title = block.title,
            sourceText = block.sourceText,
            targetText = block.targetText,
            translation = block.translation,
            pronunciation = block.pronunciation,
            explanation = block.explanation,
            audioAssetId = block.audioAssetId,
            fieldValues = block.fieldValues,
        )
    }
}

@Serializable
private data class SourceDto(
    val kind: String,
    val locator: String,
    val displayName: String,
    val rangeKind: String,
    val start: Long,
    val end: Long,
) {
    fun toDomain() = SourceSelection(
        kind = SourceKind.valueOf(kind),
        locator = locator,
        displayName = displayName,
        range = if (rangeKind == "time") ContentRange.Time(start, end) else ContentRange.Pages(start.toInt(), end.toInt()),
    )

    companion object {
        fun fromDomain(source: SourceSelection): SourceDto = when (val range = source.range) {
            is ContentRange.Time -> SourceDto(source.kind.name, source.locator, source.displayName, "time", range.startMillis, range.endMillisExclusive)
            is ContentRange.Pages -> SourceDto(source.kind.name, source.locator, source.displayName, "pages", range.startPage.toLong(), range.endPageInclusive.toLong())
        }
    }
}

@Serializable
private data class FormatDto(
    val formatId: String,
    val revision: Int,
    val title: String,
    val instruction: String,
    val teachingMode: String,
    val teachingLanguage: String,
    val targetLanguage: String,
    val learnerLevel: String,
    val fields: List<FieldDto>,
    val totalBlockCount: Int,
    val blocksPerRequest: Int,
    val requestIntervalSeconds: Int,
    val continuousRequests: Boolean,
    val cardWidthFraction: Float,
    val playback: PlaybackDto,
) {
    fun toDomain() = LessonFormat(
        formatId = formatId,
        revision = revision,
        title = title,
        instruction = instruction,
        teachingMode = TeachingMode.valueOf(teachingMode),
        teachingLanguage = teachingLanguage,
        targetLanguage = targetLanguage,
        learnerLevel = learnerLevel,
        fields = fields.map(FieldDto::toDomain),
        totalBlockCount = totalBlockCount,
        blocksPerRequest = blocksPerRequest,
        requestIntervalSeconds = requestIntervalSeconds,
        continuousRequests = continuousRequests,
        cardWidthFraction = cardWidthFraction,
        playback = playback.toDomain(),
    )

    companion object {
        fun fromDomain(format: LessonFormat) = FormatDto(
            formatId = format.formatId,
            revision = format.revision,
            title = format.title,
            instruction = format.instruction,
            teachingMode = format.teachingMode.name,
            teachingLanguage = format.teachingLanguage,
            targetLanguage = format.targetLanguage,
            learnerLevel = format.learnerLevel,
            fields = format.fields.map(FieldDto::fromDomain),
            totalBlockCount = format.totalBlockCount,
            blocksPerRequest = format.blocksPerRequest,
            requestIntervalSeconds = format.requestIntervalSeconds,
            continuousRequests = format.continuousRequests,
            cardWidthFraction = format.cardWidthFraction,
            playback = PlaybackDto.fromDomain(format.playback),
        )
    }
}

@Serializable
private data class FieldDto(
    val id: String,
    val key: String,
    val label: String,
    val type: String,
    val required: Boolean,
    val visible: Boolean,
    val speakable: Boolean,
    val position: Int,
) {
    fun toDomain() = LessonFormatField(id, key, label, FormatFieldType.valueOf(type), required, visible, speakable, position)

    companion object {
        fun fromDomain(field: LessonFormatField) = FieldDto(field.id, field.key, field.label, field.type.name, field.required, field.visible, field.speakable, field.position)
    }
}

@Serializable
private data class PlaybackDto(
    val ttsEnabled: Boolean,
    val voiceCommandsEnabled: Boolean,
    val continueWhenScreenOff: Boolean,
    val autoAdvance: Boolean,
) {
    fun toDomain() = PlaybackPreferences(ttsEnabled, voiceCommandsEnabled, continueWhenScreenOff, autoAdvance)

    companion object {
        fun fromDomain(playback: PlaybackPreferences) = PlaybackDto(playback.ttsEnabled, playback.voiceCommandsEnabled, playback.continueWhenScreenOff, playback.autoAdvance)
    }
}
