package com.mehmetdem.dil.backend.source

import com.mehmetdem.dil.backend.domain.PdfIngestionRequest
import com.mehmetdem.dil.backend.domain.SourceUnit
import com.mehmetdem.dil.backend.domain.YouTubeIngestionRequest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SourceIngestionServiceTest {
    @Test
    fun `clips youtube cues to selected interval`() = kotlinx.coroutines.test.runTest {
        val root = Files.createTempDirectory("source-youtube-test")
        val runner = ExternalProcessRunner { command, _ ->
            assertContains(command, "--js-runtimes")
            assertEquals("deno", command[command.indexOf("--js-runtimes") + 1])
            val template = command[command.indexOf("--output") + 1]
            Files.writeString(
                java.nio.file.Path.of(template.replace("%(ext)s", "en.vtt")),
                """
                WEBVTT

                00:00:01.000 --> 00:00:03.000
                first

                00:00:03.000 --> 00:00:06.000
                second
                """.trimIndent(),
            )
            ProcessOutput(0, "", "")
        }
        val service = SourceIngestionService(runner, root)

        val result = service.ingestYouTube(YouTubeIngestionRequest("dQw4w9WgXcQ", 2_000, 4_000))

        assertEquals(2, result.size)
        assertEquals(2_000, result[0].startInclusive)
        assertEquals(4_000, result[1].endExclusive)
        assertEquals(SourceUnit.MILLISECOND, result[0].unit)
        root.toFile().deleteRecursively()
    }

    @Test
    fun `keeps selected pdf pages in deterministic order`() = kotlinx.coroutines.test.runTest {
        val root = Files.createTempDirectory("source-pdf-test")
        val pdf = Files.writeString(root.resolve("lesson.pdf"), "%PDF-test")
        val runner = ExternalProcessRunner { command, _ ->
            when (command.first()) {
                "pdfinfo" -> ProcessOutput(0, "Pages:          8\n", "")
                "pdftotext" -> {
                    val page = command[command.indexOf("-f") + 1]
                    ProcessOutput(0, "Page $page contains enough deterministic learning text for extraction.", "")
                }
                else -> error("Unexpected command: $command")
            }
        }
        val service = SourceIngestionService(runner, root)

        val result = service.ingestPdf(PdfIngestionRequest(pdf.toString(), 3, 5))

        assertEquals(listOf(3L, 4L, 5L), result.map { it.startInclusive })
        assertEquals(listOf(0, 1, 2), result.map { it.ordinal })
        root.toFile().deleteRecursively()
    }
}
