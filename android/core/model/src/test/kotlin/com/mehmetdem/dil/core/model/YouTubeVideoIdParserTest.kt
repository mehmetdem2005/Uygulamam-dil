package com.mehmetdem.dil.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YouTubeVideoIdParserTest {
    @Test
    fun `extracts ids from supported youtube links`() {
        val id = "dQw4w9WgXcQ"
        assertEquals(id, YouTubeVideoIdParser.parse("https://www.youtube.com/watch?v=$id"))
        assertEquals(id, YouTubeVideoIdParser.parse("https://youtu.be/$id?t=12"))
        assertEquals(id, YouTubeVideoIdParser.parse("youtube.com/shorts/$id"))
        assertEquals(id, YouTubeVideoIdParser.parse(id))
    }

    @Test
    fun `rejects unrelated and malformed links`() {
        assertNull(YouTubeVideoIdParser.parse("https://example.com/watch?v=dQw4w9WgXcQ"))
        assertNull(YouTubeVideoIdParser.parse("https://youtube.com/watch?v=short"))
    }
}

