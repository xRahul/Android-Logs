package `in`.rahulja.getlogs.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import `in`.rahulja.getlogs.data.LogRepository
import `in`.rahulja.getlogs.model.LogEntity
import `in`.rahulja.getlogs.model.LogType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LogExporterTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LogRepository
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var mockUri: Uri
    private lateinit var logExporter: LogExporter

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        mockUri = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver

        logExporter = LogExporter(repository, testDispatcher)
    }

    @Test
    fun exportLogsToUri_Success_WritesFormattedLogsToOutputStream() = runTest(testDispatcher) {
        val logs = listOf(
            LogEntity(
                id = 1L,
                timestamp = 1700000000000L,
                action = "android.intent.action.BATTERY_LOW",
                dataPayload = """{"level":15}""",
                logType = LogType.GENERAL,
                formattedText = "2023-11-14 22:13:20\nandroid.intent.action\n\tBATTERY_LOW\n\t\tlevel: 15"
            ),
            LogEntity(
                id = 2L,
                timestamp = 1700000060000L,
                action = "in.rahulja.getlogs.LAST_LOCATION",
                dataPayload = """{"latitude":"37.7749","longitude":"-122.4194"}""",
                logType = LogType.LOCATION,
                formattedText = "2023-11-14 22:14:20\nin.rahulja.getlogs\n\tLAST_LOCATION\n\t\tlatitude: 37.7749"
            )
        )
        coEvery { repository.getAllLogs() } returns logs

        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(mockUri) } returns outputStream

        val result = logExporter.exportLogsToUri(context, mockUri)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        val writtenContent = outputStream.toString(Charsets.UTF_8.name())
        assertTrue(writtenContent.contains("BATTERY_LOW"))
        assertTrue(writtenContent.contains("LAST_LOCATION"))
        assertTrue(writtenContent.contains("37.7749"))
    }

    @Test
    fun exportLogsToUri_EmptyLogs_WritesNothingAndReturnsZeroCount() = runTest(testDispatcher) {
        coEvery { repository.getAllLogs() } returns emptyList()

        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(mockUri) } returns outputStream

        val result = logExporter.exportLogsToUri(context, mockUri)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        assertEquals("", outputStream.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun exportLogsToUri_FallbackToLogFormatterWhenFormattedTextBlank() = runTest(testDispatcher) {
        val logs = listOf(
            LogEntity(
                id = 1L,
                timestamp = 1700000000000L,
                action = "android.intent.action.BATTERY_OKAY",
                dataPayload = """{"level":80}""",
                logType = LogType.GENERAL,
                formattedText = ""
            )
        )
        coEvery { repository.getAllLogs() } returns logs

        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(mockUri) } returns outputStream

        val result = logExporter.exportLogsToUri(context, mockUri)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())

        val writtenContent = outputStream.toString(Charsets.UTF_8.name())
        assertTrue(writtenContent.contains("BATTERY_OKAY"))
        assertTrue(writtenContent.contains("level: 80"))
    }

    @Test
    fun exportLogsToUri_NullOutputStream_ReturnsFailure() = runTest(testDispatcher) {
        coEvery { repository.getAllLogs() } returns listOf(
            LogEntity(
                id = 1L,
                timestamp = 1000L,
                action = "ACTION",
                dataPayload = "{}",
                logType = LogType.GENERAL,
                formattedText = "Text"
            )
        )
        every { contentResolver.openOutputStream(mockUri) } returns null

        val result = logExporter.exportLogsToUri(context, mockUri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun exportLogsToUri_OutputStreamThrowsException_ReturnsFailure() = runTest(testDispatcher) {
        coEvery { repository.getAllLogs() } returns listOf(
            LogEntity(
                id = 1L,
                timestamp = 1000L,
                action = "ACTION",
                dataPayload = "{}",
                logType = LogType.GENERAL,
                formattedText = "Text"
            )
        )
        every { contentResolver.openOutputStream(mockUri) } throws SecurityException("Permission denied")

        val result = logExporter.exportLogsToUri(context, mockUri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun exportLogsToUri_RepositoryThrowsException_ReturnsFailure() = runTest(testDispatcher) {
        coEvery { repository.getAllLogs() } throws RuntimeException("Database error")

        val result = logExporter.exportLogsToUri(context, mockUri)

        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }
}
