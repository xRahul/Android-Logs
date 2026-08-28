package `in`.rahulja.getlogs.data

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.DateFormat
import java.util.Date

class LegacyFileWriter(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun appendLogFile(fileName: String, data: String) = withContext(ioDispatcher) {
        if (fileName.isBlank() || data.isBlank()) {
            return@withContext
        }
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val folder = File(baseDir, LOG_FOLDER)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val file = File(folder, fileName)
        OutputStreamWriter(FileOutputStream(file, true), Charsets.UTF_8).use { writer ->
            writer.append(data).append("\n")
        }
    }

    suspend fun writeAllLogs(json: String) {
        appendLogFile(ALL_LOGS_FILE, json)
    }

    suspend fun writeAction(action: String) {
        appendLogFile(ALL_ACTIONS_FILE, "${formatCurrentDate()}, $action")
    }

    suspend fun writeLocation(lat: String, lng: String) {
        appendLogFile(LOCATION_FILE, "${formatCurrentDate()}, $lat, $lng")
    }

    suspend fun writePasswordAttempt(succeeded: Boolean) {
        val status = if (succeeded) "SUCCEEDED" else "FAILED"
        appendLogFile(PASS_WORD_FILE, "${formatCurrentDate()}, $status")
    }

    suspend fun writeDeviceUsage(locked: Boolean) {
        val status = if (locked) "LOCKED" else "UNLOCKED"
        appendLogFile(DEVICE_USED_FILE, "${formatCurrentDate()}, $status")
    }

    suspend fun writeWifiScan(info: String) {
        appendLogFile(WIFI_FILE, "${formatCurrentDate()}, $info")
    }

    private fun formatCurrentDate(): String {
        return DateFormat.getDateTimeInstance().format(Date())
    }

    companion object {
        const val ALL_ACTIONS_FILE = "allActions.csv"
        const val ALL_LOGS_FILE = "allLogs.txt"
        const val LOCATION_FILE = "location.csv"
        const val PASS_WORD_FILE = "passwordAttempts.csv"
        const val DEVICE_USED_FILE = "deviceUsed.csv"
        const val WIFI_FILE = "wifi.csv"
        const val LOG_FOLDER = "AllLogs"
    }
}
