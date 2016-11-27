package in.rahulja.getlogs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter(ShowLogsReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        ShowLogsReceiver receiver = new ShowLogsReceiver();
        registerReceiver(receiver, filter);
    }

    public void startLogService(View view) {
        EditText input = (EditText) findViewById(R.id.text_input_for_testing);
        String strInputMsg = input.getText().toString();
        Intent msgIntent = new Intent(this, GetLogsService.class);
        msgIntent.putExtra(GetLogsService.PARAM_IN_MSG, strInputMsg);
        startService(msgIntent);
    }

    public void stopLogService(View view) {
    }

    public class ShowLogsReceiver extends BroadcastReceiver {

        public static final String ACTION_RESP =
                "in.rahulja.getlogs.intent.action.MESSAGE_PROCESSED";

        @Override
        public void onReceive(Context context, Intent intent) {
            TextView result = (TextView) findViewById(R.id.logs_text_view);
            String text = intent.getStringExtra(GetLogsService.PARAM_OUT_MSG);
            result.setText(text + "\n");
        }
    }
}
