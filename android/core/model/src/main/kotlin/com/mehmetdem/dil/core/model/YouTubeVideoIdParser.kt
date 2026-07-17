package com.mehmetdem.dil.core.model

import java.net.URI

object YouTubeVideoIdParser {
    private val videoId = Regex("^[A-Za-z0-9_-]{11}$")

    fun parse(raw: String): String? {
        val value = raw.trim()
        if (videoId.matches(value)) return value

        val uri = runCatching {
            URI(if (value.contains("://")) value else "https://$value")
        }.getOrNull() ?: return null

        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        val candidate = when (host) {
            "youtu.be" -> uri.path.trim('/').substringBefore('/')
            "youtube.com", "m.youtube.com", "music.youtube.com" -> {
                val pathParts = uri.path.split('/').filter(String::isNotBlank)
                when (pathParts.firstOrNull()) {
                    "shorts", "embed", "live" -> pathParts.getOrNull(1)
                    "watch" -> queryParameter(uri.rawQuery, "v")
                    else -> queryParameter(uri.rawQuery, "v")
                }
            }
            else -> null
        }

        return candidate?.takeIf(videoId::matches)
    }

    private fun queryParameter(rawQuery: String?, name: String): String? = rawQuery
        ?.split('&')
        ?.asSequence()
        ?.map { it.substringBefore('=') to it.substringAfter('=', "") }
        ?.firstOrNull { it.first == name }
        ?.second
}

