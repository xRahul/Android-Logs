package `in`.rahulja.getlogs.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import `in`.rahulja.getlogs.data.LogRepository
import `in`.rahulja.getlogs.util.BroadcastHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DynamicReceiver(
    private val logRepositoryProvider: (Context) -> LogRepository = { context ->
        LogRepository.getInstance(context.applicationContext)
    },
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val workManagerProvider: (Context) -> WorkManager? = { context ->
        try { WorkManager.getInstance(context.applicationContext) } catch (_: Exception) { null }
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
                val logType = BroadcastHelper.determineLogType(action)
                val repository = logRepositoryProvider(appContext)

                repository.recordLog(
                    action = action,
                    dataPayload = extrasJson,
                    logType = logType
                )

                if (action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS || action == Intent.ACTION_USER_PRESENT) {
                    val workRequest = OneTimeWorkRequestBuilder<LogLocationWorker>()
                        .addTag(LogLocationWorker.TAG)
                        .build()
                    workManagerProvider(appContext)?.enqueue(workRequest)
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
