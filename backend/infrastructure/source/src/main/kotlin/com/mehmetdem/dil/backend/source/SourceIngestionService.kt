package com.mehmetdem.dil.backend.source

import com.mehmetdem.dil.backend.domain.PdfIngestionRequest
import com.mehmetdem.dil.backend.domain.SourceIngestionGateway
import com.mehmetdem.dil.backend.domain.SourceSegment
import com.mehmetdem.dil.backend.domain.SourceUnit
import com.mehmetdem.dil.backend.domain.YouTubeIngestionRequest
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

class SourceIngestionService(
    private val runner: ExternalProcessRunner = SafeProcessRunner(),
    private val tempRoot: Path? = null,
) : SourceIngestionGateway {
    override suspend fun ingestYouTube(request: YouTubeIngestionRequest): List<SourceSegment> {
        require(request.videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) { "YouTube video kimliği geçersiz." }
        require(request.startMillis >= 0 && request.endMillisExclusive > request.startMillis) { "Zaman aralığı geçersiz." }
        require(request.endMillisExclusive - request.startMillis <= 6 * 60 * 60 * 1_000L) { "En fazla 6 saat işlenebilir." }

        val directory = createTempDirectory("youtube-")
        try {
            val outputTemplate = directory.resolve("subtitle.%(ext)s").absolutePathString()
            val languageSelector = request.preferredLanguages
                .map { it.lowercase().replace(Regex("[^a-z-]"), "") }
                .filter(String::isNotBlank)
                .distinct()
                .joinToString(",") { "$it.*" }
                .ifBlank { "tr.*,en.*" }
            val result = runner.run(
                listOf(
                    "yt-dlp", "--skip-download", "--write-subs", "--write-auto-subs",
                    "--js-runtimes", "node",
                    "--sub-langs", languageSelector, "--sub-format", "vtt",
                    "--no-playlist", "--output", outputTemplate,
                    "https://www.youtube.com/watch?v=${request.videoId}",
                ),
                timeoutMillis = 90_000,
            )
            check(result.exitCode == 0) { "YouTube altyazısı alınamadı: ${safeError(result.stderr)}" }
            val subtitle = directory.listDirectoryEntries()
                .filter { it.isRegularFile() && it.extension.equals("vtt", ignoreCase = true) }
                .sortedBy { it.fileName.toString() }
                .firstOrNull()
                ?: error("Bu video için kullanılabilir altyazı bulunamadı.")

            val clipped = WebVttParser.parse(Files.readString(subtitle))
                .filter { it.endMillisExclusive > request.startMillis && it.startMillis < request.endMillisExclusive }
                .map { cue ->
                    cue.copy(
                        startMillis = maxOf(cue.startMillis, request.startMillis),
                        endMillisExclusive = minOf(cue.endMillisExclusive, request.endMillisExclusive),
                    )
                }
            check(clipped.isNotEmpty()) { "Seçilen zaman aralığında konuşma bulunamadı." }
            val revision = sha256("youtube|${request.videoId}|${request.startMillis}|${request.endMillisExclusive}|${clipped.joinToString { it.text }}")
            return clipped.mapIndexed { index, cue ->
                SourceSegment(
                    revisionId = revision,
                    ordinal = index,
                    text = cue.text,
                    unit = SourceUnit.MILLISECOND,
                    startInclusive = cue.startMillis,
                    endExclusive = cue.endMillisExclusive,
                )
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    override suspend fun ingestPdf(request: PdfIngestionRequest): List<SourceSegment> {
        require(request.startPage >= 1 && request.endPageInclusive >= request.startPage) { "PDF sayfa aralığı geçersiz." }
        require(request.endPageInclusive - request.startPage + 1 <= 50) { "Tek istekte en fazla 50 PDF sayfası işlenebilir." }
        val input = Path.of(request.filePath).toAbsolutePath().normalize()
        require(Files.isRegularFile(input)) { "PDF dosyası bulunamadı." }

        val info = runner.run(listOf("pdfinfo", input.toString()), timeoutMillis = 15_000)
        check(info.exitCode == 0) { "PDF okunamadı: ${safeError(info.stderr)}" }
        val pageCount = Regex("(?m)^Pages:\\s+(\\d+)").find(info.stdout)?.groupValues?.get(1)?.toIntOrNull()
            ?: error("PDF sayfa sayısı belirlenemedi.")
        require(request.endPageInclusive <= pageCount) { "PDF yalnızca $pageCount sayfa içeriyor." }

        val directory = createTempDirectory("pdf-")
        try {
            val pageTexts = (request.startPage..request.endPageInclusive).map { page ->
                val extracted = runner.run(
                    listOf("pdftotext", "-f", page.toString(), "-l", page.toString(), "-layout", input.toString(), "-"),
                    timeoutMillis = 20_000,
                )
                check(extracted.exitCode == 0) { "PDF sayfası çıkarılamadı: ${safeError(extracted.stderr)}" }
                val normalized = normalizeText(extracted.stdout)
                page to if (normalized.length >= 40) normalized else ocrPage(input, page, directory)
            }
            val revision = sha256("pdf|${sha256(Files.readAllBytes(input))}|${request.startPage}|${request.endPageInclusive}|${pageTexts.joinToString { it.second }}")
            return pageTexts.mapIndexed { index, (page, text) ->
                check(text.isNotBlank()) { "$page. sayfadan okunabilir metin çıkarılamadı." }
                SourceSegment(
                    revisionId = revision,
                    ordinal = index,
                    text = text,
                    unit = SourceUnit.PAGE,
                    startInclusive = page.toLong(),
                    endExclusive = (page + 1).toLong(),
                )
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private suspend fun ocrPage(input: Path, page: Int, directory: Path): String {
        val prefix = directory.resolve("page-$page")
        val render = runner.run(
            listOf("pdftoppm", "-f", page.toString(), "-l", page.toString(), "-singlefile", "-r", "200", "-png", input.toString(), prefix.toString()),
            timeoutMillis = 40_000,
        )
        check(render.exitCode == 0) { "PDF OCR görüntüsü üretilemedi: ${safeError(render.stderr)}" }
        val image = Path.of("${prefix}.png")
        val ocr = runner.run(listOf("tesseract", image.toString(), "stdout", "-l", "tur+eng"), timeoutMillis = 60_000)
        check(ocr.exitCode == 0) { "PDF OCR işlemi başarısız: ${safeError(ocr.stderr)}" }
        return normalizeText(ocr.stdout)
    }

    private fun createTempDirectory(prefix: String): Path = tempRoot?.let {
        Files.createDirectories(it)
        Files.createTempDirectory(it, prefix)
    } ?: Files.createTempDirectory(prefix)

    private fun normalizeText(value: String): String = value
        .replace("\u0000", "")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    private fun safeError(value: String): String = value.replace(Regex("\\s+"), " ").trim().take(300)

    private fun sha256(value: String): String = sha256(value.toByteArray())

    private fun sha256(value: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(value)
        .joinToString("") { "%02x".format(it) }
}
