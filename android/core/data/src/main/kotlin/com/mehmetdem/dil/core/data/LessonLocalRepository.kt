package com.mehmetdem.dil.core.data

import android.content.Context
import com.mehmetdem.dil.core.model.LessonJobState
import com.mehmetdem.dil.core.model.LessonBlock
import com.mehmetdem.dil.core.model.LessonSessionConfig
import com.mehmetdem.dil.core.model.LessonGenerationMetrics
import com.mehmetdem.dil.core.model.StoredLesson
import java.util.UUID

interface LessonLocalRepository {
    fun all(): List<StoredLesson>
    fun find(id: String): StoredLesson?
    fun create(config: LessonSessionConfig): StoredLesson
    fun updateState(id: String, state: LessonJobState, completedBlockCount: Int? = null): StoredLesson?
    fun replaceBlocks(id: String, blocks: List<LessonBlock>, state: LessonJobState): StoredLesson?
    fun applyRemoteUpdate(
        id: String,
        remoteJobId: String,
        state: LessonJobState,
        blocks: List<LessonBlock>,
        metrics: LessonGenerationMetrics,
        error: String?,
    ): StoredLesson?
    fun updateSyncError(id: String, error: String?): StoredLesson?
    fun delete(id: String): Boolean
}

class PreferencesLessonLocalRepository(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) : LessonLocalRepository {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun all(): List<StoredLesson> = read().sortedByDescending(StoredLesson::updatedAtEpochMillis)

    @Synchronized
    override fun find(id: String): StoredLesson? = read().firstOrNull { it.id == id }

    @Synchronized
    override fun create(config: LessonSessionConfig): StoredLesson {
        val now = clock().coerceAtLeast(1L)
        val lesson = StoredLesson(
            id = UUID.randomUUID().toString(),
            config = config,
            state = LessonJobState.CREATED,
            completedBlockCount = 0,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        write(read().filterNot { it.id == lesson.id } + lesson)
        return lesson
    }

    @Synchronized
    override fun updateState(id: String, state: LessonJobState, completedBlockCount: Int?): StoredLesson? {
        var updated: StoredLesson? = null
        val lessons = read().map { lesson ->
            if (lesson.id != id) lesson else lesson.copy(
                state = state,
                completedBlockCount = (completedBlockCount ?: lesson.completedBlockCount)
                    .coerceIn(0, lesson.config.format.totalBlockCount),
                updatedAtEpochMillis = maxOf(clock(), lesson.createdAtEpochMillis),
            ).also { updated = it }
        }
        if (updated != null) write(lessons)
        return updated
    }

    @Synchronized
    override fun replaceBlocks(id: String, blocks: List<LessonBlock>, state: LessonJobState): StoredLesson? {
        var updated: StoredLesson? = null
        val lessons = read().map { lesson ->
            if (lesson.id != id) lesson else lesson.copy(
                state = state,
                blocks = blocks.sortedBy(LessonBlock::index).take(lesson.config.format.totalBlockCount),
                completedBlockCount = blocks.size.coerceIn(0, lesson.config.format.totalBlockCount),
                updatedAtEpochMillis = maxOf(clock(), lesson.createdAtEpochMillis),
            ).also { updated = it }
        }
        if (updated != null) write(lessons)
        return updated
    }

    @Synchronized
    override fun applyRemoteUpdate(
        id: String,
        remoteJobId: String,
        state: LessonJobState,
        blocks: List<LessonBlock>,
        metrics: LessonGenerationMetrics,
        error: String?,
    ): StoredLesson? {
        var updated: StoredLesson? = null
        val lessons = read().map { lesson ->
            if (lesson.id != id) lesson else {
                val normalizedBlocks = blocks
                    .associateBy(LessonBlock::index)
                    .toSortedMap()
                    .values
                    .take(lesson.config.format.totalBlockCount)
                lesson.copy(
                    state = state,
                    completedBlockCount = normalizedBlocks.size,
                    blocks = normalizedBlocks,
                    remoteJobId = remoteJobId,
                    generationMetrics = metrics,
                    lastSyncError = error,
                    updatedAtEpochMillis = maxOf(clock(), lesson.createdAtEpochMillis),
                ).also { updated = it }
            }
        }
        if (updated != null) write(lessons)
        return updated
    }

    @Synchronized
    override fun updateSyncError(id: String, error: String?): StoredLesson? {
        var updated: StoredLesson? = null
        val lessons = read().map { lesson ->
            if (lesson.id != id) lesson else lesson.copy(
                lastSyncError = error,
                updatedAtEpochMillis = maxOf(clock(), lesson.createdAtEpochMillis),
            ).also { updated = it }
        }
        if (updated != null) write(lessons)
        return updated
    }

    @Synchronized
    override fun delete(id: String): Boolean {
        val current = read()
        val remaining = current.filterNot { it.id == id }
        if (remaining.size == current.size) return false
        write(remaining)
        return true
    }

    private fun read(): List<StoredLesson> {
        val payload = preferences.getString(KEY_LESSONS, null) ?: return emptyList()
        return runCatching { LessonJsonCodec.decode(payload) }
            .getOrElse {
                preferences.edit().putString(KEY_CORRUPT_BACKUP, payload).apply()
                emptyList()
            }
    }

    private fun write(lessons: List<StoredLesson>) {
        preferences.edit().putString(KEY_LESSONS, LessonJsonCodec.encode(lessons)).commit()
    }

    private companion object {
        const val PREFERENCES_NAME = "local_lessons_v1"
        const val KEY_LESSONS = "lessons"
        const val KEY_CORRUPT_BACKUP = "corrupt_backup"
    }
}
