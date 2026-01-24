package in.rahulja.getlogs;

import android.annotation.SuppressLint;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class AllReceivers extends DeviceAdminReceiver {

  private static final String ALL_ACTIONS_FILE = "allActions.csv";
  private static final String ALL_LOGS_FILE = "allLogs.txt";
  private static final String LOCATION_FILE = "location.csv";
  private static final String PASS_WORD_FILE = "passwordAttempts.csv";
  private static final String DEVICE_USED_FILE = "deviceUsed.csv";
  private static final String WIFI_FILE = "wifi.csv";

  private Intent intent;
  private Context context;

  public AllReceivers() {
    // to override
  }

  @SuppressLint("UnsafeProtectedBroadcastReceiver")
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

      Bundle intentExtras = intent.getExtras();
      if (intentExtras != null) {
        JSONObject logExtraData = new JSONObject();
        for (String key : intentExtras.keySet()) {
          Object value = intentExtras.get(key);
          if (value != null) {
            logExtraData.put(key, value.toString());
          }
        }
        logData.put("data", logExtraData);
      }
    } catch (JSONException e) {
      Log.e("Android-Logs", Arrays.toString(e.getStackTrace()));
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
    if (intent.getAction() == null) {
      return;
    }
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
      default:
    }
  }

  private void handleWifiScanResults() {
    if (intent.getExtras() != null && (boolean) intent.getExtras().get("resultsUpdated")) {
      WifiManager wifiManager = (WifiManager) context.getApplicationContext()
          .getSystemService(Context.WIFI_SERVICE);
      if (wifiManager != null) {
        List<ScanResult> results = wifiManager.getScanResults();
        String log = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) +
            ", " +
            results.toString();
        writeLogToFile(WIFI_FILE, log);
      }
    }
  }

  private void handleLastLocation() {
    if (intent.getExtras() != null) {
      String log = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) +
          ", " +
          intent.getExtras().getString("latitude") +
          ", " +
          intent.getExtras().getString("longitude");
      writeLogToFile(LOCATION_FILE, log);
    }
  }

  private void handleActionPasswordFailed() {
    String log = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) +
        ", FAILED";
    writeLogToFile(PASS_WORD_FILE, log);
  }

  private void handleActionPasswordSucceeded() {
    String log = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) +
        ", SUCCEEDED";
    writeLogToFile(PASS_WORD_FILE, log);
  }

  private void handleCloseSystemDialogs() {

    OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(LogLocationWorker.class)
        .build();
    WorkManager.getInstance(context).enqueue(request);

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
    Data inputData = new Data.Builder()
        .putString("filename", fileName)
        .putString("data", data)
        .build();

    OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WriteLogWorker.class)
        .setInputData(inputData)
        .build();

    WorkManager.getInstance(context)
        .beginUniqueWork("write_logs", ExistingWorkPolicy.APPEND, request)
        .enqueue();
  }
}
