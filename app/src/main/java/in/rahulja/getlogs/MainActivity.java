package in.rahulja.getlogs;

import android.app.SearchManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

  final static String LOG_FOLDER = "AllLogs";
  final static String ALL_LOGS_FILE = "allLogs.txt";

  public AllLogsArrayAdapter itemsAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    showLogs();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.options_menu, menu);

    // Associate searchable configuration with the SearchView
    SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
    SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    searchView.setSubmitButtonEnabled(true);
    searchView.setOnQueryTextListener(this);

    return true;
  }

  public void viewLogs(View view) {
    showLogs();
  }

  private void showLogs() {
    setLogsInListView(getLogsArray());
  }

  private void setLogsInListView(ArrayList<String> logArray) {
    itemsAdapter = new AllLogsArrayAdapter(this, logArray);
    RecyclerView listView = (RecyclerView) findViewById(R.id.list_view_logs);
    listView.setLayoutManager(new LinearLayoutManager(this));
    listView.setHasFixedSize(true);
    listView.setAdapter(itemsAdapter);
  }

  private ArrayList<String> getLogsArray() {

    ArrayList<String> logArray = new ArrayList<>();
    File file = new File(Environment.getExternalStorageDirectory() +
        File.separator + LOG_FOLDER + File.separator + ALL_LOGS_FILE);

    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;

      while ((line = br.readLine()) != null) {
        try {
          JSONObject lineObj = new JSONObject(line);
          String toWrite = lineObj.get("datetime").toString();
          toWrite += "\n";
          String actionName = lineObj.get("action").toString();
          int actionSeparator = actionName.lastIndexOf('.');
          actionName = actionName.substring(0, actionSeparator)
              + "\n\t"
              + actionName.substring(actionSeparator + 1);
          toWrite += actionName;
          if (lineObj.has("data")) {
            JSONObject dataObj = lineObj.getJSONObject("data");
            Iterator<String> dataObjKeys = dataObj.keys();
            while (dataObjKeys.hasNext()) {
              toWrite += "\n\t\t";
              String key = dataObjKeys.next();
              String value = dataObj.get(key).toString();
              if (value.charAt(0) == '[') {
                value = value.substring(1, value.length() - 1);
                String[] parts = value.split(", ");
                value = "";
                for (String str : parts) {
                  value += "\n\t\t\t\t" + str;
                }
              }
              if (key.equals("wifiInfo")) {
                String[] parts = value.split(", ");
                value = "";
                for (String str : parts) {
                  value += "\n\t\t\t\t" + str;
                }
              }
              toWrite += key + ": " + value;
            }
          }
          logArray.add(0, toWrite);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
      br.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return logArray;
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    return false;
  }

  @Override
  public boolean onQueryTextChange(String newText) {
    //        itemsAdapter.getFilter().filter(newText);
    //        return true;
    return false;
  }
}
