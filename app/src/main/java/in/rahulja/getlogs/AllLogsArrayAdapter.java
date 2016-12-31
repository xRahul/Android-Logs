package in.rahulja.getlogs;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by slx on 31/12/16.
 */

public class AllLogsArrayAdapter extends ArrayAdapter<String> {

    private Context context;
    private List<String> logs;

    public AllLogsArrayAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);

        this.context = context;
        this.logs = objects;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        String log = logs.get(position);

        //get the inflater and inflate the XML layout for each item
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.logs_list_view_item, null);

        TextView fullLog = (TextView) view.findViewById(R.id.list_view_item_text);

        fullLog.setText(log);

        return view;

    }
}
