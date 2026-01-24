package in.rahulja.getlogs;

import android.app.SearchManager;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

  private static final String LOG_FOLDER = "AllLogs";
  private static final String ALL_LOGS_FILE = "allLogs.txt";

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
    if (searchManager != null) {
      SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
      searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
      searchView.setSubmitButtonEnabled(true);
      searchView.setOnQueryTextListener(this);
    }
    return true;
  }

  public void viewLogs(View view) {
    showLogs();
  }

  private void showLogs() {
    setLogsInListView(getLogsArray());
  }

  private void setLogsInListView(ArrayList<String> logArray) {
    AllLogsArrayAdapter itemsAdapter = new AllLogsArrayAdapter(this, logArray);
    RecyclerView listView = (RecyclerView) findViewById(R.id.list_view_logs);
    if (listView != null) {
      listView.setLayoutManager(new LinearLayoutManager(this));
      listView.setHasFixedSize(true);
      listView.setAdapter(itemsAdapter);
    }
  }

  private ArrayList<String> getLogsArray() {

    ArrayList<String> logArray = new ArrayList<>();
    File file = new File(getExternalFilesDir(null), LOG_FOLDER + File.separator + ALL_LOGS_FILE);

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = br.readLine()) != null) {
        logArray.add(0, getLogLineForArray(line));
      }
    } catch (IOException e) {
      Log.e("Android-Logs", Arrays.toString(e.getStackTrace()));
    }

    return logArray;
  }

  private String getLogLineForArray(String line) {
    StringBuilder lineToWrite = new StringBuilder();
    try {
      JSONObject lineObj = new JSONObject(line);
      lineToWrite.append(lineObj.get("datetime")).append("\n");
      String actionName = lineObj.get("action").toString();
      int actionSeparator = actionName.lastIndexOf('.');
      actionName = actionName.substring(0, actionSeparator)
          + "\n\t"
          + actionName.substring(actionSeparator + 1);
      lineToWrite.append(actionName);
      if (lineObj.has("data")) {
        JSONObject dataObj = lineObj.getJSONObject("data");
        Iterator<String> dataObjKeys = dataObj.keys();
        while (dataObjKeys.hasNext()) {
          lineToWrite.append("\n\t\t");
          String key = dataObjKeys.next();
          StringBuilder value = getDataValue(dataObj, key);
          lineToWrite.append(key).append(": ").append(value);
        }
      }
      return lineToWrite.toString();
    } catch (JSONException e) {
      Log.e("Android-Logs", Arrays.toString(e.getStackTrace()));
      return "";
    }
  }

  @NonNull private StringBuilder getDataValue(JSONObject dataObj, String key) throws JSONException {
    StringBuilder value = new StringBuilder(dataObj.get(key).toString());
    if (value.charAt(0) == '[') {
      value = new StringBuilder(value.substring(1, value.length() - 1));
      String[] parts = value.toString().split(", ");
      value = new StringBuilder();
      for (String str : parts) {
        value.append("\n\t\t\t\t").append(str);
      }
    }
    if (key.equals("wifiInfo")) {
      String[] parts = value.toString().split(", ");
      value = new StringBuilder();
      for (String str : parts) {
        value.append("\n\t\t\t\t").append(str);
      }
    }
    return value;
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    return false;
  }

  @Override
  public boolean onQueryTextChange(String newText) {
    //        itemsAdapter.getFilter().filter(newText);
    // return true;
    return false;
  }
}
