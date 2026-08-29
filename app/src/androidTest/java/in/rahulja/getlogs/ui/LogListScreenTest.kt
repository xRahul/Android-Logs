package `in`.rahulja.getlogs.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import `in`.rahulja.getlogs.model.LogEntity
import `in`.rahulja.getlogs.model.LogType
import `in`.rahulja.getlogs.ui.components.EmptyLogState
import `in`.rahulja.getlogs.ui.components.LogItemCard
import `in`.rahulja.getlogs.ui.components.ServiceControlCard
import `in`.rahulja.getlogs.ui.theme.AndroidLogsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LogListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLogItemCardDisplaysActionAndCopiesOnClick() {
        val testLog = LogEntity(
            id = 1L,
            timestamp = System.currentTimeMillis(),
            action = "android.intent.action.BATTERY_LOW",
            dataPayload = "{\"level\": 15}",
            logType = LogType.GENERAL,
            formattedText = "Formatted text for battery"
        )

        var copiedLog: LogEntity? = null

        composeTestRule.setContent {
            AndroidLogsTheme {
                LogItemCard(
                    log = testLog,
                    onLogCopied = { copiedLog = it }
                )
            }
        }

        composeTestRule.onNodeWithText("android.intent.action.BATTERY_LOW").assertIsDisplayed()
        composeTestRule.onNodeWithText("GENERAL").assertIsDisplayed()
        composeTestRule.onNodeWithText("{\"level\": 15}").assertIsDisplayed()

        composeTestRule.onNodeWithText("android.intent.action.BATTERY_LOW").performClick()
        assertEquals(testLog, copiedLog)
    }

    @Test
    fun testServiceControlCardActiveState() {
        var toggleClicked = false

        composeTestRule.setContent {
            AndroidLogsTheme {
                ServiceControlCard(
                    isRunning = true,
                    onToggleService = { toggleClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Service Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monitoring system events in background").assertIsDisplayed()
    }

    @Test
    fun testServiceControlCardInactiveStateAndToggle() {
        var toggleClicked = false

        composeTestRule.setContent {
            AndroidLogsTheme {
                ServiceControlCard(
                    isRunning = false,
                    onToggleService = { toggleClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Service Inactive").assertIsDisplayed()
        composeTestRule.onNodeWithText("Background monitoring is stopped").assertIsDisplayed()
    }

    @Test
    fun testEmptyLogStateDisplaysCorrectText() {
        composeTestRule.setContent {
            AndroidLogsTheme {
                EmptyLogState(
                    searchQuery = "",
                    selectedLogType = null
                )
            }
        }

        composeTestRule.onNodeWithText("No Logs Recorded Yet").assertIsDisplayed()
    }
}
