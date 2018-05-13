package in.rahulja.getlogs;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class WriteLogIntentService extends IntentService {

  final static String LOG_FOLDER = "AllLogs";

  public WriteLogIntentService() {
    super("WriteLogIntentService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
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
      FileOutputStream fOut = new FileOutputStream(myFile, true);
      OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
      myOutWriter.append(data);
      myOutWriter.append("\n");
      myOutWriter.close();
      fOut.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
