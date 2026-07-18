package com.mehmetdem.dil.backend.application

import com.mehmetdem.dil.backend.domain.CompiledFormatSchema
import com.mehmetdem.dil.backend.domain.FormatFieldDefinition
import com.mehmetdem.dil.backend.domain.FormatFieldKind
import com.mehmetdem.dil.backend.domain.LessonFormatDefinition
import java.security.MessageDigest

class FormatSchemaCompiler {
    fun compile(format: LessonFormatDefinition): CompiledFormatSchema {
        validate(format)
        val fields = format.fields.sortedWith(compareBy<FormatFieldDefinition> { it.position }.thenBy { it.key })
        val properties = fields.joinToString(",") { field ->
            val typeSchema = when (field.kind) {
                FormatFieldKind.EXAMPLES -> "{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"maxItems\":5}"
                FormatFieldKind.QUIZ -> "{\"type\":\"object\",\"additionalProperties\":false,\"required\":[\"question\",\"answer\"],\"properties\":{\"question\":{\"type\":\"string\"},\"answer\":{\"type\":\"string\"}}}"
                FormatFieldKind.SHORT_TEXT -> "{\"type\":\"string\",\"maxLength\":300}"
                FormatFieldKind.LONG_TEXT -> "{\"type\":\"string\",\"maxLength\":1600}"
                FormatFieldKind.PRONUNCIATION -> "{\"type\":\"string\",\"maxLength\":300}"
            }
            "\"${escape(field.key)}\":$typeSchema"
        }
        val required = fields.filter { it.required }.joinToString(",") { "\"${escape(it.key)}\"" }
        val schema = "{" +
            "\"\u0024schema\":\"https://json-schema.org/draft/2020-12/schema\"," +
            "\"type\":\"object\"," +
            "\"additionalProperties\":false," +
            "\"required\":[\"index\"${if (required.isNotEmpty()) ",$required" else ""}]," +
            "\"properties\":{\"index\":{\"type\":\"integer\",\"minimum\":0},$properties}" +
            "}"
        return CompiledFormatSchema(
            formatId = format.formatId,
            revision = format.revision,
            schemaJson = schema,
            sha256 = sha256(schema),
        )
    }

    fun nextRevision(previous: LessonFormatDefinition, edited: LessonFormatDefinition): LessonFormatDefinition {
        require(previous.formatId == edited.formatId) { "Format kimliği sürümleme sırasında değiştirilemez." }
        return edited.copy(revision = previous.revision + 1)
    }

    fun copyAsNew(source: LessonFormatDefinition, newFormatId: String): LessonFormatDefinition {
        require(newFormatId.isNotBlank()) { "Yeni format kimliği gereklidir." }
        return source.copy(formatId = newFormatId, revision = 1, title = "${source.title} — Kopya")
    }

    private fun validate(format: LessonFormatDefinition) {
        require(format.formatId.isNotBlank()) { "Format kimliği gereklidir." }
        require(format.revision >= 1) { "Format sürümü geçersiz." }
        require(format.title.trim().length in 1..120) { "Format adı 1..120 karakter olmalıdır." }
        require(format.instruction.trim().length in 10..4_000) { "Format talimatı 10..4000 karakter olmalıdır." }
        require(format.fields.size in 1..8) { "Format 1..8 alan içermelidir." }
        require(format.fields.map { it.key }.distinct().size == format.fields.size) { "Alan anahtarları benzersiz olmalıdır." }
        require(format.fields.map { it.position }.distinct().size == format.fields.size) { "Alan sıraları benzersiz olmalıdır." }
        format.fields.forEach { field ->
            require(field.key.matches(Regex("[a-z][a-z0-9_]{1,39}"))) { "Geçersiz alan anahtarı: ${field.key}" }
            require(field.label.trim().length in 1..40) { "Alan etiketi 1..40 karakter olmalıdır." }
        }
        require(format.totalBlockCount in 1..100)
        require(format.blocksPerRequest in 1..10 && format.blocksPerRequest <= format.totalBlockCount)
        require(format.requestIntervalSeconds in 2..120)
        require(format.cardWidthFraction in 0.72f..1f)
    }

    private fun escape(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
