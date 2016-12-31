package in.rahulja.getlogs;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


class AllLogsHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView logTextView;

    private String log;
    private Context context;

    AllLogsHolder(Context contextTemp, View itemView) {
        super(itemView);

        // 1. Set the context
        context = contextTemp;

        // 2. Set up the UI widgets of the holder
        logTextView = (TextView) itemView.findViewById(R.id.list_view_item_text);

        // 3. Set the "onClick" listener of the holder
        itemView.setOnClickListener(this);
    }

    void bindLog(String logTemp) {
        log = logTemp;
        // 4. Bind the data to the ViewHolder
        this.logTextView.setText(log);
    }

    @Override
    public void onClick(View v) {
        // 5. Handle the onClick event for the ViewHolder
        if (this.log != null) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("log", this.log);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this.context, "Copied Log- \n" + this.log, Toast.LENGTH_SHORT ).show();
        }
    }
}
