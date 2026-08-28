package `in`.rahulja.getlogs.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import `in`.rahulja.getlogs.data.LogRepository
import `in`.rahulja.getlogs.model.LogType
import org.json.JSONObject

class LogLocationWorker @JvmOverloads constructor(
    appContext: Context,
    params: WorkerParameters,
    logRepo: LogRepository? = null,
    locationClient: FusedLocationProviderClient? = null
) : CoroutineWorker(appContext, params) {

    private val logRepository: LogRepository = logRepo ?: LogRepository.getInstance(appContext)
    private val fusedLocationClient: FusedLocationProviderClient =
        locationClient ?: LocationServices.getFusedLocationProviderClient(appContext)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result {
        val hasFine = ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            logRepository.recordLog(
                action = ACTION_LAST_LOCATION,
                dataPayload = JSONObject().apply {
                    put("error", "PERMISSION_DENIED")
                }.toString(),
                logType = LogType.LOCATION
            )
            return Result.failure()
        }

        return try {
            val location: Location? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.google.android.gms.tasks.Tasks.await(fusedLocationClient.lastLocation)
            }
            val payload = JSONObject().apply {
                if (location != null) {
                    put("latitude", location.latitude.toString())
                    put("longitude", location.longitude.toString())
                } else {
                    put("latitude", "")
                    put("longitude", "")
                }
            }.toString()

            logRepository.recordLog(
                action = ACTION_LAST_LOCATION,
                dataPayload = payload,
                logType = LogType.LOCATION
            )
            Result.success()
        } catch (e: Exception) {
            logRepository.recordLog(
                action = ACTION_LAST_LOCATION,
                dataPayload = JSONObject().apply {
                    put("error", e.message ?: "ERROR_GETTING")
                }.toString(),
                logType = LogType.LOCATION
            )
            Result.failure()
        }
    }

    companion object {
        const val ACTION_LAST_LOCATION = "in.rahulja.getlogs.LAST_LOCATION"
        const val TAG = "LogLocationWorker"
    }
}
