package com.mehmetdem.dil.backend.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PreviewAccessTest {
    private val json = Json { encodeDefaults = true; explicitNulls = false }

    @Test
    fun `issued token verifies only until expiry`() {
        var now = 1_000_000L
        val service = PreviewTokenService(
            secret = "0123456789abcdef0123456789abcdef",
            json = json,
            clock = { now },
            sessionDurationSeconds = 60,
        )
        val session = service.issue("installation-12345")

        assertEquals("installation-12345", service.verify(session.accessToken))
        now += 61_000
        assertFailsWith<PreviewUnauthorizedException> { service.verify(session.accessToken) }
    }

    @Test
    fun `tampered token is rejected`() {
        val service = PreviewTokenService("0123456789abcdef0123456789abcdef", json)
        val token = service.issue("installation-12345").accessToken

        assertFailsWith<PreviewUnauthorizedException> { service.verify(token.dropLast(1) + "x") }
    }
}
