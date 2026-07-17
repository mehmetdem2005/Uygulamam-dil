package com.mehmetdem.dil.backend.deepseek

import com.mehmetdem.dil.backend.domain.ModelGateway
import com.mehmetdem.dil.backend.domain.ModelGenerationRequest
import com.mehmetdem.dil.backend.domain.ModelStreamEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class DeepSeekConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.deepseek.com",
    val model: String = "deepseek-v4-pro",
)

class DeepSeekProviderException(
    val statusCode: Int,
    message: String,
) : RuntimeException(message)

class DeepSeekModelGateway(
    private val config: DeepSeekConfig,
    private val json: Json = providerJson(),
    private val client: HttpClient = defaultClient(json),
) : ModelGateway {
    override fun stream(request: ModelGenerationRequest): Flow<ModelStreamEvent> = flow {
        val payload = DeepSeekChatRequest(
            model = config.model,
            messages = listOf(
                DeepSeekMessage(role = "system", content = request.systemPrompt),
                DeepSeekMessage(role = "user", content = request.userPrompt),
            ),
            thinking = DeepSeekThinking(type = if (request.thinkingEnabled) "enabled" else "disabled"),
            stream = true,
            streamOptions = DeepSeekStreamOptions(includeUsage = true),
        )

        client.preparePost("${config.baseUrl.trimEnd('/')}/chat/completions") {
            bearerAuth(config.apiKey)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(payload)
        }.execute { response ->
            if (!response.status.isSuccess()) {
                val error = runCatching { response.bodyAsText() }.getOrDefault("DeepSeek request failed")
                throw DeepSeekProviderException(response.status.value, error.take(2_000))
            }

            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") {
                    emit(ModelStreamEvent.Completed)
                    break
                }
                if (data.isEmpty()) continue

                val chunk = json.decodeFromString<DeepSeekChunk>(data)
                chunk.choices.firstOrNull()?.delta?.reasoningContent
                    ?.takeIf(String::isNotEmpty)
                    ?.let { emit(ModelStreamEvent.ReasoningDelta(it)) }
                chunk.choices.firstOrNull()?.delta?.content
                    ?.takeIf(String::isNotEmpty)
                    ?.let { emit(ModelStreamEvent.ContentDelta(it)) }
                chunk.usage?.let {
                    emit(
                        ModelStreamEvent.Usage(
                            inputTokens = it.promptTokens,
                            outputTokens = it.completionTokens,
                            totalTokens = it.totalTokens,
                        ),
                    )
                }
            }
        }
    }

    companion object {
        private fun providerJson() = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        private fun defaultClient(json: Json) = HttpClient(CIO) {
            install(ContentNegotiation) { json(json) }
            expectSuccess = false
        }
    }
}

@Serializable
private data class DeepSeekChatRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val thinking: DeepSeekThinking,
    val stream: Boolean,
    @SerialName("stream_options") val streamOptions: DeepSeekStreamOptions,
)

@Serializable
private data class DeepSeekMessage(val role: String, val content: String)

@Serializable
private data class DeepSeekThinking(val type: String)

@Serializable
private data class DeepSeekStreamOptions(
    @SerialName("include_usage") val includeUsage: Boolean,
)

@Serializable
private data class DeepSeekChunk(
    val choices: List<DeepSeekChoice> = emptyList(),
    val usage: DeepSeekUsage? = null,
)

@Serializable
private data class DeepSeekChoice(val delta: DeepSeekDelta)

@Serializable
private data class DeepSeekDelta(
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
)

@Serializable
private data class DeepSeekUsage(
    @SerialName("prompt_tokens") val promptTokens: Long = 0,
    @SerialName("completion_tokens") val completionTokens: Long = 0,
    @SerialName("total_tokens") val totalTokens: Long = 0,
)
