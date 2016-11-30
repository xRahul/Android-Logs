package in.rahulja.getlogs;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;

public class AllReceivers extends DeviceAdminReceiver {
    public AllReceivers() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        logAllActions(intent);

    }

    private void logAllActions(Intent intent) {
        JSONObject logData = new JSONObject();

        try {
            logData.put("datetime", java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()));
            logData.put("action", intent.getAction());

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


        writeLogToFile("allLogs.txt", logData.toString());

    }

    private void writeLogToFile(String fileName, String data) {
        try {
            File myFile = new File(Environment.getExternalStorageDirectory(), fileName);
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
