package com.mehmetdem.dil.core.model

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class LessonConfigValidatorTest {
    @Test
    fun `accepts a complete youtube lesson configuration`() {
        val config = LessonSessionConfig(
            source = SourceSelection(
                kind = SourceKind.YOUTUBE,
                locator = "https://youtu.be/dQw4w9WgXcQ",
                displayName = "YouTube",
                range = ContentRange.Time(0, 60_000),
            ),
            format = LessonFormat(
                title = "İngilizce dersi",
                instruction = "Cümleyi çevir, Amerikan IPA ekle ve grameri açıkla.",
            ),
        )

        assertTrue(LessonConfigValidator.validate(config).isEmpty())
    }

    @Test
    fun `copying a format starts a separate revision history`() {
        val source = LessonFormat(
            formatId = "original",
            revision = 4,
            title = "Dil öğret",
            instruction = "Cümleyi göster ve anlaşılır biçimde çevir.",
        )

        val copied = source.copyAsNew("copy")

        assertEquals("copy", copied.formatId)
        assertEquals(1, copied.revision)
        assertEquals(listOf(0, 1), copied.orderedFields().map { it.position })
    }
}
