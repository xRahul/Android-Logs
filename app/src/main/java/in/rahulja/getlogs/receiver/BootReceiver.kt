package `in`.rahulja.getlogs.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.rahulja.getlogs.data.LogRepository
import `in`.rahulja.getlogs.model.LogType
import `in`.rahulja.getlogs.service.TelemetryService
import `in`.rahulja.getlogs.util.BroadcastHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver(
    private val logRepositoryProvider: (Context) -> LogRepository = { context ->
        LogRepository.getInstance(context.applicationContext)
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher),
    private val serviceStarter: (Context) -> Unit = { context ->
        TelemetryService.start(context)
    }
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action ?: return

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        coroutineScope.launch {
            try {
                val extrasJson = BroadcastHelper.extractExtras(appContext, intent)
                val repository = logRepositoryProvider(appContext)

                repository.recordLog(
                    action = action,
                    dataPayload = extrasJson,
                    logType = LogType.SYSTEM
                )

                if (action == Intent.ACTION_BOOT_COMPLETED ||
                    action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                    action == ACTION_QUICKBOOT_POWERON
                ) {
                    serviceStarter(appContext)
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
