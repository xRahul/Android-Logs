package in.rahulja.getlogs;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;


class AllLogsArrayAdapter extends RecyclerView.Adapter<AllLogsHolder> {

    private Context context;
    private List<String> logs;

    AllLogsArrayAdapter(Context context, List<String> objects) {
//        super(context, resource, objects);

        this.context = context;
        this.logs = objects;
    }

    // 2. Override the onCreateViewHolder method
    @Override
    public AllLogsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 3. Inflate the view and return the new ViewHolder
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.logs_list_view_item, parent, false);

        return new AllLogsHolder(this.context, view);
    }

    // 4. Override the onBindViewHolder method
    @Override
    public void onBindViewHolder(AllLogsHolder holder, int position) {

        // 5. Use position to access the correct Bakery object
        String log = this.logs.get(position);

        // 6. Bind the bakery object to the holder
        holder.bindLog(log);
    }

    @Override
    public int getItemCount() {
        return this.logs.size();
    }
}
