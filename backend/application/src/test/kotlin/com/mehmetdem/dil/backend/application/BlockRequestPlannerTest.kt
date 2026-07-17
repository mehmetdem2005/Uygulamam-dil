package com.mehmetdem.dil.backend.application

import com.mehmetdem.dil.backend.domain.LessonGenerationPlan
import com.mehmetdem.dil.backend.domain.SourceSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BlockRequestPlannerTest {
    private val planner = BlockRequestPlanner()

    @Test
    fun `creates deterministic bounded batches`() {
        val plan = LessonGenerationPlan(
            lessonId = "lesson-1",
            formatRevisionId = "format-1",
            instruction = "Translate and explain each sentence.",
            teachingLanguage = "Turkish",
            targetLanguage = "English",
            firstBlockIndex = 0,
            totalBlockCount = 5,
            blocksPerRequest = 2,
            thinkingEnabled = false,
        )
        val segments = listOf(SourceSegment("source-1", 0, "He said that he was ready."))

        val first = planner.plan(segments, plan)
        val second = planner.plan(segments, plan)

        assertEquals(listOf(2, 2, 1), first.map { it.blockCount })
        assertEquals(listOf(0, 2, 4), first.map { it.firstBlockIndex })
        assertEquals(first.map { it.idempotencyKey }, second.map { it.idempotencyKey })
        assertNotEquals(first[0].idempotencyKey, first[1].idempotencyKey)
    }
}

