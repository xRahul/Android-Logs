package `in`.rahulja.getlogs.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import `in`.rahulja.getlogs.data.LogRepository
import `in`.rahulja.getlogs.model.LogEntity
import `in`.rahulja.getlogs.model.LogType
import `in`.rahulja.getlogs.service.TelemetryService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LogRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        context = ApplicationProvider.getApplicationContext()
        mockkObject(TelemetryService.Companion)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun initialUiStateHasCorrectDefaults() = runTest(testDispatcher) {
        val viewModel = MainViewModel(repository, testDispatcher)
        val state = viewModel.uiState.value

        assertEquals("", state.searchQuery)
        assertFalse(state.isTelemetryServiceRunning)
        assertNull(state.selectedLogType)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(0L, state.totalLogsCount)
    }

    @Test
    fun onSearchQueryChangedUpdatesState() = runTest(testDispatcher) {
        val viewModel = MainViewModel(repository, testDispatcher)

        viewModel.uiState.test {
            assertEquals("", awaitItem().searchQuery)

            viewModel.onSearchQueryChanged("BATTERY")
            assertEquals("BATTERY", awaitItem().searchQuery)

            viewModel.onSearchQueryChanged("WIFI_SCAN")
            assertEquals("WIFI_SCAN", awaitItem().searchQuery)
        }
    }

    @Test
    fun onLogTypeFilterSelectedUpdatesState() = runTest(testDispatcher) {
        val viewModel = MainViewModel(repository, testDispatcher)

        viewModel.uiState.test {
            assertNull(awaitItem().selectedLogType)

            viewModel.onLogTypeFilterSelected(LogType.LOCATION)
            assertEquals(LogType.LOCATION, awaitItem().selectedLogType)

            viewModel.onLogTypeFilterSelected(LogType.SECURITY)
            assertEquals(LogType.SECURITY, awaitItem().selectedLogType)

            viewModel.onLogTypeFilterSelected(null)
            assertNull(awaitItem().selectedLogType)
        }
    }

    @Test
    fun onClearLogsCallsRepositoryAndUpdatesState() = runTest(testDispatcher) {
        coEvery { repository.clearAllLogs() } returns Unit
        coEvery { repository.getLogsCount() } returns 0L

        val viewModel = MainViewModel(repository, testDispatcher)
        viewModel.onClearLogs()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.clearAllLogs() }
        assertEquals(0L, viewModel.uiState.value.totalLogsCount)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onClearLogsHandlesError() = runTest(testDispatcher) {
        coEvery { repository.clearAllLogs() } throws RuntimeException("DB error")

        val viewModel = MainViewModel(repository, testDispatcher)
        viewModel.onClearLogs()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.clearAllLogs() }
        assertEquals("DB error", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onToggleTelemetryServiceStartsServiceWhenStopped() = runTest(testDispatcher) {
        every { TelemetryService.isRunning(any()) } returns false
        every { TelemetryService.start(any()) } returns Unit

        val viewModel = MainViewModel(repository, testDispatcher)
        viewModel.onToggleTelemetryService(context)

        verify(exactly = 1) { TelemetryService.start(context) }
        assertTrue(viewModel.uiState.value.isTelemetryServiceRunning)
    }

    @Test
    fun onToggleTelemetryServiceStopsServiceWhenRunning() = runTest(testDispatcher) {
        every { TelemetryService.isRunning(any()) } returns true
        every { TelemetryService.stop(any()) } returns Unit

        val viewModel = MainViewModel(repository, testDispatcher)
        viewModel.onToggleTelemetryService(context)

        verify(exactly = 1) { TelemetryService.stop(context) }
        assertFalse(viewModel.uiState.value.isTelemetryServiceRunning)
    }

    @Test
    fun refreshServiceStatusUpdatesIsTelemetryServiceRunning() = runTest(testDispatcher) {
        every { TelemetryService.isRunning(any()) } returns true

        val viewModel = MainViewModel(repository, testDispatcher)
        viewModel.refreshServiceStatus(context)

        assertTrue(viewModel.uiState.value.isTelemetryServiceRunning)

        every { TelemetryService.isRunning(any()) } returns false
        viewModel.refreshServiceStatus(context)

        assertFalse(viewModel.uiState.value.isTelemetryServiceRunning)
    }

    @Test
    fun logsPagingFlowDebouncesSearchQuery() = runTest(testDispatcher) {
        val dummyPagingData = PagingData.from(
            listOf(
                LogEntity(
                    id = 1L,
                    timestamp = 1000L,
                    action = "ACTION",
                    dataPayload = "{}",
                    logType = LogType.GENERAL,
                    formattedText = "Formatted"
                )
            )
        )

        every { repository.getLogsPaging(any()) } returns flowOf(dummyPagingData)

        val viewModel = MainViewModel(repository, testDispatcher)

        viewModel.logsPagingFlow.test {
            // Initial empty query triggers debounced load
            advanceTimeBy(350)
            assertNotNull(awaitItem())

            // Type queries rapidly within debounce window
            viewModel.onSearchQueryChanged("A")
            advanceTimeBy(100)
            viewModel.onSearchQueryChanged("AB")
            advanceTimeBy(100)
            viewModel.onSearchQueryChanged("ABC")

            // Advance time past debounce window (300ms)
            advanceTimeBy(350)
            assertNotNull(awaitItem())

            coVerify { repository.getLogsPaging("ABC") }
        }
    }

    @Test
    fun factoryCreatesMainViewModelInstance() {
        val factory = MainViewModel.Factory(repository)
        val viewModel = factory.create(MainViewModel::class.java)

        assertNotNull(viewModel)
    }

    @Test
    fun factoryThrowsOnUnknownViewModelClass() {
        class UnknownViewModel : ViewModel()

        val factory = MainViewModel.Factory(repository)
        assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnknownViewModel::class.java)
        }
    }
}
