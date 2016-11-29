package in.rahulja.getlogs;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startLogService(View view) {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "intentLogs.txt");

            //Read text from file
            StringBuilder text = new StringBuilder();

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
            br.close();

            TextView tv = (TextView)findViewById(R.id.logs_text_view);
            tv.setText(text.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
