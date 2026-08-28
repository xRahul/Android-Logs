package `in`.rahulja.getlogs.service

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController

@RunWith(RobolectricTestRunner::class)
class TelemetryServiceTest {

    private lateinit var context: Context
    private lateinit var controller: ServiceController<TelemetryService>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        controller = Robolectric.buildService(TelemetryService::class.java)
    }

    @Test
    fun testServiceLifecycleAndIsRunningState() {
        assertFalse(TelemetryService.isRunning(context))

        controller.create().startCommand(0, 0)
        assertTrue(TelemetryService.isRunning(context))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(TelemetryService.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(TelemetryService.CHANNEL_NAME, channel.name)

        controller.destroy()
        assertFalse(TelemetryService.isRunning(context))
    }

    @Test
    fun testStartAndStopHelpers() {
        TelemetryService.start(context)
        TelemetryService.stop(context)
    }
}
