package com.mehmetdem.dil.backend.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class ProcessOutput(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

fun interface ExternalProcessRunner {
    suspend fun run(command: List<String>, timeoutMillis: Long): ProcessOutput
}

class SafeProcessRunner(
    private val maxOutputBytes: Int = 4 * 1024 * 1024,
) : ExternalProcessRunner {
    override suspend fun run(command: List<String>, timeoutMillis: Long): ProcessOutput = withContext(Dispatchers.IO) {
        require(command.isNotEmpty()) { "Komut boş olamaz." }
        require(timeoutMillis in 1_000..300_000) { "Komut zaman aşımı geçersiz." }

        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        coroutineScope {
            val stdout = async { process.inputStream.readCapped(maxOutputBytes) }
            val stderr = async { process.errorStream.readCapped(maxOutputBytes) }
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                throw IllegalStateException("Kaynak işleme komutu zaman aşımına uğradı.")
            }
            ProcessOutput(process.exitValue(), stdout.await(), stderr.await())
        }
    }

    private fun InputStream.readCapped(limit: Int): String {
        val output = ByteArrayOutputStream(minOf(limit, 64 * 1024))
        val buffer = ByteArray(8 * 1024)
        var stored = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            val writable = minOf(read, limit - stored).coerceAtLeast(0)
            if (writable > 0) {
                output.write(buffer, 0, writable)
                stored += writable
            }
        }
        return output.toString(StandardCharsets.UTF_8)
    }
}
