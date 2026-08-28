package `in`.rahulja.getlogs.data

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import `in`.rahulja.getlogs.model.LogEntity
import `in`.rahulja.getlogs.model.LogType
import `in`.rahulja.getlogs.util.LogFormatter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

@Suppress("TooManyFunctions")
class LogRepository(
    private val logDao: LogDao,
    private val legacyFileWriter: LegacyFileWriter,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getLogsPaging(query: String? = null): Flow<PagingData<LogEntity>> {
        val config = PagingConfig(
            pageSize = PAGE_SIZE,
            enablePlaceholders = false
        )
        return if (query.isNullOrBlank()) {
            Pager(config = config) {
                logDao.getAllLogsPaging()
            }.flow
        } else {
            val ftsQuery = formatSearchQuery(query)
            Pager(config = config) {
                logDao.searchLogsPaging(ftsQuery)
            }.flow
        }
    }

    suspend fun recordLog(
        action: String,
        dataPayload: String = "{}",
        logType: LogType = LogType.GENERAL,
        customFormattedText: String? = null
    ): LogEntity = withContext(ioDispatcher) {
        val timestamp = System.currentTimeMillis()
        val sanitizedPayload = PiiSanitizer.sanitizeJson(dataPayload)

        val formattedText = if (!customFormattedText.isNullOrBlank()) {
            customFormattedText
        } else {
            val tempLog = LogEntity(
                timestamp = timestamp,
                action = action,
                dataPayload = sanitizedPayload,
                logType = logType,
                formattedText = ""
            )
            LogFormatter.formatForClipboard(tempLog)
        }

        val logEntity = LogEntity(
            timestamp = timestamp,
            action = action,
            dataPayload = sanitizedPayload,
            logType = logType,
            formattedText = formattedText
        )

        val id = logDao.insert(logEntity)
        val savedEntity = logEntity.copy(id = id)

        dispatchLegacyFileWrites(savedEntity)
        savedEntity
    }

    suspend fun getAllLogs(): List<LogEntity> = withContext(ioDispatcher) {
        logDao.getAllLogsList()
    }

    suspend fun searchLogs(query: String): List<LogEntity> = withContext(ioDispatcher) {
        if (query.isBlank()) {
            logDao.getAllLogsList()
        } else {
            logDao.searchLogsList(formatSearchQuery(query))
        }
    }

    suspend fun clearAllLogs() = withContext(ioDispatcher) {
        logDao.deleteAll()
    }

    suspend fun getLogsCount(): Long = withContext(ioDispatcher) {
        logDao.count().toLong()
    }

    private fun formatSearchQuery(query: String): String {
        val cleaned = query.trim().replace(Regex("[^a-zA-Z0-9_.]"), " ").trim()
        if (cleaned.isBlank()) {
            return "*"
        }
        return cleaned.split(Regex("\\s+")).joinToString(" ") { "$it*" }
    }

    private suspend fun dispatchLegacyFileWrites(log: LogEntity) {
        val allLogsJson = buildAllLogsJson(log)
        legacyFileWriter.writeAllLogs(allLogsJson)
        legacyFileWriter.writeAction(log.action)
        dispatchSpecializedAction(log)
    }

    private fun buildAllLogsJson(log: LogEntity): String {
        val dateFormatted = DateFormat.getDateTimeInstance().format(Date(log.timestamp))
        val allLogsJson = JSONObject()
        allLogsJson.put("action", log.action)
        allLogsJson.put("datetime", dateFormatted)
        if (log.dataPayload.isNotBlank() && log.dataPayload != "{}") {
            try {
                allLogsJson.put("data", JSONObject(log.dataPayload))
            } catch (_: JSONException) {
                try {
                    allLogsJson.put("data", JSONArray(log.dataPayload))
                } catch (_: JSONException) {
                    allLogsJson.put("data", log.dataPayload)
                }
            }
        }
        return allLogsJson.toString()
    }

    private suspend fun dispatchSpecializedAction(log: LogEntity) {
        when (log.action) {
            "android.intent.action.USER_PRESENT" -> legacyFileWriter.writeDeviceUsage(locked = false)
            "android.intent.action.CLOSE_SYSTEM_DIALOGS" -> legacyFileWriter.writeDeviceUsage(locked = true)
            "android.app.action.ACTION_PASSWORD_SUCCEEDED" -> legacyFileWriter.writePasswordAttempt(succeeded = true)
            "android.app.action.ACTION_PASSWORD_FAILED" -> legacyFileWriter.writePasswordAttempt(succeeded = false)
            "in.rahulja.getlogs.LAST_LOCATION" -> dispatchLocation(log.dataPayload)
            "android.net.wifi.SCAN_RESULTS" -> dispatchWifi(log.dataPayload)
        }
    }

    private suspend fun dispatchLocation(payload: String) {
        try {
            val json = JSONObject(payload)
            val lat = json.optString("latitude", "")
            val lng = json.optString("longitude", "")
            if (lat.isNotEmpty() && lng.isNotEmpty()) {
                legacyFileWriter.writeLocation(lat, lng)
            }
        } catch (_: JSONException) {
            // ignore invalid JSON
        }
    }

    private suspend fun dispatchWifi(payload: String) {
        try {
            val json = JSONObject(payload)
            val wifiInfo = if (json.has("wifiInfo")) {
                json.optString("wifiInfo", "")
            } else {
                json.optString("results", "")
            }
            if (wifiInfo.isNotEmpty()) {
                legacyFileWriter.writeWifiScan(wifiInfo)
            }
        } catch (_: JSONException) {
            // ignore invalid JSON
        }
    }

    companion object {
        const val PAGE_SIZE = 50

        @Volatile
        private var instance: LogRepository? = null

        fun getInstance(context: Context): LogRepository {
            return instance ?: synchronized(this) {
                instance ?: LogRepository(
                    logDao = LogDatabase.getInstance(context).logDao(),
                    legacyFileWriter = LegacyFileWriter(context)
                ).also { instance = it }
            }
        }
    }
}
