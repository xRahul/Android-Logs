package `in`.rahulja.getlogs.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object PiiSanitizer {

    private const val REDACTED_VALUE = "[REDACTED]"

    private val SENSITIVE_KEYWORDS = listOf(
        "password",
        "pin",
        "token",
        "secret",
        "auth",
        "credential"
    )

    fun sanitizeJson(jsonString: String): String {
        if (jsonString.isBlank()) return jsonString
        val trimmed = jsonString.trim()
        return try {
            when {
                trimmed.startsWith("{") -> {
                    val jsonObject = JSONObject(jsonString)
                    sanitizeJsonObject(jsonObject).toString()
                }
                trimmed.startsWith("[") -> {
                    val jsonArray = JSONArray(jsonString)
                    sanitizeJsonArray(jsonArray).toString()
                }
                else -> jsonString
            }
        } catch (_: JSONException) {
            jsonString
        }
    }

    private fun isSensitiveKey(key: String): Boolean {
        val lowerKey = key.lowercase()
        return SENSITIVE_KEYWORDS.any { lowerKey.contains(it) }
    }

    private fun sanitizeJsonObject(obj: JSONObject): JSONObject {
        val sanitized = JSONObject()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.get(key)
            if (isSensitiveKey(key)) {
                sanitized.put(key, REDACTED_VALUE)
            } else {
                sanitized.put(key, sanitizeValue(value))
            }
        }
        return sanitized
    }

    private fun sanitizeJsonArray(arr: JSONArray): JSONArray {
        val sanitized = JSONArray()
        for (i in 0 until arr.length()) {
            val value = arr.get(i)
            sanitized.put(sanitizeValue(value))
        }
        return sanitized
    }

    private fun sanitizeValue(value: Any?): Any? {
        return when (value) {
            is JSONObject -> sanitizeJsonObject(value)
            is JSONArray -> sanitizeJsonArray(value)
            else -> value
        }
    }
}
