package `in`.rahulja.getlogs.util

import `in`.rahulja.getlogs.model.LogEntity
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

object LogFormatter {

    fun formatLogForDisplay(jsonString: String): String {
        if (jsonString.isBlank()) {
            return ""
        }
        return try {
            val lineObj = JSONObject(jsonString)
            val lineToWrite = StringBuilder()
            if (lineObj.has("datetime")) {
                lineToWrite.append(lineObj.get("datetime")).append("\n")
            }

            if (lineObj.has("action")) {
                var actionName = lineObj.get("action").toString()
                val actionSeparator = actionName.lastIndexOf('.')
                if (actionSeparator != -1) {
                    actionName = actionName.substring(0, actionSeparator) +
                        "\n\t" +
                        actionName.substring(actionSeparator + 1)
                }
                lineToWrite.append(actionName)
            }

            if (lineObj.has("data")) {
                val dataObj = lineObj.getJSONObject("data")
                val dataObjKeys = dataObj.keys()
                while (dataObjKeys.hasNext()) {
                    lineToWrite.append("\n\t\t")
                    val key = dataObjKeys.next()
                    val value = getDataValue(dataObj, key)
                    lineToWrite.append(key).append(": ").append(value)
                }
            }
            lineToWrite.toString()
        } catch (_: JSONException) {
            ""
        }
    }

    fun formatForClipboard(log: LogEntity): String {
        if (log.formattedText.isNotBlank()) {
            return log.formattedText
        }

        val builder = StringBuilder()
        val dateFormatted = DateFormat.getDateTimeInstance().format(Date(log.timestamp))
        builder.append(dateFormatted).append("\n")

        var actionName = log.action
        val actionSeparator = actionName.lastIndexOf('.')
        if (actionSeparator != -1) {
            actionName = actionName.substring(0, actionSeparator) +
                "\n\t" +
                actionName.substring(actionSeparator + 1)
        }
        builder.append(actionName)

        if (log.dataPayload.isNotBlank()) {
            try {
                val dataObj = JSONObject(log.dataPayload)
                val dataObjKeys = dataObj.keys()
                while (dataObjKeys.hasNext()) {
                    builder.append("\n\t\t")
                    val key = dataObjKeys.next()
                    val value = getDataValue(dataObj, key)
                    builder.append(key).append(": ").append(value)
                }
            } catch (_: JSONException) {
                builder.append("\n\t\t").append(log.dataPayload)
            }
        }

        return builder.toString()
    }

    private fun getDataValue(dataObj: JSONObject, key: String): StringBuilder {
        var value = StringBuilder(dataObj.get(key).toString())
        if (value.isNotEmpty() && value[0] == '[') {
            if (value.length > 2) {
                value.deleteCharAt(value.length - 1)
                val parts = value.substring(1).split(", ")
                value = StringBuilder()
                for (str in parts) {
                    value.append("\n\t\t\t\t").append(str)
                }
            } else {
                value = StringBuilder()
            }
        }
        if (key == "wifiInfo") {
            val parts = value.toString().split(", ")
            value = StringBuilder()
            for (str in parts) {
                value.append("\n\t\t\t\t").append(str)
            }
        }
        return value
    }
}
