package com.mehmetdem.dil.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimecodeParserTest {
    @Test
    fun `parses supported time formats`() {
        assertEquals(90_000L, TimecodeParser.parseMillis("90"))
        assertEquals(90_000L, TimecodeParser.parseMillis("01:30"))
        assertEquals(3_723_000L, TimecodeParser.parseMillis("1:02:03"))
    }

    @Test
    fun `rejects malformed timecodes`() {
        assertNull(TimecodeParser.parseMillis(""))
        assertNull(TimecodeParser.parseMillis("1:61"))
        assertNull(TimecodeParser.parseMillis("a:b"))
        assertNull(TimecodeParser.parseMillis("-1"))
    }
}

