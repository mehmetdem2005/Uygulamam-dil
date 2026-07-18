package com.mehmetdem.dil.core.data

import com.mehmetdem.dil.core.model.ContentRange
import com.mehmetdem.dil.core.model.LessonFormat
import com.mehmetdem.dil.core.model.LessonBlock
import com.mehmetdem.dil.core.model.LessonJobState
import com.mehmetdem.dil.core.model.LessonGenerationMetrics
import com.mehmetdem.dil.core.model.LessonSessionConfig
import com.mehmetdem.dil.core.model.SourceKind
import com.mehmetdem.dil.core.model.SourceSelection
import com.mehmetdem.dil.core.model.StoredLesson
import kotlin.test.Test
import kotlin.test.assertEquals

class LessonJsonCodecTest {
    @Test
    fun `round trips a stored lesson without changing its format revision`() {
        val source = StoredLesson(
            id = "lesson-1",
            config = LessonSessionConfig(
                SourceSelection(SourceKind.YOUTUBE, "https://youtu.be/dQw4w9WgXcQ", "YouTube", ContentRange.Time(1_000, 8_000)),
                LessonFormat(formatId = "format-1", revision = 3, title = "Gerçek ders", instruction = "Kaynak cümleyi çevir ve kısaca açıkla."),
            ),
            state = LessonJobState.PAUSED,
            completedBlockCount = 2,
            blocks = listOf(
                LessonBlock(
                    index = 0,
                    title = "İfade",
                    sourceText = "How are you?",
                    targetText = "How are you?",
                    translation = "Nasılsın?",
                    pronunciation = null,
                    explanation = "Günlük selamlaşma.",
                    fieldValues = mapOf("source_text" to "How are you?", "translation" to "Nasılsın?"),
                ),
            ),
            remoteJobId = "remote-job-1",
            generationMetrics = LessonGenerationMetrics(providerRequestCount = 2, totalTokens = 340),
            lastSyncError = "geçici bağlantı hatası",
            createdAtEpochMillis = 1_000,
            updatedAtEpochMillis = 2_000,
        )

        val decoded = LessonJsonCodec.decode(LessonJsonCodec.encode(listOf(source))).single()

        assertEquals(source, decoded)
        assertEquals(3, decoded.config.format.revision)
    }
}
