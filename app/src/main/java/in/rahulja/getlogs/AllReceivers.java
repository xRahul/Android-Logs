package in.rahulja.getlogs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;

public class AllReceivers extends BroadcastReceiver {
    public AllReceivers() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String intent_string = "\n\n" + java.text.DateFormat.getDateTimeInstance()
                                                        .format(Calendar.getInstance().getTime());
        intent_string += "\nAction: " + intent.getAction();
        Bundle intent_extras = intent.getExtras();
        String extras = "";
        if (intent_extras != null) {
            for (String key : intent_extras.keySet()) {
                Object value = intent_extras.get(key);
                extras += "\t" + key + ":\t" + value.toString();
            }
        }
        intent_string += "\nExtras: " + extras;

        try {
            File myFile = new File(Environment.getExternalStorageDirectory(), "intentLogs.txt");
            myFile.createNewFile();
            FileOutputStream fOut = null;
            fOut = new FileOutputStream(myFile, true);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(intent_string);
            myOutWriter.close();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
