package `in`.rahulja.getlogs.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import `in`.rahulja.getlogs.R

class TelemetryService : Service() {

    private val dynamicReceiver = DynamicReceiver()
    private var isReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        isRunningFlag = true
        createNotificationChannel()
        startForegroundServiceNotification()
        registerDynamicReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunningFlag = true
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterDynamicReceiver()
        isRunningFlag = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing telemetry service notification channel"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Android Logs Telemetry")
            .setContentText("Monitoring system events in background")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                0
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun registerDynamicReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_BATTERY_OKAY)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_USER_UNLOCKED)
                addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                addAction(Intent.ACTION_CONFIGURATION_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_HEADSET_PLUG)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                addAction("android.net.wifi.SCAN_RESULTS")
                addAction("android.net.wifi.STATE_CHANGE")
                addAction("android.net.wifi.WIFI_STATE_CHANGED")
                addAction("android.net.conn.CONNECTIVITY_CHANGE")
                addAction("android.bluetooth.adapter.action.STATE_CHANGED")
                addAction("android.bluetooth.device.action.ACL_CONNECTED")
                addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
                addAction(LogLocationWorker.ACTION_LAST_LOCATION)
            }
            ContextCompat.registerReceiver(
                this,
                dynamicReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true
        }
    }

    private fun unregisterDynamicReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(dynamicReceiver)
            } catch (_: IllegalArgumentException) {
                // Ignore if not registered
            }
            isReceiverRegistered = false
        }
    }

    companion object {
        const val CHANNEL_ID = "telemetry_channel"
        const val CHANNEL_NAME = "Telemetry Service"
        const val NOTIFICATION_ID = 1001

        @Volatile
        private var isRunningFlag = false

        fun start(context: Context) {
            val intent = Intent(context, TelemetryService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TelemetryService::class.java)
            context.stopService(intent)
        }

        @Suppress("UnusedParameter")
        fun isRunning(context: Context? = null): Boolean {
            return isRunningFlag
        }
    }
}
