package in.rahulja.getlogs;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class AllReceivers extends DeviceAdminReceiver {

  final static String ALL_ACTIONS_FILE = "allActions.csv";
  final static String ALL_LOGS_FILE = "allLogs.txt";
  final static String LOCATION_FILE = "location.csv";
  final static String PASSWORD_FILE = "passwordAttempts.csv";
  final static String DEVICE_USED_FILE = "deviceUsed.csv";
  final static String WIFI_FILE = "wifi.csv";

  private Intent intent;
  private Context context;

  public AllReceivers() {
  }

  @Override
  public void onReceive(Context contextReceived, Intent receivedIntent) {
    context = contextReceived;
    intent = receivedIntent;

    logAllActions();
    logActionSeparately();
  }

  private void logAllActions() {

    JSONObject logData = new JSONObject();

    try {
      logData.put("action", intent.getAction());
      logData.put(
          "datetime",
          DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime())
      );

      Bundle intent_extras = intent.getExtras();
      if (intent_extras != null) {
        JSONObject logExtraData = new JSONObject();
        for (String key : intent_extras.keySet()) {
          Object value = intent_extras.get(key);
          if (value != null) {
            logExtraData.put(key, value.toString());
          }
        }
        logData.put("data", logExtraData);
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    /* Log Actions only */
    writeLogToFile(
        ALL_ACTIONS_FILE,
        DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) +
            ", " +
            intent.getAction()
    );

    writeLogToFile(ALL_LOGS_FILE, logData.toString());
  }

  private void logActionSeparately() {

    switch (intent.getAction()) {
      case "android.intent.action.USER_PRESENT":
        handleUserPresentAction();
        break;
      case "android.intent.action.CLOSE_SYSTEM_DIALOGS":
        handleCloseSystemDialogs();
        break;
      case "android.app.action.ACTION_PASSWORD_SUCCEEDED":
        handleActionPasswordSucceeded();
        break;
      case "android.app.action.ACTION_PASSWORD_FAILED":
        handleActionPasswordFailed();
        break;
      case "in.rahulja.getlogs.LAST_LOCATION":
        handleLastLocation();
        break;
      case "android.net.wifi.SCAN_RESULTS":
        handleWifiScanResults();
        break;
    }
  }

  private void handleWifiScanResults() {
    if ((boolean) intent.getExtras().get("resultsUpdated")) {
      WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
      List<ScanResult> results = wifiManager.getScanResults();
      String log = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) +
          ", " +
          results.toString();
      writeLogToFile(WIFI_FILE, log);
    }
  }

  private void handleLastLocation() {
    String log = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) +
        ", " +
        intent.getExtras().getString("latitude") +
        ", " +
        intent.getExtras().getString("longitude");
    writeLogToFile(LOCATION_FILE, log);
  }

  private void handleActionPasswordFailed() {
    String log = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) +
        ", FAILED";
    writeLogToFile(PASSWORD_FILE, log);
  }

  private void handleActionPasswordSucceeded() {
    String log = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) +
        ", SUCCEEDED";
    writeLogToFile(PASSWORD_FILE, log);
  }

  private void handleCloseSystemDialogs() {

    Intent mServiceIntent = new Intent(context, LogLocationIntentService.class);
    context.startService(mServiceIntent);

    String log = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) +
        ", LOCKED";
    writeLogToFile(DEVICE_USED_FILE, log);
  }

  private void handleUserPresentAction() {
    String log = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) +
        ", UNLOCKED";
    writeLogToFile(DEVICE_USED_FILE, log);
  }

  private void writeLogToFile(String fileName, String data) {
    Intent mServiceIntent = new Intent(context, WriteLogIntentService.class);
    mServiceIntent.putExtra("filename", fileName);
    mServiceIntent.putExtra("data", data);
    context.startService(mServiceIntent);
  }
}
