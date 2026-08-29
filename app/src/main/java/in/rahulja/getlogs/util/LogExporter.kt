package `in`.rahulja.getlogs.util

import android.content.Context
import android.net.Uri
import `in`.rahulja.getlogs.data.LogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class LogExporter(
    private val repository: LogRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    @Suppress("TooGenericExceptionCaught")
    suspend fun exportLogsToUri(context: Context, destinationUri: Uri): Result<Int> =
        withContext(ioDispatcher) {
            try {
                val logs = repository.getAllLogs()
                val outputStream = context.contentResolver.openOutputStream(destinationUri)
                    ?: return@withContext Result.failure(
                        IOException("Failed to open output stream for URI: $destinationUri")
                    )

                outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    logs.forEachIndexed { index, log ->
                        if (index > 0) {
                            writer.write("\n\n" + LOG_SEPARATOR + "\n\n")
                        }
                        val formatted = if (log.formattedText.isNotBlank()) {
                            log.formattedText
                        } else {
                            LogFormatter.formatForClipboard(log)
                        }
                        writer.write(formatted)
                    }
                }
                Result.success(logs.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    companion object {
        private const val LOG_SEPARATOR = "----------------------------------------"
    }
}
