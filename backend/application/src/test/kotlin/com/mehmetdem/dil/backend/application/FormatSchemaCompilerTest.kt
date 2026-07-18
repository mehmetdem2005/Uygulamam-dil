package com.mehmetdem.dil.backend.application

import com.mehmetdem.dil.backend.domain.FormatFieldDefinition
import com.mehmetdem.dil.backend.domain.FormatFieldKind
import com.mehmetdem.dil.backend.domain.FormatTeachingMode
import com.mehmetdem.dil.backend.domain.LessonFormatDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FormatSchemaCompilerTest {
    private val compiler = FormatSchemaCompiler()

    @Test
    fun `same format always compiles to same schema and hash`() {
        val format = sampleFormat()
        val first = compiler.compile(format)
        val second = compiler.compile(format.copy(fields = format.fields.reversed()))

        assertEquals(first.schemaJson, second.schemaJson)
        assertEquals(first.sha256, second.sha256)
        assertTrue(first.schemaJson.contains("\"source_text\""))
        assertTrue(first.schemaJson.contains("\"translation\""))
    }

    @Test
    fun `editing creates a new immutable revision identity`() {
        val original = sampleFormat()
        val revised = compiler.nextRevision(original, original.copy(title = "Yeni ad"))
        val copied = compiler.copyAsNew(original, "format-copy")

        assertEquals(2, revised.revision)
        assertEquals(original.formatId, revised.formatId)
        assertEquals(1, copied.revision)
        assertNotEquals(original.formatId, copied.formatId)
    }

    private fun sampleFormat() = LessonFormatDefinition(
        formatId = "format-1",
        revision = 1,
        title = "İngilizce — Türkçe Örnekler",
        instruction = "Kaynak cümleyi göster ve Türkçe karşılığını açıkla.",
        teachingMode = FormatTeachingMode.LANGUAGE,
        teachingLanguage = "Türkçe",
        targetLanguage = "İngilizce",
        learnerLevel = "A2",
        fields = listOf(
            FormatFieldDefinition("translation", "Türkçe", FormatFieldKind.SHORT_TEXT, true, true, true, 1),
            FormatFieldDefinition("source_text", "İngilizce", FormatFieldKind.SHORT_TEXT, true, true, true, 0),
        ),
        totalBlockCount = 10,
        blocksPerRequest = 2,
        requestIntervalSeconds = 15,
        cardWidthFraction = 1f,
    )
}
