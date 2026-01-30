package in.rahulja.getlogs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;

public class WriteLogWorker extends Worker {

  private static final String LOG_FOLDER = "AllLogs";

  public WriteLogWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
  }

  @NonNull
  @Override
  public Result doWork() {
    String fileName = getInputData().getString("filename");
    String data = getInputData().getString("data");

    if (fileName == null || data == null) {
      return Result.failure();
    }

    try {
      File folder = new File(getApplicationContext().getExternalFilesDir(null), LOG_FOLDER);
      File myFile = new File(folder, fileName);

      try {
        try (OutputStreamWriter myOutWriter = new OutputStreamWriter(
            new FileOutputStream(myFile, true))) {
          myOutWriter.append(data);
          myOutWriter.append("\n");
        }
      } catch (FileNotFoundException e) {
        if (!folder.exists() && folder.mkdirs()) {
          try (OutputStreamWriter myOutWriter = new OutputStreamWriter(
              new FileOutputStream(myFile, true))) {
            myOutWriter.append(data);
            myOutWriter.append("\n");
          }
        } else {
          throw e;
        }
      }
    } catch (IOException e) {
      Log.e("Android-Logs", Arrays.toString(e.getStackTrace()));
      return Result.failure();
    }

    return Result.success();
  }
}
