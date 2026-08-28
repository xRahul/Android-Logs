package `in`.rahulja.getlogs.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.getlogs.model.LogType
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LogRepositoryTest {

    private lateinit var database: LogDatabase
    private lateinit var logDao: LogDao
    private lateinit var legacyFileWriter: LegacyFileWriter
    private lateinit var repository: LogRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        logDao = database.logDao()
        legacyFileWriter = mockk(relaxed = true)
        repository = LogRepository(logDao, legacyFileWriter)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun recordLogSanitizesPiiAndSavesToRoom() = runBlocking {
        val sensitivePayload = """{"password":"superSecret123","level":90}"""
        val saved = repository.recordLog(
            action = "android.intent.action.BATTERY_LOW",
            dataPayload = sensitivePayload,
            logType = LogType.GENERAL
        )

        assertTrue(saved.id > 0)
        assertFalse(saved.dataPayload.contains("superSecret123"))
        assertTrue(saved.dataPayload.contains("\"password\":\"[REDACTED]\""))
        assertTrue(saved.formattedText.isNotBlank())

        val inDb = logDao.getLogById(saved.id)
        assertNotNull(inDb)
        assertFalse(inDb?.dataPayload?.contains("superSecret123") ?: true)
    }

    @Test
    fun recordLogPreservesCustomFormattedText() = runBlocking {
        val customText = "Custom Formatted String"
        val saved = repository.recordLog(
            action = "CUSTOM_ACTION",
            dataPayload = "{}",
            logType = LogType.SYSTEM,
            customFormattedText = customText
        )

        assertEquals(customText, saved.formattedText)
        val inDb = logDao.getLogById(saved.id)
        assertEquals(customText, inDb?.formattedText)
    }

    @Test
    fun recordLogDispatchesToLegacyFileWriterGeneral() = runBlocking {
        val payload = """{"status":"ok"}"""
        repository.recordLog(
            action = "android.intent.action.SCREEN_ON",
            dataPayload = payload,
            logType = LogType.GENERAL
        )

        val jsonSlot = slot<String>()
        coVerify(exactly = 1) { legacyFileWriter.writeAllLogs(capture(jsonSlot)) }
        coVerify(exactly = 1) { legacyFileWriter.writeAction("android.intent.action.SCREEN_ON") }

        assertTrue(jsonSlot.captured.contains("android.intent.action.SCREEN_ON"))
    }

    @Test
    fun recordLogDispatchesSecurityActions() = runBlocking {
        repository.recordLog(
            action = "android.app.action.ACTION_PASSWORD_SUCCEEDED",
            dataPayload = "{}",
            logType = LogType.SECURITY
        )
        repository.recordLog(
            action = "android.app.action.ACTION_PASSWORD_FAILED",
            dataPayload = "{}",
            logType = LogType.SECURITY
        )

        coVerify(exactly = 1) { legacyFileWriter.writePasswordAttempt(succeeded = true) }
        coVerify(exactly = 1) { legacyFileWriter.writePasswordAttempt(succeeded = false) }
    }

    @Test
    fun recordLogDispatchesDeviceUsageActions() = runBlocking {
        repository.recordLog(
            action = "android.intent.action.USER_PRESENT",
            dataPayload = "{}",
            logType = LogType.GENERAL
        )
        repository.recordLog(
            action = "android.intent.action.CLOSE_SYSTEM_DIALOGS",
            dataPayload = "{}",
            logType = LogType.GENERAL
        )

        coVerify(exactly = 1) { legacyFileWriter.writeDeviceUsage(locked = false) }
        coVerify(exactly = 1) { legacyFileWriter.writeDeviceUsage(locked = true) }
    }

    @Test
    fun recordLogDispatchesLocationAndWifi() = runBlocking {
        val locPayload = """{"latitude":"37.7749","longitude":"-122.4194"}"""
        repository.recordLog(
            action = "in.rahulja.getlogs.LAST_LOCATION",
            dataPayload = locPayload,
            logType = LogType.LOCATION
        )

        val wifiPayload = """{"wifiInfo":"SSID_TEST, -65dBm"}"""
        repository.recordLog(
            action = "android.net.wifi.SCAN_RESULTS",
            dataPayload = wifiPayload,
            logType = LogType.WIFI
        )

        coVerify(exactly = 1) { legacyFileWriter.writeLocation("37.7749", "-122.4194") }
        coVerify(exactly = 1) { legacyFileWriter.writeWifiScan("SSID_TEST, -65dBm") }
    }

    @Test
    fun getLogsPagingReturnsFlow() = runBlocking {
        repository.recordLog("ACTION_1")
        repository.recordLog("ACTION_2")

        val pagingDataFlow = repository.getLogsPaging(query = null)
        assertNotNull(pagingDataFlow.first())

        val searchPagingDataFlow = repository.getLogsPaging(query = "ACTION_1")
        assertNotNull(searchPagingDataFlow.first())
    }
}
