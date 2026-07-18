package com.mehmetdem.dil

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.mehmetdem.dil.core.data.LessonLocalRepository
import com.mehmetdem.dil.core.model.LessonJobState
import com.mehmetdem.dil.core.model.SourceKind
import com.mehmetdem.dil.core.model.StoredLesson
import com.mehmetdem.dil.core.network.LessonApiException
import com.mehmetdem.dil.core.network.LessonJobApi
import com.mehmetdem.dil.core.network.RemoteLessonJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class LessonJobCoordinator(
    context: Context,
    private val repository: LessonLocalRepository,
    private val api: LessonJobApi,
    private val scope: CoroutineScope,
    private val onChanged: () -> Unit,
) {
    private val resolver = context.applicationContext.contentResolver
    private val installationId = installationId(context.applicationContext)
    private val active = ConcurrentHashMap<String, Job>()

    fun syncAll(lessons: List<StoredLesson>) {
        lessons.filter { it.state in autoSyncStates }.forEach(::sync)
    }

    fun sync(lesson: StoredLesson) {
        if (lesson.state !in autoSyncStates) return
        active.computeIfAbsent(lesson.id) { lessonId ->
            scope.launch {
                try {
                    synchronize(lessonId)
                } finally {
                    active.remove(lessonId)
                }
            }
        }
    }

    fun pause(lesson: StoredLesson) = control(lesson, api::pause, continuePolling = false)

    fun resume(lesson: StoredLesson) = control(lesson, api::resume, continuePolling = true)

    fun retry(lesson: StoredLesson) {
        if (lesson.remoteJobId == null) {
            repository.updateState(lesson.id, LessonJobState.CREATED)
            repository.updateSyncError(lesson.id, null)
            onChanged()
            repository.find(lesson.id)?.let(::sync)
        } else {
            control(lesson, api::retry, continuePolling = true)
        }
    }

    fun cancelRemoteBestEffort(lesson: StoredLesson) {
        val remoteId = lesson.remoteJobId ?: return
        scope.launch { runCatching { api.cancel(installationId, remoteId) } }
    }

    private fun control(
        lesson: StoredLesson,
        action: suspend (String, String) -> RemoteLessonJob,
        continuePolling: Boolean,
    ) {
        val remoteId = lesson.remoteJobId
        if (remoteId == null) {
            if (continuePolling) retry(lesson)
            return
        }
        scope.launch {
            runCatching { action(installationId, remoteId) }
                .onSuccess { remote ->
                    apply(remote)
                    active.remove(lesson.id)?.cancel()
                    if (continuePolling) repository.find(lesson.id)?.let(::sync)
                }
                .onFailure { failure ->
                    repository.updateSyncError(lesson.id, failure.userMessage())
                    onChanged()
                }
        }
    }

    private suspend fun synchronize(lessonId: String) {
        var failures = 0
        while (true) {
            val lesson = repository.find(lessonId) ?: return
            if (lesson.state !in autoSyncStates) return
            try {
                val remoteId = lesson.remoteJobId
                val remote = if (remoteId == null) createRemoteJob(lesson) else {
                    api.getJob(installationId, remoteId)
                }
                failures = 0
                apply(remote)
                if (remote.state !in autoSyncStates) return
                delay(POLL_INTERVAL_MILLIS)
            } catch (failure: Throwable) {
                failures += 1
                val nonRetryable = failure is LessonApiException && failure.statusCode in 400..499 && failure.statusCode != 408 && failure.statusCode != 429
                if (nonRetryable) {
                    repository.updateState(lessonId, LessonJobState.FAILED)
                    repository.updateSyncError(lessonId, failure.userMessage())
                    onChanged()
                    return
                }
                repository.updateSyncError(lessonId, "Bağlantı kurulamadı; ders sunucuda sürüyorsa yeniden bağlanılacak. ${failure.userMessage()}")
                onChanged()
                if (failures >= MAX_RECONNECT_ATTEMPTS) return
                delay((2_000L shl (failures - 1)).coerceAtMost(30_000L))
            }
        }
    }

    private suspend fun createRemoteJob(lesson: StoredLesson): RemoteLessonJob {
        repository.updateState(lesson.id, LessonJobState.INGESTING)
        repository.updateSyncError(lesson.id, null)
        onChanged()
        val uploadId = if (lesson.config.source.kind == SourceKind.PDF) {
            val bytes = withContext(Dispatchers.IO) { resolver.readBytesWithLimit(Uri.parse(lesson.config.source.locator), MAX_PDF_BYTES) }
            api.uploadPdf(installationId, lesson.config.source.displayName, bytes)
        } else null
        return api.createJob(installationId, lesson, uploadId)
    }

    private fun apply(remote: RemoteLessonJob) {
        repository.applyRemoteUpdate(
            id = remote.localLessonId,
            remoteJobId = remote.jobId,
            state = remote.state,
            blocks = remote.blocks,
            metrics = remote.metrics,
            error = remote.errorMessage,
        )
        onChanged()
    }

    private fun ContentResolver.readBytesWithLimit(uri: Uri, limit: Int): ByteArray {
        val input = openInputStream(uri) ?: throw IllegalArgumentException("PDF dosyası açılamadı.")
        return input.use { it.readCapped(limit) }
    }

    private fun InputStream.readCapped(limit: Int): ByteArray {
        val output = ByteArrayOutputStream(minOf(limit, 64 * 1024))
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            require(total <= limit) { "PDF en fazla ${limit / 1024 / 1024} MB olabilir." }
            output.write(buffer, 0, read)
        }
        require(total > 0) { "PDF dosyası boş." }
        return output.toByteArray()
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 1_000L
        const val MAX_RECONNECT_ATTEMPTS = 5
        const val MAX_PDF_BYTES = 50 * 1024 * 1024
        val autoSyncStates = setOf(LessonJobState.CREATED, LessonJobState.INGESTING, LessonJobState.GENERATING)

        fun installationId(context: Context): String {
            val preferences = context.getSharedPreferences("installation", Context.MODE_PRIVATE)
            return preferences.getString("id", null) ?: UUID.randomUUID().toString().also { generated ->
                preferences.edit().putString("id", generated).commit()
            }
        }
    }
}

private fun Throwable.userMessage(): String = when (this) {
    is LessonApiException -> message
    is IllegalArgumentException -> message ?: "Ders ayarları geçersiz."
    else -> message?.takeIf(String::isNotBlank)?.take(240) ?: "Sunucuya bağlanılamadı."
}
