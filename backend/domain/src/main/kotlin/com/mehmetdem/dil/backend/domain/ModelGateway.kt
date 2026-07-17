package com.mehmetdem.dil.backend.domain

import kotlinx.coroutines.flow.Flow

data class ModelGenerationRequest(
    val systemPrompt: String,
    val userPrompt: String,
    val thinkingEnabled: Boolean,
)

sealed interface ModelStreamEvent {
    data class ContentDelta(val text: String) : ModelStreamEvent
    data class ReasoningDelta(val text: String) : ModelStreamEvent
    data class Usage(
        val inputTokens: Long,
        val outputTokens: Long,
        val totalTokens: Long,
    ) : ModelStreamEvent
    data object Completed : ModelStreamEvent
}

fun interface ModelGateway {
    fun stream(request: ModelGenerationRequest): Flow<ModelStreamEvent>
}

