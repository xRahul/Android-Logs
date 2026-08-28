package `in`.rahulja.getlogs.security

import `in`.rahulja.getlogs.data.PiiSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PiiSanitizerTest {

    @Test
    fun sanitizeRedactsPasswordAndPinKeys() {
        val rawJson = """{"password":"mySecretPassword","pin":"1234","action":"TEST"}"""
        val sanitized = PiiSanitizer.sanitizeJson(rawJson)
        assertTrue(sanitized.contains("\"password\":\"[REDACTED]\""))
        assertTrue(sanitized.contains("\"pin\":\"[REDACTED]\""))
        assertTrue(sanitized.contains("\"action\":\"TEST\""))
        assertFalse(sanitized.contains("mySecretPassword"))
        assertFalse(sanitized.contains("1234"))
    }

    @Test
    fun sanitizeRedactsTokenSecretAuthCredential() {
        val rawJson =
            """
            {
                "token": "tok_12345",
                "secret": "super_secret",
                "auth": "bearer xyz",
                "credential": "cred_user_pass",
                "authToken": "tok_67890",
                "api_secret": "sec_456",
                "credentials": "cred_list",
                "authorization": "basic dXNlcjpwYXNz"
            }
            """.trimIndent()
        val sanitized = PiiSanitizer.sanitizeJson(rawJson)
        assertFalse(sanitized.contains("tok_12345"))
        assertFalse(sanitized.contains("super_secret"))
        assertFalse(sanitized.contains("bearer xyz"))
        assertFalse(sanitized.contains("cred_user_pass"))
        assertFalse(sanitized.contains("tok_67890"))
        assertFalse(sanitized.contains("sec_456"))
        assertFalse(sanitized.contains("cred_list"))
        assertFalse(sanitized.contains("basic dXNlcjpwYXNz"))
        assertTrue(sanitized.contains("\"token\":\"[REDACTED]\""))
        assertTrue(sanitized.contains("\"secret\":\"[REDACTED]\""))
        assertTrue(sanitized.contains("\"auth\":\"[REDACTED]\""))
        assertTrue(sanitized.contains("\"credential\":\"[REDACTED]\""))
    }

    @Test
    fun sanitizeCaseInsensitiveMatching() {
        val rawJson = """{"PASSWORD":"sec","Pin":"4321","AUTH_TOKEN":"tok","SECRET_KEY":"key"}"""
        val sanitized = PiiSanitizer.sanitizeJson(rawJson)
        assertTrue(sanitized.contains("\"PASSWORD\":\"[REDACTED]\""))
        assertTrue(sanitized.contains("\"Pin\":\"[REDACTED]\""))
        assertTrue(sanitized.contains("\"AUTH_TOKEN\":\"[REDACTED]\""))
        assertTrue(sanitized.contains("\"SECRET_KEY\":\"[REDACTED]\""))
        assertFalse(sanitized.contains("sec"))
        assertFalse(sanitized.contains("4321"))
        assertFalse(sanitized.contains("tok"))
        assertFalse(sanitized.contains("key"))
    }

    @Test
    fun sanitizePreservesNonSensitiveKeys() {
        val rawJson = """{"action":"android.intent.action.BATTERY_LOW","level":15,"status":"charging"}"""
        val sanitized = PiiSanitizer.sanitizeJson(rawJson)
        assertTrue(sanitized.contains("\"action\":\"android.intent.action.BATTERY_LOW\""))
        assertTrue(sanitized.contains("\"level\":15"))
        assertTrue(sanitized.contains("\"status\":\"charging\""))
    }

    @Test
    fun sanitizeHandlesNestedObjects() {
        val rawJson =
            """
            {
                "user": {
                    "name": "Alice",
                    "password": "alicePassword",
                    "session": {
                        "token": "sess_123"
                    }
                }
            }
            """.trimIndent()
        val sanitized = PiiSanitizer.sanitizeJson(rawJson)
        assertTrue(sanitized.contains("\"name\":\"Alice\""))
        assertTrue(sanitized.contains("\"password\":\"[REDACTED]\""))
        assertTrue(sanitized.contains("\"token\":\"[REDACTED]\""))
        assertFalse(sanitized.contains("alicePassword"))
        assertFalse(sanitized.contains("sess_123"))
    }

    @Test
    fun sanitizeHandlesJsonArrays() {
        val rawJson =
            """
            [
                {"pin": "1111", "name": "Device 1"},
                {"pin": "2222", "name": "Device 2"}
            ]
            """.trimIndent()
        val sanitized = PiiSanitizer.sanitizeJson(rawJson)
        assertFalse(sanitized.contains("1111"))
        assertFalse(sanitized.contains("2222"))
        assertTrue(sanitized.contains("\"pin\":\"[REDACTED]\""))
        assertTrue(sanitized.contains("\"name\":\"Device 1\""))
    }

    @Test
    fun sanitizeHandlesInvalidOrEmptyJson() {
        assertEquals("", PiiSanitizer.sanitizeJson(""))
        assertEquals("   ", PiiSanitizer.sanitizeJson("   "))
        assertEquals("{not valid json}", PiiSanitizer.sanitizeJson("{not valid json}"))
    }
}
