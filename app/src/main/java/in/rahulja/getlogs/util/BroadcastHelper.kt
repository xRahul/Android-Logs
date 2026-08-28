package `in`.rahulja.getlogs.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat
import `in`.rahulja.getlogs.model.LogType
import `in`.rahulja.getlogs.service.LogLocationWorker
import org.json.JSONObject

object BroadcastHelper {

    fun extractExtras(context: Context, intent: Intent): String {
        val json = JSONObject()
        val extras = intent.extras
        if (extras != null) {
            for (key in extras.keySet()) {
                val value = extras.get(key)
                if (value != null) {
                    json.put(key, value.toString())
                }
            }
        }

        if (intent.action == "android.net.wifi.SCAN_RESULTS") {
            val hasFineLocation = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasFineLocation) {
                val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val scanResults = wifiManager?.scanResults
                if (scanResults != null) {
                    json.put("wifiInfo", scanResults.toString())
                }
            }
        }

        return json.toString()
    }

    fun determineLogType(action: String): LogType {
        return when {
            action == LogLocationWorker.ACTION_LAST_LOCATION -> LogType.LOCATION
            action.startsWith("android.net.wifi.") -> LogType.WIFI
            action.startsWith("android.app.action.ACTION_PASSWORD_") ||
                action.startsWith("android.app.action.DEVICE_ADMIN_") ||
                action.startsWith("android.app.action.DEVICE_OWNER_") ||
                action.startsWith("android.app.action.PROFILE_PROVISIONING_COMPLETE") ||
                action.startsWith("android.app.action.LOCK_TASK_") -> LogType.SECURITY
            action.startsWith("android.intent.action.BOOT_COMPLETED") ||
                action.startsWith("android.intent.action.BATTERY_") ||
                action.startsWith("android.intent.action.POWER_") ||
                action.startsWith("android.intent.action.ACTION_SHUTDOWN") ||
                action.startsWith("android.intent.action.REBOOT") -> LogType.SYSTEM
            else -> LogType.GENERAL
        }
    }
}
