package com.mehmetdem.dil.core.model

object TimecodeParser {
    fun parseMillis(raw: String): Long? {
        val normalized = raw.trim()
        if (normalized.isEmpty()) return null

        val parts = normalized.split(':')
        if (parts.size !in 1..3 || parts.any { it.isEmpty() || it.any(Char::isWhitespace) }) {
            return null
        }

        val values = parts.map { it.toLongOrNull() ?: return null }
        if (values.any { it < 0 }) return null

        val seconds = when (values.size) {
            1 -> values[0]
            2 -> {
                if (values[1] >= 60) return null
                values[0] * 60 + values[1]
            }
            3 -> {
                if (values[1] >= 60 || values[2] >= 60) return null
                values[0] * 3_600 + values[1] * 60 + values[2]
            }
            else -> return null
        }

        return runCatching { Math.multiplyExact(seconds, 1_000L) }.getOrNull()
    }

    fun formatMillis(millis: Long): String {
        require(millis >= 0) { "Zaman negatif olamaz." }
        val totalSeconds = millis / 1_000
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}

