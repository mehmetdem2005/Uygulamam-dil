package com.mehmetdem.dil.backend.application

import com.mehmetdem.dil.backend.domain.ModelGateway
import com.mehmetdem.dil.backend.domain.ModelStreamEvent
import com.mehmetdem.dil.backend.domain.PdfIngestionRequest
import com.mehmetdem.dil.backend.domain.SourceIngestionGateway
import com.mehmetdem.dil.backend.domain.SourceSegment
import com.mehmetdem.dil.backend.domain.YouTubeIngestionRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class LessonJobOrchestratorTest {
    @Test
    fun `job checkpoints model blocks and becomes ready`() = runBlocking {
        val root = Files.createTempDirectory("lesson-orchestrator-test")
        val store = FileLessonJobStore(root)
        val source = object : SourceIngestionGateway {
            override suspend fun ingestYouTube(request: YouTubeIngestionRequest) = listOf(
                SourceSegment("revision-1", 0, "Hello there"),
            )

            override suspend fun ingestPdf(request: PdfIngestionRequest): List<SourceSegment> = error("not used")
        }
        val model = ModelGateway {
            flow {
                emit(ModelStreamEvent.ContentDelta("""{"blocks":[{"index":0,"source_text":"hello","translation":"merhaba"},{"index":1,"source_text":"goodbye","translation":"hoşça kal"}]}"""))
                emit(ModelStreamEvent.Usage(20, 30, 50))
                emit(ModelStreamEvent.Completed)
            }
        }
        val orchestrator = LessonJobOrchestrator(
            store = store,
            sourceGateway = source,
            modelGateway = model,
            limits = LessonJobLimits(maxProviderAttemptsPerBatch = 1),
        )
        try {
            val created = orchestrator.create(sampleCommand())
            val ready = awaitState(store, created.id, LessonJobState.READY)

            assertEquals(listOf(0, 1), ready.blocks.map(GeneratedLessonBlock::index))
            assertEquals("merhaba", ready.blocks.first().translation)
            assertEquals(50, ready.metrics.totalTokens)
            assertEquals(1, ready.metrics.providerRequestCount)
        } finally {
            orchestrator.close()
            root.toFile().deleteRecursively()
        }
    }

    private suspend fun awaitState(store: FileLessonJobStore, id: String, state: LessonJobState): LessonJobRecord {
        repeat(100) {
            val job = requireNotNull(store.get(id))
            if (job.state == state) return job
            if (job.state == LessonJobState.FAILED) error(job.errorMessage.orEmpty())
            delay(20)
        }
        error("Job did not reach $state")
    }
}
