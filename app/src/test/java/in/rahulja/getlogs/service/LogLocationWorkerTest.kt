package `in`.rahulja.getlogs.service

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.Tasks
import `in`.rahulja.getlogs.data.LogRepository
import `in`.rahulja.getlogs.model.LogType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
class LogLocationWorkerTest {

    private lateinit var context: Context
    private lateinit var shadowApp: ShadowApplication
    private lateinit var logRepository: LogRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        shadowApp = Shadows.shadowOf(context as Application)
        shadowApp.denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        logRepository = mockk(relaxed = true)
        fusedLocationClient = mockk(relaxed = true)
    }

    private fun buildWorker(
        repo: LogRepository = logRepository,
        client: FusedLocationProviderClient = fusedLocationClient
    ): LogLocationWorker {
        val builder = TestListenableWorkerBuilder<LogLocationWorker>(context)
        builder.setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                return LogLocationWorker(appContext, workerParameters, repo, client)
            }
        })
        return builder.build()
    }

    @Test
    fun testDoWorkWithoutPermissionReturnsFailureGracefully() = runBlocking {
        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun testDoWorkWithFineLocationPermissionRecordsLocationAndSucceeds() = runBlocking {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        val mockLocation = mockk<Location> {
            every { latitude } returns 37.7749
            every { longitude } returns -122.4194
        }
        every { fusedLocationClient.lastLocation } returns Tasks.forResult(mockLocation)

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        val payloadSlot = slot<String>()
        coVerify(exactly = 1) {
            logRepository.recordLog(
                action = LogLocationWorker.ACTION_LAST_LOCATION,
                dataPayload = capture(payloadSlot),
                logType = LogType.LOCATION
            )
        }
        assertTrue(payloadSlot.captured.contains("37.7749"))
        assertTrue(payloadSlot.captured.contains("-122.4194"))
    }

    @Test
    fun testDoWorkWithCoarseLocationPermissionRecordsLocationAndSucceeds() = runBlocking {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

        val mockLocation = mockk<Location> {
            every { latitude } returns 40.7128
            every { longitude } returns -74.0060
        }
        every { fusedLocationClient.lastLocation } returns Tasks.forResult(mockLocation)

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            logRepository.recordLog(
                action = LogLocationWorker.ACTION_LAST_LOCATION,
                dataPayload = any(),
                logType = LogType.LOCATION
            )
        }
    }

    @Test
    fun testDoWorkWhenLocationIsNullRecordsAndSucceeds() = runBlocking {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        every { fusedLocationClient.lastLocation } returns Tasks.forResult(null)

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            logRepository.recordLog(
                action = LogLocationWorker.ACTION_LAST_LOCATION,
                dataPayload = any(),
                logType = LogType.LOCATION
            )
        }
    }

    @Test
    fun testDoWorkWhenLocationThrowsReturnsFailure() = runBlocking {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        every { fusedLocationClient.lastLocation } returns Tasks.forException(RuntimeException("GPS failed"))

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
