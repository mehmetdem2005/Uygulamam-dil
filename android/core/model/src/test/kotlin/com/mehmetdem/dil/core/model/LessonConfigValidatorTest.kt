package com.mehmetdem.dil.core.model

import kotlin.test.Test
import kotlin.test.assertTrue

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
}
