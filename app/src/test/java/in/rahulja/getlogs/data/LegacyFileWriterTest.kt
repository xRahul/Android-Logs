package `in`.rahulja.getlogs.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LegacyFileWriterTest {

    private lateinit var context: Context
    private lateinit var legacyFileWriter: LegacyFileWriter
    private lateinit var logFolder: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        legacyFileWriter = LegacyFileWriter(context)
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        logFolder = File(baseDir, LegacyFileWriter.LOG_FOLDER)
        if (logFolder.exists()) {
            logFolder.deleteRecursively()
        }
    }

    @After
    fun tearDown() {
        if (logFolder.exists()) {
            logFolder.deleteRecursively()
        }
    }

    @Test
    fun appendLogFileCreatesFolderAndAppends() = runBlocking {
        val fileName = "custom.txt"
        legacyFileWriter.appendLogFile(fileName, "line1")
        legacyFileWriter.appendLogFile(fileName, "line2")

        val targetFile = File(logFolder, fileName)
        assertTrue(targetFile.exists())

        val lines = targetFile.readLines()
        assertEquals(2, lines.size)
        assertEquals("line1", lines[0])
        assertEquals("line2", lines[1])
    }

    @Test
    fun writeAllLogsAppendsToAllLogsFile() = runBlocking {
        val jsonPayload = """{"action":"TEST_ACTION","datetime":"2026-08-28 12:00:00"}"""
        legacyFileWriter.writeAllLogs(jsonPayload)

        val targetFile = File(logFolder, LegacyFileWriter.ALL_LOGS_FILE)
        assertTrue(targetFile.exists())
        val content = targetFile.readText()
        assertTrue(content.contains(jsonPayload))
    }

    @Test
    fun writeActionAppendsToAllActionsFile() = runBlocking {
        legacyFileWriter.writeAction("android.intent.action.BATTERY_LOW")

        val targetFile = File(logFolder, LegacyFileWriter.ALL_ACTIONS_FILE)
        assertTrue(targetFile.exists())
        val content = targetFile.readText()
        assertTrue(content.contains("android.intent.action.BATTERY_LOW"))
    }

    @Test
    fun writeLocationAppendsCoordinates() = runBlocking {
        legacyFileWriter.writeLocation("37.7749", "-122.4194")

        val targetFile = File(logFolder, LegacyFileWriter.LOCATION_FILE)
        assertTrue(targetFile.exists())
        val content = targetFile.readText()
        assertTrue(content.contains("37.7749, -122.4194"))
    }

    @Test
    fun writePasswordAttemptAppendsSucceededAndFailed() = runBlocking {
        legacyFileWriter.writePasswordAttempt(succeeded = true)
        legacyFileWriter.writePasswordAttempt(succeeded = false)

        val targetFile = File(logFolder, LegacyFileWriter.PASS_WORD_FILE)
        assertTrue(targetFile.exists())
        val lines = targetFile.readLines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].endsWith("SUCCEEDED"))
        assertTrue(lines[1].endsWith("FAILED"))
    }

    @Test
    fun writeDeviceUsageAppendsLockedAndUnlocked() = runBlocking {
        legacyFileWriter.writeDeviceUsage(locked = true)
        legacyFileWriter.writeDeviceUsage(locked = false)

        val targetFile = File(logFolder, LegacyFileWriter.DEVICE_USED_FILE)
        assertTrue(targetFile.exists())
        val lines = targetFile.readLines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].endsWith("LOCKED"))
        assertTrue(lines[1].endsWith("UNLOCKED"))
    }

    @Test
    fun writeWifiScanAppendsInfo() = runBlocking {
        legacyFileWriter.writeWifiScan("[SSID: Office, BSSID: 00:11:22:33:44:55]")

        val targetFile = File(logFolder, LegacyFileWriter.WIFI_FILE)
        assertTrue(targetFile.exists())
        val content = targetFile.readText()
        assertTrue(content.contains("[SSID: Office, BSSID: 00:11:22:33:44:55]"))
    }
}
