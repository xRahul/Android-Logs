@file:Suppress("TooManyFunctions")

package `in`.rahulja.getlogs.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import `in`.rahulja.getlogs.model.LogEntity
import `in`.rahulja.getlogs.model.LogType
import `in`.rahulja.getlogs.ui.MainUiState
import `in`.rahulja.getlogs.ui.MainViewModel
import `in`.rahulja.getlogs.util.PermissionHelper
import kotlinx.coroutines.launch

@Composable
fun LogListScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val pagingItems = viewModel.logsPagingFlow.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberPermissionLauncher(snackbarHostState) { showPermissionDialog = false }
    val exportLauncher = rememberExportLauncher(viewModel, snackbarHostState)

    LaunchedEffect(Unit) {
        if (!PermissionHelper.hasAllRequiredPermissions(context)) {
            showPermissionDialog = true
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg -> snackbarHostState.showSnackbar(msg) }
    }

    LogListDialogs(
        showClearDialog = showClearDialog,
        showPermissionDialog = showPermissionDialog,
        onConfirmClear = {
            showClearDialog = false
            viewModel.onClearLogs()
            coroutineScope.launch { snackbarHostState.showSnackbar("All logs cleared") }
        },
        onDismissClear = { showClearDialog = false },
        onGrantPermissions = { permissionLauncher.launch(PermissionHelper.getRequiredPermissions()) },
        onDismissPermissions = { showPermissionDialog = false }
    )

    val actions = LogListActions(
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onLogTypeSelected = viewModel::onLogTypeFilterSelected,
        onToggleService = {
            if (!uiState.isTelemetryServiceRunning && !PermissionHelper.hasAllRequiredPermissions(context)) {
                showPermissionDialog = true
            } else {
                viewModel.onToggleTelemetryService(context)
            }
        },
        onLogCopied = { coroutineScope.launch { snackbarHostState.showSnackbar("Log copied to clipboard") } },
        onExportClicked = { exportLauncher.launch("android_logs_${System.currentTimeMillis()}.txt") },
        onClearClicked = { showClearDialog = true }
    )

    LogListScaffoldContent(
        uiState = uiState,
        pagingItems = pagingItems,
        snackbarHostState = snackbarHostState,
        actions = actions,
        modifier = modifier
    )
}

@Composable
private fun rememberPermissionLauncher(
    snackbarHostState: SnackbarHostState,
    onResult: () -> Unit
): ActivityResultLauncher<Array<String>> {
    val coroutineScope = rememberCoroutineScope()
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        onResult()
        val allGranted = permissionsMap.values.all { it }
        val message = if (allGranted) "Permissions granted" else "Some permissions were denied."
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }
}

@Composable
private fun rememberExportLauncher(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState
): ActivityResultLauncher<String> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { destinationUri ->
            coroutineScope.launch {
                val result = viewModel.exportLogs(context, destinationUri)
                val msg = result.fold(
                    onSuccess = { "Successfully exported $it logs" },
                    onFailure = { "Export failed: ${it.localizedMessage ?: "Unknown error"}" }
                )
                snackbarHostState.showSnackbar(msg)
            }
        }
    }
}

@Composable
private fun LogListScaffoldContent(
    uiState: MainUiState,
    pagingItems: LazyPagingItems<LogEntity>,
    snackbarHostState: SnackbarHostState,
    actions: LogListActions,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LogListTopAppBar(
                onExportClicked = actions.onExportClicked,
                onClearClicked = actions.onClearClicked
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LogListContentColumn(
            uiState = uiState,
            pagingItems = pagingItems,
            actions = actions,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun LogListDialogs(
    showClearDialog: Boolean,
    showPermissionDialog: Boolean,
    onConfirmClear: () -> Unit,
    onDismissClear: () -> Unit,
    onGrantPermissions: () -> Unit,
    onDismissPermissions: () -> Unit
) {
    if (showClearDialog) {
        ClearLogsConfirmationDialog(
            onConfirm = onConfirmClear,
            onDismiss = onDismissClear
        )
    }

    if (showPermissionDialog) {
        PermissionRequestDialog(
            onGrantPermissions = onGrantPermissions,
            onDismiss = onDismissPermissions
        )
    }
}

data class LogListActions(
    val onSearchQueryChanged: (String) -> Unit,
    val onLogTypeSelected: (LogType?) -> Unit,
    val onToggleService: () -> Unit,
    val onLogCopied: (LogEntity) -> Unit,
    val onExportClicked: () -> Unit,
    val onClearClicked: () -> Unit
)

@Composable
fun LogListContentColumn(
    uiState: MainUiState,
    pagingItems: LazyPagingItems<LogEntity>,
    actions: LogListActions,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchAndFilterSection(
            searchQuery = uiState.searchQuery,
            onSearchQueryChanged = actions.onSearchQueryChanged,
            selectedLogType = uiState.selectedLogType,
            onLogTypeSelected = actions.onLogTypeSelected,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ServiceControlCard(
            isRunning = uiState.isTelemetryServiceRunning,
            onToggleService = actions.onToggleService,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        LogsListContent(
            pagingItems = pagingItems,
            uiState = uiState,
            onLogCopied = actions.onLogCopied
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogListTopAppBar(
    onExportClicked: () -> Unit,
    onClearClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = "Android Logs",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        actions = {
            IconButton(onClick = onExportClicked) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export logs"
                )
            }
            IconButton(onClick = onClearClicked) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear all logs"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedLogType: LogType?,
    onLogTypeSelected: (LogType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search logs (e.g. WiFi, Battery)...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        FilterChipsRow(
            selectedType = selectedLogType,
            onTypeSelected = onLogTypeSelected
        )
    }
}

@Composable
fun FilterChipsRow(
    selectedType: LogType?,
    onTypeSelected: (LogType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors()
        )

        LogType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = {
                    onTypeSelected(if (selectedType == type) null else type)
                },
                label = {
                    Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}

@Composable
fun LogsListContent(
    pagingItems: LazyPagingItems<LogEntity>,
    uiState: MainUiState,
    onLogCopied: (LogEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    when {
        pagingItems.loadState.refresh is LoadState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        pagingItems.itemCount == 0 -> {
            EmptyLogState(
                searchQuery = uiState.searchQuery,
                selectedLogType = uiState.selectedLogType,
                modifier = modifier
            )
        }
        else -> {
            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.id }
                ) { index ->
                    val log = pagingItems[index]
                    if (log != null) {
                        LogItemCard(
                            log = log,
                            onLogCopied = onLogCopied
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLogState(
    searchQuery: String,
    selectedLogType: LogType?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (searchQuery.isNotBlank() || selectedLogType != null) {
                    "No Matching Logs Found"
                } else {
                    "No Logs Recorded Yet"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (searchQuery.isNotBlank() || selectedLogType != null) {
                    "Try changing your search terms or filter selection."
                } else {
                    "Enable the telemetry service or perform device actions to generate event logs."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ClearLogsConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Clear All Logs?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("This will permanently delete all stored event logs from the database. This action cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Clear",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
