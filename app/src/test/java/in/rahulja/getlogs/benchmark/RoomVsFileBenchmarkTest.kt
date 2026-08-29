package `in`.rahulja.getlogs.benchmark

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.getlogs.data.LogDao
import `in`.rahulja.getlogs.data.LogDatabase
import `in`.rahulja.getlogs.model.LogEntity
import `in`.rahulja.getlogs.model.LogType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
class RoomVsFileBenchmarkTest {

    private lateinit var context: Context
    private lateinit var database: LogDatabase
    private lateinit var logDao: LogDao
    private lateinit var tempLogFile: File

    companion object {
        private const val BENCHMARK_ITEM_COUNT = 500
        private const val SEARCH_KEYWORD = "CRITICAL_ALERT"
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        logDao = database.logDao()

        tempLogFile = File(context.cacheDir, "benchmark_logs.txt")
        if (tempLogFile.exists()) {
            tempLogFile.delete()
        }
    }

    @After
    fun tearDown() {
        database.close()
        if (tempLogFile.exists()) {
            tempLogFile.delete()
        }
    }

    @Test
    fun benchmarkInsertionPerformance() = runBlocking {
        val sampleEntries = generateSampleEntries(BENCHMARK_ITEM_COUNT)

        // Benchmark File Appending
        val fileWriteTimeMs = measureTimeMillis {
            tempLogFile.bufferedWriter().use { writer ->
                sampleEntries.forEach { entry ->
                    val line = "${entry.timestamp} [${entry.logType}] ${entry.action}: ${entry.dataPayload}\n"
                    writer.write(line)
                }
            }
        }

        // Benchmark Room Insertion with FTS Indexing
        val roomInsertTimeMs = measureTimeMillis {
            sampleEntries.forEach { entry ->
                logDao.insert(entry)
            }
        }

        assertTrue("File should exist after writing", tempLogFile.exists())
        assertEquals("Room count should match sample size", BENCHMARK_ITEM_COUNT, logDao.count())

        println("=== BENCHMARK INSERTION ($BENCHMARK_ITEM_COUNT items) ===")
        println("File sequential append time: ${fileWriteTimeMs}ms")
        println("Room DB + FTS index insert time: ${roomInsertTimeMs}ms")
    }

    @Test
    fun benchmarkFullTextSearchPerformance() = runBlocking {
        val sampleEntries = generateSampleEntries(BENCHMARK_ITEM_COUNT)

        // Seed data in file and Room
        tempLogFile.bufferedWriter().use { writer ->
            sampleEntries.forEach { entry ->
                val line = "${entry.timestamp} [${entry.logType}] ${entry.action}: " +
                    "${entry.dataPayload} - ${entry.formattedText}\n"
                writer.write(line)
            }
        }

        sampleEntries.forEach { entry ->
            logDao.insert(entry)
        }

        // Search File line-by-line (sequential grep)
        var fileFoundCount = 0
        val fileSearchTimeMs = measureTimeMillis {
            tempLogFile.useLines { lines ->
                fileFoundCount = lines.count { it.contains(SEARCH_KEYWORD) }
            }
        }

        // Search Room using SQLite FTS index
        var roomResults: List<LogEntity> = emptyList()
        val roomSearchTimeMs = measureTimeMillis {
            roomResults = logDao.searchLogsList("$SEARCH_KEYWORD*")
        }

        assertTrue("Should have matched records", fileFoundCount > 0)
        assertEquals("FTS match count should equal file grep count", fileFoundCount, roomResults.size)

        println("=== BENCHMARK FULL TEXT SEARCH ($BENCHMARK_ITEM_COUNT items) ===")
        println("File sequential grep search time: ${fileSearchTimeMs}ms (matches: $fileFoundCount)")
        println("Room FTS index search time: ${roomSearchTimeMs}ms (matches: ${roomResults.size})")
    }

    @Test
    fun benchmarkSearchAccuracyAndOrdering() = runBlocking {
        val baseTime = 1700000000000L
        val entry1 = LogEntity(
            timestamp = baseTime + 1000,
            action = "android.intent.action.BATTERY_LOW",
            dataPayload = """{"level": 10, "state": "discharging"}""",
            logType = LogType.GENERAL,
            formattedText = "BATTERY_LOW level: 10"
        )
        val entry2 = LogEntity(
            timestamp = baseTime + 2000,
            action = "android.intent.action.BATTERY_CHANGED",
            dataPayload = """{"level": 15, "state": "charging"}""",
            logType = LogType.GENERAL,
            formattedText = "BATTERY_CHANGED level: 15"
        )
        val entry3 = LogEntity(
            timestamp = baseTime + 3000,
            action = "in.rahulja.getlogs.LOCATION",
            dataPayload = """{"lat": 37.7749, "lng": -122.4194}""",
            logType = LogType.LOCATION,
            formattedText = "LOCATION: 37.7749, -122.4194"
        )

        logDao.insert(entry1)
        logDao.insert(entry2)
        logDao.insert(entry3)

        val results = logDao.searchLogsList("BATTERY*")
        assertEquals(2, results.size)
        // Should be ordered timestamp DESC (entry2 first, then entry1)
        assertEquals("android.intent.action.BATTERY_CHANGED", results[0].action)
        assertEquals("android.intent.action.BATTERY_LOW", results[1].action)
    }

    private fun generateSampleEntries(count: Int): List<LogEntity> {
        val baseTime = 1700000000000L
        return (1..count).map { index ->
            val isTarget = index % 25 == 0
            val actionName = if (isTarget) {
                "in.rahulja.getlogs.event.$SEARCH_KEYWORD"
            } else {
                "android.intent.action.EVENT_$index"
            }
            val payload = if (isTarget) {
                """{"id": $index, "tag": "$SEARCH_KEYWORD", "severity": "HIGH"}"""
            } else {
                """{"id": $index, "tag": "NORMAL_EVENT", "status": "OK"}"""
            }
            LogEntity(
                timestamp = baseTime + index * 1000,
                action = actionName,
                dataPayload = payload,
                logType = if (isTarget) LogType.SECURITY else LogType.GENERAL,
                formattedText = "Event #$index: $actionName payload=$payload"
            )
        }
    }
}
