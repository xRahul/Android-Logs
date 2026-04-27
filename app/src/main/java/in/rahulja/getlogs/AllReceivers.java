package in.rahulja.getlogs;

import android.annotation.SuppressLint;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;

public class AllReceivers extends DeviceAdminReceiver {

  private static final String ALL_ACTIONS_FILE = "allActions.csv";
  private static final String ALL_LOGS_FILE = "allLogs.txt";
  private static final String LOCATION_FILE = "location.csv";
  private static final String PASS_WORD_FILE = "passwordAttempts.csv";
  private static final String DEVICE_USED_FILE = "deviceUsed.csv";
  private static final String WIFI_FILE = "wifi.csv";
  private static final String LOG_FOLDER = "AllLogs";

  private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

  private static final ThreadLocal<DateFormat> DATE_FORMATTER = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return DateFormat.getDateTimeInstance();
    }
  };

  private Intent intent;
  private Context context;

  public AllReceivers() {
    // to override
  }

  @SuppressLint("UnsafeProtectedBroadcastReceiver")
  @Override
  public void onReceive(Context contextReceived, Intent receivedIntent) {
    final PendingResult pendingResult = goAsync();
    context = contextReceived;
    intent = receivedIntent;

    try {
      logAllActions();
      logActionSeparately();
    } finally {
      if (pendingResult != null) {
        EXECUTOR.execute(pendingResult::finish);
      }
    }
  }

  private void logAllActions() {

    JSONObject logData = new JSONObject();

    try {
      logData.put("action", intent.getAction());
      logData.put(
          "datetime",
          DATE_FORMATTER.get().format(new Date())
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
        DATE_FORMATTER.get().format(new Date()) +
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
        if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
          return;
        }
        List<ScanResult> results = wifiManager.getScanResults();
        String log = DATE_FORMATTER.get().format(new Date()) +
            ", " +
            results.toString();
        writeLogToFile(WIFI_FILE, log);
      }
    }
  }

  private void handleLastLocation() {
    if (intent.getExtras() != null) {
      String log = DATE_FORMATTER.get().format(new Date()) +
          ", " +
          intent.getExtras().getString("latitude") +
          ", " +
          intent.getExtras().getString("longitude");
      writeLogToFile(LOCATION_FILE, log);
    }
  }

  private void handleActionPasswordFailed() {
    String log = DATE_FORMATTER.get().format(new Date()) +
        ", FAILED";
    writeLogToFile(PASS_WORD_FILE, log);
  }

  private void handleActionPasswordSucceeded() {
    String log = DATE_FORMATTER.get().format(new Date()) +
        ", SUCCEEDED";
    writeLogToFile(PASS_WORD_FILE, log);
  }

  private void handleCloseSystemDialogs() {

    OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(LogLocationWorker.class)
        .build();
    WorkManager.getInstance(context).enqueue(request);

    String log = DATE_FORMATTER.get().format(new Date()) +
        ", LOCKED";
    writeLogToFile(DEVICE_USED_FILE, log);
  }

  private void handleUserPresentAction() {
    String log = DATE_FORMATTER.get().format(new Date()) +
        ", UNLOCKED";
    writeLogToFile(DEVICE_USED_FILE, log);
  }

  private void writeLogToFile(String fileName, String data) {
    final Context appContext = context.getApplicationContext();
    EXECUTOR.execute(() -> {
      if (fileName == null || data == null) {
        return;
      }

      try {
        File folder = new File(appContext.getExternalFilesDir(null), LOG_FOLDER);
        if (!folder.exists()) {
          folder.mkdirs();
        }

        File myFile = new File(folder, fileName);
        if (!myFile.exists()) {
          myFile.createNewFile();
        }

        try (OutputStreamWriter myOutWriter = new OutputStreamWriter(
            new FileOutputStream(myFile, true))) {
          myOutWriter.append(data);
          myOutWriter.append("\n");
        }
      } catch (IOException e) {
        Log.e("Android-Logs", Arrays.toString(e.getStackTrace()));
      }
    });
  }
}
