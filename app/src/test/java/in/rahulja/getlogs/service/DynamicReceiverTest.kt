package `in`.rahulja.getlogs.service

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import `in`.rahulja.getlogs.data.LogRepository
import `in`.rahulja.getlogs.model.LogType
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowApplication

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DynamicReceiverTest {

    private lateinit var context: Context
    private lateinit var shadowApp: ShadowApplication
    private lateinit var logRepository: LogRepository
    private lateinit var mockWorkManager: WorkManager
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        shadowApp = Shadows.shadowOf(context as Application)
        logRepository = mockk(relaxed = true)
        mockWorkManager = mockk(relaxed = true)
    }

    @Test
    fun testReceiveGeneralBroadcastRecordsLog() = testScope.runTest {
        val receiver = DynamicReceiver(
            logRepositoryProvider = { logRepository },
            coroutineScope = this,
            workManagerProvider = { mockWorkManager }
        )

        val intent = Intent(Intent.ACTION_BATTERY_LOW).apply {
            putExtra("level", 15)
        }

        receiver.onReceive(context, intent)
        advanceUntilIdle()

        val payloadSlot = slot<String>()
        coVerify(exactly = 1) {
            logRepository.recordLog(
                action = Intent.ACTION_BATTERY_LOW,
                dataPayload = capture(payloadSlot),
                logType = LogType.SYSTEM
            )
        }
        assertTrue(payloadSlot.captured.contains("15"))
    }

    @Test
    fun testReceiveCloseSystemDialogsEnqueuesWorkAndRecordsLog() = testScope.runTest {
        val receiver = DynamicReceiver(
            logRepositoryProvider = { logRepository },
            coroutineScope = this,
            workManagerProvider = { mockWorkManager }
        )

        val intent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            logRepository.recordLog(
                action = Intent.ACTION_CLOSE_SYSTEM_DIALOGS,
                dataPayload = any(),
                logType = LogType.GENERAL
            )
        }

        io.mockk.verify(exactly = 1) { mockWorkManager.enqueue(any<androidx.work.WorkRequest>()) }
    }

    @Test
    fun testReceiveUserPresentEnqueuesWorkAndRecordsLog() = testScope.runTest {
        val receiver = DynamicReceiver(
            logRepositoryProvider = { logRepository },
            coroutineScope = this,
            workManagerProvider = { mockWorkManager }
        )

        val intent = Intent(Intent.ACTION_USER_PRESENT)
        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            logRepository.recordLog(
                action = Intent.ACTION_USER_PRESENT,
                dataPayload = any(),
                logType = LogType.GENERAL
            )
        }

        io.mockk.verify(exactly = 1) { mockWorkManager.enqueue(any<androidx.work.WorkRequest>()) }
    }

    @Test
    fun testReceiveNullOrEmptyActionIsHandledGracefully() = testScope.runTest {
        val receiver = DynamicReceiver(
            logRepositoryProvider = { logRepository },
            coroutineScope = this
        )

        val emptyIntent = Intent()
        receiver.onReceive(context, emptyIntent)
        receiver.onReceive(context, null)
        advanceUntilIdle()

        coVerify(exactly = 0) {
            logRepository.recordLog(any(), any(), any(), any())
        }
    }

    @Test
    fun testReceiveWifiScanResultsWithPermissionRecordsWifiLog() = testScope.runTest {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        val receiver = DynamicReceiver(
            logRepositoryProvider = { logRepository },
            coroutineScope = this
        )

        val intent = Intent("android.net.wifi.SCAN_RESULTS").apply {
            putExtra("resultsUpdated", true)
        }

        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            logRepository.recordLog(
                action = "android.net.wifi.SCAN_RESULTS",
                dataPayload = any(),
                logType = LogType.WIFI
            )
        }
    }
}
