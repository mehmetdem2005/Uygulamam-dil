package com.mehmetdem.dil.backend.source

data class WebVttCue(
    val startMillis: Long,
    val endMillisExclusive: Long,
    val text: String,
)

object WebVttParser {
    private val timestampLine = Regex("([0-9:.]+)\\s+-->\\s+([0-9:.]+).*")
    private val htmlTag = Regex("<[^>]+>")

    fun parse(content: String): List<WebVttCue> {
        val lines = content.replace("\r\n", "\n").split('\n')
        val cues = mutableListOf<WebVttCue>()
        var index = 0
        while (index < lines.size) {
            val match = timestampLine.matchEntire(lines[index].trim())
            if (match == null) {
                index++
                continue
            }
            val start = parseTimestamp(match.groupValues[1])
            val end = parseTimestamp(match.groupValues[2])
            index++
            val textLines = mutableListOf<String>()
            while (index < lines.size && lines[index].isNotBlank()) {
                textLines += lines[index]
                index++
            }
            val text = textLines.joinToString(" ")
                .replace(htmlTag, "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (text.isNotBlank() && end > start) cues += WebVttCue(start, end, text)
        }

        return cues.fold(mutableListOf()) { result, cue ->
            val previous = result.lastOrNull()
            if (previous != null && previous.text == cue.text && cue.startMillis <= previous.endMillisExclusive) {
                result[result.lastIndex] = previous.copy(endMillisExclusive = maxOf(previous.endMillisExclusive, cue.endMillisExclusive))
            } else {
                result += cue
            }
            result
        }
    }

    private fun parseTimestamp(value: String): Long {
        val parts = value.split(':')
        require(parts.size in 2..3) { "VTT zaman damgası geçersiz: $value" }
        val seconds = parts.last().replace(',', '.').toDouble()
        val minutes = parts[parts.lastIndex - 1].toLong()
        val hours = if (parts.size == 3) parts.first().toLong() else 0L
        return ((hours * 3_600 + minutes * 60) * 1_000 + seconds * 1_000).toLong()
    }
}
