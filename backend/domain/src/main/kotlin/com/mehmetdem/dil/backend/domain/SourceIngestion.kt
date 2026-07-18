package com.mehmetdem.dil.backend.domain

enum class SourceUnit {
    MILLISECOND,
    PAGE,
    TEXT,
}

data class YouTubeIngestionRequest(
    val videoId: String,
    val startMillis: Long,
    val endMillisExclusive: Long,
    val preferredLanguages: List<String> = listOf("tr", "en"),
)

data class PdfIngestionRequest(
    val filePath: String,
    val startPage: Int,
    val endPageInclusive: Int,
)

interface SourceIngestionGateway {
    suspend fun ingestYouTube(request: YouTubeIngestionRequest): List<SourceSegment>
    suspend fun ingestPdf(request: PdfIngestionRequest): List<SourceSegment>
}
