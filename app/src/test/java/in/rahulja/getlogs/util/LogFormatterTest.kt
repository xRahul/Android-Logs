package `in`.rahulja.getlogs.util

import `in`.rahulja.getlogs.model.LogEntity
import `in`.rahulja.getlogs.model.LogType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LogFormatterTest {

    @Test
    fun formatLogForDisplay_ValidJson() {
        val json =
            """
            {
                "datetime": "2023-10-27 10:00:00",
                "action": "in.rahulja.getlogs.ACTION_TEST",
                "data": {
                    "key1": "value1",
                    "key2": "[item1, item2]"
                }
            }
            """.trimIndent()

        val result = LogFormatter.formatLogForDisplay(json)

        assertTrue(result.contains("2023-10-27 10:00:00"))
        assertTrue(result.contains("in.rahulja.getlogs"))
        assertTrue(result.contains("ACTION_TEST"))
        assertTrue(result.contains("key1: value1"))
        assertTrue(result.contains("key2: "))
        assertTrue(result.contains("item1"))
        assertTrue(result.contains("item2"))
    }

    @Test
    fun formatLogForDisplay_InvalidJson() {
        val json = "{invalid_json}"
        val result = LogFormatter.formatLogForDisplay(json)
        assertEquals("", result)
    }

    @Test
    fun formatLogForDisplay_EmptyOrBlankInput() {
        assertEquals("", LogFormatter.formatLogForDisplay(""))
        assertEquals("", LogFormatter.formatLogForDisplay("   "))
    }

    @Test
    fun formatLogForDisplay_MissingFields() {
        val json = """{"datetime": "2023-10-27"}"""
        val result = LogFormatter.formatLogForDisplay(json)
        assertEquals("2023-10-27\n", result)
    }

    @Test
    fun formatLogForDisplay_ActionWithoutDot() {
        val json = """{"action": "CUSTOM_ACTION"}"""
        val result = LogFormatter.formatLogForDisplay(json)
        assertEquals("CUSTOM_ACTION", result)
    }

    @Test
    fun formatLogForDisplay_WifiInfo() {
        val json =
            """
            {
                "datetime": "2023-10-27",
                "action": "test.action",
                "data": {
                    "wifiInfo": "ssid, bssid, signal"
                }
            }
            """.trimIndent()

        val result = LogFormatter.formatLogForDisplay(json)
        assertTrue(result.contains("ssid"))
        assertTrue(result.contains("bssid"))
        assertTrue(result.contains("signal"))
        assertTrue(result.contains("\n\t\t\t\tssid"))
    }

    @Test
    fun formatLogForDisplay_EmptyArray() {
        val json =
            """
            {
                "action": "test.action",
                "data": {
                    "emptyList": "[]"
                }
            }
            """.trimIndent()

        val result = LogFormatter.formatLogForDisplay(json)
        assertTrue(result.contains("emptyList: "))
    }

    @Test
    fun formatForClipboard_WithFormattedText() {
        val log = LogEntity(
            id = 1,
            timestamp = 1700000000000L,
            action = "android.intent.action.BATTERY_LOW",
            dataPayload = """{"level":15}""",
            logType = LogType.GENERAL,
            formattedText = "2023-11-14 22:13:20\nandroid.intent.action\n\tBATTERY_LOW\n\t\tlevel: 15"
        )

        val clipboardText = LogFormatter.formatForClipboard(log)
        assertEquals(log.formattedText, clipboardText)
    }

    @Test
    fun formatForClipboard_WithoutFormattedText() {
        val log = LogEntity(
            id = 2,
            timestamp = 1700000000000L,
            action = "android.intent.action.BATTERY_LOW",
            dataPayload = """{"level":15}""",
            logType = LogType.GENERAL,
            formattedText = ""
        )

        val clipboardText = LogFormatter.formatForClipboard(log)
        assertTrue(clipboardText.contains("android.intent.action"))
        assertTrue(clipboardText.contains("BATTERY_LOW"))
        assertTrue(clipboardText.contains("level: 15"))
    }
}
