package `in`.rahulja.getlogs.ui

import `in`.rahulja.getlogs.model.LogType

data class MainUiState(
    val searchQuery: String = "",
    val isTelemetryServiceRunning: Boolean = false,
    val selectedLogType: LogType? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val totalLogsCount: Long = 0L
)
