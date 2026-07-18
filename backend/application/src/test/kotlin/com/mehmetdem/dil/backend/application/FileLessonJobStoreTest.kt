package com.mehmetdem.dil.backend.application

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FileLessonJobStoreTest {
    @Test
    fun `same idempotency command returns same durable job`() {
        val root = Files.createTempDirectory("lesson-job-store-test")
        try {
            val store = FileLessonJobStore(root)
            val command = sampleCommand()
            val first = store.createOrGet(command)
            val second = store.createOrGet(command)
            val reopened = FileLessonJobStore(root).get(first.id)

            assertEquals(first.id, second.id)
            assertEquals(first, reopened)
            assertFailsWith<IdempotencyConflictException> {
                store.createOrGet(command.copy(localLessonId = "different-lesson"))
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}

internal fun sampleCommand() = LessonJobCreateCommand(
    ownerInstallationId = "installation-12345",
    idempotencyKey = "idempotency-12345",
    localLessonId = "local-lesson-1",
    source = LessonJobSourceSpec(
        kind = LessonJobSourceKind.YOUTUBE,
        locator = "dQw4w9WgXcQ",
        displayName = "Test videosu",
        startMillis = 0,
        endMillisExclusive = 30_000,
    ),
    format = LessonJobFormatSpec(
        formatId = "format-1",
        revision = 1,
        title = "İngilizce kartlar",
        instruction = "Kaynak cümleyi göster ve Türkçe karşılığını yaz.",
        teachingMode = "language",
        teachingLanguage = "Türkçe",
        targetLanguage = "İngilizce",
        learnerLevel = "A2",
        fields = listOf(
            LessonJobFormatFieldSpec("source_text", "İngilizce", "short_text", true, true, true, 0),
            LessonJobFormatFieldSpec("translation", "Türkçe", "short_text", true, true, true, 1),
        ),
        totalBlockCount = 2,
        blocksPerRequest = 2,
        requestIntervalSeconds = 2,
        cardWidthFraction = 1f,
    ),
    qualityMode = false,
    continuousRequests = true,
)
