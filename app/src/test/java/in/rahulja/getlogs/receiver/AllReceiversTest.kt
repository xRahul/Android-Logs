package `in`.rahulja.getlogs.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.getlogs.data.LogRepository
import `in`.rahulja.getlogs.model.LogType
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AllReceiversTest {

    private lateinit var context: Context
    private lateinit var logRepository: LogRepository
    private lateinit var mockWorkManager: androidx.work.WorkManager
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        logRepository = mockk(relaxed = true)
        mockWorkManager = mockk(relaxed = true)
    }

    @Test
    fun testReceivePasswordSucceededRecordsSecurityLog() = testScope.runTest {
        val receiver = AllReceivers(
            logRepositoryProvider = { logRepository },
            coroutineScope = this,
            workManagerProvider = { mockWorkManager }
        )

        val intent = Intent(DeviceAdminReceiver.ACTION_PASSWORD_SUCCEEDED)
        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            logRepository.recordLog(
                action = DeviceAdminReceiver.ACTION_PASSWORD_SUCCEEDED,
                dataPayload = any(),
                logType = LogType.SECURITY
            )
        }
    }

    @Test
    fun testReceivePasswordFailedRecordsSecurityLog() = testScope.runTest {
        val receiver = AllReceivers(
            logRepositoryProvider = { logRepository },
            coroutineScope = this,
            workManagerProvider = { mockWorkManager }
        )

        val intent = Intent(DeviceAdminReceiver.ACTION_PASSWORD_FAILED)
        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            logRepository.recordLog(
                action = DeviceAdminReceiver.ACTION_PASSWORD_FAILED,
                dataPayload = any(),
                logType = LogType.SECURITY
            )
        }
    }

    @Test
    fun testReceiveDeviceAdminEnabledRecordsSecurityLog() = testScope.runTest {
        val receiver = AllReceivers(
            logRepositoryProvider = { logRepository },
            coroutineScope = this,
            workManagerProvider = { mockWorkManager }
        )

        val intent = Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED)
        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            logRepository.recordLog(
                action = DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED,
                dataPayload = any(),
                logType = LogType.SECURITY
            )
        }
    }

    @Test
    fun testReceiveCloseSystemDialogsEnqueuesWorker() = testScope.runTest {
        val receiver = AllReceivers(
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
    fun testReceiveUserPresentEnqueuesWorker() = testScope.runTest {
        val receiver = AllReceivers(
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
    fun testReceiveNullActionHandledGracefully() = testScope.runTest {
        val receiver = AllReceivers(
            logRepositoryProvider = { logRepository },
            coroutineScope = this,
            workManagerProvider = { mockWorkManager }
        )

        val intent = Intent()
        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 0) {
            logRepository.recordLog(any(), any(), any(), any())
        }
    }
}
