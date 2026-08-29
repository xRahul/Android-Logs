package `in`.rahulja.getlogs.data

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.getlogs.model.LogEntity
import `in`.rahulja.getlogs.model.LogType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LogDaoTest {

    private lateinit var database: LogDatabase
    private lateinit var logDao: LogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        logDao = database.logDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetAllLogsList() = runBlocking {
        val log1 = LogEntity(
            timestamp = 1000L,
            action = "android.intent.action.BATTERY_LOW",
            dataPayload = """{"level":15}""",
            logType = LogType.GENERAL,
            formattedText = "BATTERY_LOW 15"
        )
        val log2 = LogEntity(
            timestamp = 2000L,
            action = "android.intent.action.SCREEN_ON",
            dataPayload = "{}",
            logType = LogType.GENERAL,
            formattedText = "SCREEN_ON"
        )

        val id1 = logDao.insert(log1)
        val id2 = logDao.insert(log2)

        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        val logs = logDao.getAllLogsList()
        assertEquals(2, logs.size)
        assertEquals("android.intent.action.SCREEN_ON", logs[0].action)
        assertEquals("android.intent.action.BATTERY_LOW", logs[1].action)
    }

    @Test
    fun insertAndSearchLogsByAction() = runBlocking {
        val log1 = LogEntity(
            timestamp = 1000L,
            action = "android.intent.action.BATTERY_LOW",
            dataPayload = """{"level":15}""",
            logType = LogType.GENERAL,
            formattedText = "BATTERY_LOW"
        )
        val log2 = LogEntity(
            timestamp = 2000L,
            action = "android.intent.action.SCREEN_OFF",
            dataPayload = "{}",
            logType = LogType.GENERAL,
            formattedText = "SCREEN_OFF"
        )

        logDao.insert(log1)
        logDao.insert(log2)

        val results = logDao.searchLogsList("BATTERY*")
        assertEquals(1, results.size)
        assertEquals("android.intent.action.BATTERY_LOW", results[0].action)
    }

    @Test
    fun insertAndSearchLogsByPayload() = runBlocking {
        val log1 = LogEntity(
            timestamp = 1000L,
            action = "in.rahulja.getlogs.WIFI_SCAN",
            dataPayload = """{"ssid":"HomeWifiNetwork"}""",
            logType = LogType.WIFI,
            formattedText = "WIFI: HomeWifiNetwork"
        )
        val log2 = LogEntity(
            timestamp = 2000L,
            action = "in.rahulja.getlogs.WIFI_SCAN",
            dataPayload = """{"ssid":"OfficeWifiNetwork"}""",
            logType = LogType.WIFI,
            formattedText = "WIFI: OfficeWifiNetwork"
        )

        logDao.insert(log1)
        logDao.insert(log2)

        val results = logDao.searchLogsList("HomeWifiNetwork*")
        assertEquals(1, results.size)
        assertEquals("""{"ssid":"HomeWifiNetwork"}""", results[0].dataPayload)
    }

    @Test
    fun insertAndSearchLogsByFormattedText() = runBlocking {
        val log1 = LogEntity(
            timestamp = 1000L,
            action = "android.app.action.ACTION_PASSWORD_FAILED",
            dataPayload = "{}",
            logType = LogType.SECURITY,
            formattedText = "Device security: Authentication failed"
        )

        logDao.insert(log1)

        val results = logDao.searchLogsList("Authentication*")
        assertEquals(1, results.size)
        assertEquals(log1.formattedText, results[0].formattedText)
    }

    @Test
    fun pagingSourceGetAllLogs() = runBlocking {
        val log = LogEntity(
            timestamp = 5000L,
            action = "android.intent.action.USER_PRESENT",
            dataPayload = "{}",
            logType = LogType.GENERAL,
            formattedText = "UNLOCKED"
        )
        logDao.insert(log)

        val pagingSource = logDao.getAllLogsPaging()
        val loadResult = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        )

        assertTrue(loadResult is PagingSource.LoadResult.Page<*, *>)
        val page = loadResult as PagingSource.LoadResult.Page<Int, LogEntity>
        assertEquals(1, page.data.size)
        assertEquals("android.intent.action.USER_PRESENT", page.data[0].action)
    }

    @Test
    fun pagingSourceSearchLogs() = runBlocking {
        val log1 = LogEntity(
            timestamp = 1000L,
            action = "in.rahulja.getlogs.LOCATION",
            dataPayload = """{"latitude":"37.7749","longitude":"-122.4194"}""",
            logType = LogType.LOCATION,
            formattedText = "Location: 37.7749, -122.4194"
        )
        val log2 = LogEntity(
            timestamp = 2000L,
            action = "android.intent.action.SCREEN_ON",
            dataPayload = "{}",
            logType = LogType.GENERAL,
            formattedText = "SCREEN_ON"
        )
        logDao.insert(log1)
        logDao.insert(log2)

        val pagingSource = logDao.searchLogsPaging("LOCATION*")
        val loadResult = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        )

        assertTrue(loadResult is PagingSource.LoadResult.Page<*, *>)
        val page = loadResult as PagingSource.LoadResult.Page<Int, LogEntity>
        assertEquals(1, page.data.size)
        assertEquals("in.rahulja.getlogs.LOCATION", page.data[0].action)
    }

    @Test
    fun pagingSourceWithLogTypeFilter() = runBlocking {
        val log1 = LogEntity(
            timestamp = 1000L,
            action = "in.rahulja.getlogs.LOCATION",
            dataPayload = """{"latitude":"37.7749","longitude":"-122.4194"}""",
            logType = LogType.LOCATION,
            formattedText = "Location: 37.7749"
        )
        val log2 = LogEntity(
            timestamp = 2000L,
            action = "android.intent.action.SCREEN_ON",
            dataPayload = "{}",
            logType = LogType.GENERAL,
            formattedText = "SCREEN_ON"
        )
        logDao.insert(log1)
        logDao.insert(log2)

        val locationPaging = logDao.getAllLogsPaging(LogType.LOCATION)
        val locationResult = locationPaging.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page<Int, LogEntity>
        assertEquals(1, locationResult.data.size)
        assertEquals("in.rahulja.getlogs.LOCATION", locationResult.data[0].action)

        val wifiPaging = logDao.getAllLogsPaging(LogType.WIFI)
        val wifiResult = wifiPaging.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page<Int, LogEntity>
        assertEquals(0, wifiResult.data.size)
    }

    @Test
    fun helperMethodsCountAndGetByIdAndClear() = runBlocking {
        val log = LogEntity(
            timestamp = 3000L,
            action = "test.action",
            dataPayload = "{}",
            logType = LogType.GENERAL,
            formattedText = "TEST"
        )
        val insertedId = logDao.insert(log)
        assertEquals(1, logDao.count())

        val fetched = logDao.getLogById(insertedId)
        assertNotNull(fetched)
        assertEquals("test.action", fetched?.action)

        logDao.deleteAll()
        assertEquals(0, logDao.count())
    }
}
