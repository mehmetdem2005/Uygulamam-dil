package com.mehmetdem.dil.backend.supabase

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class SupabaseConfig(
    val url: String,
    val serviceRoleKey: String,
)

data class SupabaseHealthResult(
    val reachable: Boolean,
    val schemaVersion: String? = null,
)

class SupabaseHealthGateway(
    private val config: SupabaseConfig,
    private val json: Json = defaultJson(),
    private val client: HttpClient = defaultClient(),
) {
    suspend fun check(): SupabaseHealthResult = runCatching {
        val response = client.post("${config.url.trimEnd('/')}/rest/v1/rpc/app_health") {
            header("apikey", config.serviceRoleKey)
            bearerAuth(config.serviceRoleKey)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("{}")
        }
        if (!response.status.isSuccess()) return@runCatching SupabaseHealthResult(reachable = false)

        val payload = json.decodeFromString<SupabaseHealthPayload>(response.bodyAsText())
        SupabaseHealthResult(
            reachable = payload.status == "ok",
            schemaVersion = payload.schemaVersion,
        )
    }.getOrDefault(SupabaseHealthResult(reachable = false))

    companion object {
        private fun defaultJson() = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        private fun defaultClient() = HttpClient(CIO) {
            install(ContentNegotiation) { json(defaultJson()) }
            install(HttpTimeout) {
                connectTimeoutMillis = 2_000
                requestTimeoutMillis = 3_000
                socketTimeoutMillis = 3_000
            }
            expectSuccess = false
        }
    }
}

@Serializable
private data class SupabaseHealthPayload(
    val status: String,
    @SerialName("schema_version") val schemaVersion: String,
)
