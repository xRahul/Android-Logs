package in.rahulja.getlogs;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;

public class WriteLogIntentService extends IntentService {

  private static final String LOG_FOLDER = "AllLogs";

  public WriteLogIntentService() {
    super("WriteLogIntentService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (intent.getExtras() == null) {
      return;
    }
    String fileName = intent.getExtras().getString("filename");
    String data = intent.getExtras().getString("data");

    try {
      File folder = new File(Environment.getExternalStorageDirectory() +
          File.separator + LOG_FOLDER);
      //noinspection ResultOfMethodCallIgnored
      folder.mkdirs();

      File myFile = new File(Environment.getExternalStorageDirectory() +
          File.separator + LOG_FOLDER + File.separator + fileName);
      //noinspection ResultOfMethodCallIgnored
      myFile.createNewFile();
      try (OutputStreamWriter myOutWriter = new OutputStreamWriter(
          new FileOutputStream(myFile, true))) {
        myOutWriter.append(data);
        myOutWriter.append("\n");
      }
    } catch (IOException e) {
      Log.e("Android-Logs", Arrays.toString(e.getStackTrace()));
    }
  }
}
