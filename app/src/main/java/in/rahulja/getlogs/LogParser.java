package in.rahulja.getlogs;

import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

public class LogParser {

  public static String getLogLineForArray(String line) {
    StringBuilder lineToWrite = new StringBuilder();
    try {
      JSONObject lineObj = new JSONObject(line);
      if (lineObj.has("datetime")) {
        lineToWrite.append(lineObj.get("datetime")).append("\n");
      }

      if (lineObj.has("action")) {
        String actionName = lineObj.get("action").toString();
        int actionSeparator = actionName.lastIndexOf('.');
        if (actionSeparator != -1) {
          actionName = actionName.substring(0, actionSeparator)
              + "\n\t"
              + actionName.substring(actionSeparator + 1);
        }
        lineToWrite.append(actionName);
      }

      if (lineObj.has("data")) {
        JSONObject dataObj = lineObj.getJSONObject("data");
        Iterator<String> dataObjKeys = dataObj.keys();
        while (dataObjKeys.hasNext()) {
          lineToWrite.append("\n\t\t");
          String key = dataObjKeys.next();
          StringBuilder value = getDataValue(dataObj, key);
          lineToWrite.append(key).append(": ").append(value);
        }
      }
      return lineToWrite.toString();
    } catch (JSONException e) {
      return "";
    }
  }

  private static StringBuilder getDataValue(JSONObject dataObj, String key) throws JSONException {
    StringBuilder value = new StringBuilder(dataObj.get(key).toString());
    if (value.length() > 0 && value.charAt(0) == '[') {
      if (value.length() > 2) {
        value = new StringBuilder(value.substring(1, value.length() - 1));
        String[] parts = value.toString().split(", ");
        value = new StringBuilder();
        for (String str : parts) {
          value.append("\n\t\t\t\t").append(str);
        }
      } else {
        // empty array []
        value = new StringBuilder();
      }
    }
    if (key.equals("wifiInfo")) {
      String[] parts = value.toString().split(", ");
      value = new StringBuilder();
      for (String str : parts) {
        value.append("\n\t\t\t\t").append(str);
      }
    }
    return value;
  }
}
