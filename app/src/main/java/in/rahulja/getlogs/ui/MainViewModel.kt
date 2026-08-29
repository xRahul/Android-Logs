package `in`.rahulja.getlogs.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import `in`.rahulja.getlogs.data.LogRepository
import `in`.rahulja.getlogs.model.LogEntity
import `in`.rahulja.getlogs.model.LogType
import `in`.rahulja.getlogs.service.TelemetryService
import `in`.rahulja.getlogs.util.LogExporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: LogRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val logExporter: LogExporter = LogExporter(repository, ioDispatcher)
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val logsPagingFlow: Flow<PagingData<LogEntity>> = _uiState
        .map { Pair(it.searchQuery, it.selectedLogType) }
        .distinctUntilChanged()
        .debounce(DEBOUNCE_MILLIS)
        .flatMapLatest { (query, logType) ->
            repository.getLogsPaging(
                query = query.ifBlank { null },
                logType = logType
            )
        }
        .cachedIn(viewModelScope)

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onLogTypeFilterSelected(logType: LogType?) {
        _uiState.update { it.copy(selectedLogType = logType) }
    }

    @Suppress("TooGenericExceptionCaught")
    fun onClearLogs() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.clearAllLogs()
                val count = repository.getLogsCount()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalLogsCount = count,
                        errorMessage = null
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to clear logs"
                    )
                }
            }
        }
    }

    fun onToggleTelemetryService(context: Context) {
        val currentlyRunning = TelemetryService.isRunning(context)
        if (currentlyRunning) {
            TelemetryService.stop(context)
            _uiState.update { it.copy(isTelemetryServiceRunning = false) }
        } else {
            TelemetryService.start(context)
            _uiState.update { it.copy(isTelemetryServiceRunning = true) }
        }
    }

    suspend fun exportLogs(context: Context, destinationUri: Uri): Result<Int> {
        return logExporter.exportLogsToUri(context, destinationUri)
    }

    fun refreshServiceStatus(context: Context) {
        val running = TelemetryService.isRunning(context)
        _uiState.update { it.copy(isTelemetryServiceRunning = running) }
    }

    class Factory(
        private val repository: LogRepository,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository, ioDispatcher) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        private const val DEBOUNCE_MILLIS = 300L
    }
}
