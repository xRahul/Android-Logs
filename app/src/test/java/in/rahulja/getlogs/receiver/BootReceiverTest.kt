package `in`.rahulja.getlogs.receiver

import android.app.Application
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
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var logRepository: LogRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private var serviceStarted = false

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        logRepository = mockk(relaxed = true)
        serviceStarted = false
    }

    @Test
    fun testReceiveBootCompletedRecordsLogAndStartsService() = testScope.runTest {
        val receiver = BootReceiver(
            logRepositoryProvider = { logRepository },
            ioDispatcher = testDispatcher,
            coroutineScope = this,
            serviceStarter = { serviceStarted = true }
        )

        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            logRepository.recordLog(
                action = Intent.ACTION_BOOT_COMPLETED,
                dataPayload = "{}",
                logType = LogType.SYSTEM
            )
        }
        org.junit.Assert.assertTrue(serviceStarted)
    }

    @Test
    fun testReceiveMyPackageReplacedRecordsLogAndStartsService() = testScope.runTest {
        val receiver = BootReceiver(
            logRepositoryProvider = { logRepository },
            ioDispatcher = testDispatcher,
            coroutineScope = this,
            serviceStarter = { serviceStarted = true }
        )

        val intent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED)
        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            logRepository.recordLog(
                action = Intent.ACTION_MY_PACKAGE_REPLACED,
                dataPayload = "{}",
                logType = LogType.SYSTEM
            )
        }
        org.junit.Assert.assertTrue(serviceStarted)
    }

    @Test
    fun testReceiveNullIntentOrActionDoesNothing() = testScope.runTest {
        val receiver = BootReceiver(
            logRepositoryProvider = { logRepository },
            ioDispatcher = testDispatcher,
            coroutineScope = this,
            serviceStarter = { serviceStarted = true }
        )

        receiver.onReceive(context, null)
        receiver.onReceive(null, Intent(Intent.ACTION_BOOT_COMPLETED))
        receiver.onReceive(context, Intent())
        advanceUntilIdle()

        coVerify(exactly = 0) {
            logRepository.recordLog(any(), any(), any(), any())
        }
        org.junit.Assert.assertFalse(serviceStarted)
    }
}
