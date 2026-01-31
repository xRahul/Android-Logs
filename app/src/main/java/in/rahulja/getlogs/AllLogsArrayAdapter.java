package in.rahulja.getlogs;

import android.app.Activity;
import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Filter;
import android.widget.Filterable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

class AllLogsArrayAdapter extends RecyclerView.Adapter<AllLogsHolder> implements Filterable {

  private Context context;
  private List<String> logs;
  private List<String> logsFiltered;
  private CharSequence currentQuery = "";

  AllLogsArrayAdapter(Context context, List<String> objects) {
    this.context = context;
    this.logs = objects;
    this.logsFiltered = objects;
  }

  public void addLogs(List<String> newLogs) {
    this.logs.addAll(newLogs);
    if (currentQuery == null || currentQuery.length() == 0) {
      if (this.logsFiltered != this.logs) {
        this.logsFiltered = this.logs;
      }
      notifyDataSetChanged();
    } else {
      getFilter().filter(currentQuery);
    }
  }

  @Override
  public AllLogsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater =
        (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

    View view = null;
    if (inflater != null) {
      view = inflater.inflate(R.layout.logs_list_view_item, parent, false);
    }

    return new AllLogsHolder(this.context, view);
  }

  @Override
  public void onBindViewHolder(AllLogsHolder holder, int position) {
    String log = this.logsFiltered.get(position);
    holder.bindLog(log);
  }

  @Override
  public int getItemCount() {
    return this.logsFiltered.size();
  }

  @Override
  public Filter getFilter() {
    return new Filter() {
      @Override
      protected FilterResults performFiltering(CharSequence charSequence) {
        String charString = charSequence.toString();
        if (charString.isEmpty()) {
          logsFiltered = logs;
        } else {
          List<String> filteredList = new ArrayList<>();
          for (String row : logs) {
            if (containsIgnoreCase(row, charString)) {
              filteredList.add(row);
            }
          }
          logsFiltered = filteredList;
        }

        FilterResults filterResults = new FilterResults();
        filterResults.values = logsFiltered;
        return filterResults;
      }

      @Override
      protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
        currentQuery = charSequence;
        logsFiltered = (List<String>) filterResults.values;
        notifyDataSetChanged();
      }
    };
  }

  private static boolean containsIgnoreCase(String src, String what) {
    final int length = what.length();
    if (length == 0) {
      return true;
    }

    final char firstLo = Character.toLowerCase(what.charAt(0));
    final char firstUp = Character.toUpperCase(what.charAt(0));

    for (int i = src.length() - length; i >= 0; i--) {
      // Quick check before calling regionMatches
      final char ch = src.charAt(i);
      if (ch != firstLo && ch != firstUp) {
        continue;
      }

      if (src.regionMatches(true, i, what, 0, length)) {
        return true;
      }
    }

    return false;
  }
}
