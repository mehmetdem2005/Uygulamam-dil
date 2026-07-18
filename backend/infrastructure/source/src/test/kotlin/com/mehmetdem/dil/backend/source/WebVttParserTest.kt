package com.mehmetdem.dil.backend.source

import kotlin.test.Test
import kotlin.test.assertEquals

class WebVttParserTest {
    @Test
    fun `parses timestamps strips tags and merges duplicate overlap`() {
        val cues = WebVttParser.parse(
            """
            WEBVTT

            00:00:01.000 --> 00:00:02.500
            <c>he said that</c>

            00:00:02.000 --> 00:00:03.000
            he said that

            00:00:03.200 --> 00:00:04.000
            o dedi ki
            """.trimIndent(),
        )

        assertEquals(2, cues.size)
        assertEquals(1_000, cues[0].startMillis)
        assertEquals(3_000, cues[0].endMillisExclusive)
        assertEquals("he said that", cues[0].text)
    }
}
