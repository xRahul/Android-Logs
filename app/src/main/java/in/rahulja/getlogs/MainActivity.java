package in.rahulja.getlogs;

import android.app.SearchManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

  private static final String LOG_FOLDER = "AllLogs";
  private static final String ALL_LOGS_FILE = "allLogs.txt";

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private AllLogsArrayAdapter itemsAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    showLogs();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    executor.shutdownNow();
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
    loadLogs();
  }

  private void setLogsInListView(ArrayList<String> logArray) {
    itemsAdapter = new AllLogsArrayAdapter(this, logArray);
    RecyclerView listView = (RecyclerView) findViewById(R.id.list_view_logs);
    TextView emptyView = (TextView) findViewById(R.id.empty_view);

    if (listView != null && emptyView != null) {
      if (logArray.isEmpty()) {
        listView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
      } else {
        listView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setHasFixedSize(true);
        listView.setAdapter(itemsAdapter);
      }
    }
  }

  private void loadLogs() {
    executor.execute(() -> {
      ArrayList<String> logArray = new ArrayList<>();
      File file = new File(getExternalFilesDir(null), LOG_FOLDER + File.separator + ALL_LOGS_FILE);

      try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        String line;
        while ((line = br.readLine()) != null && !Thread.currentThread().isInterrupted()) {
          logArray.add(LogParser.getLogLineForArray(line));
        }
        if (!Thread.currentThread().isInterrupted()) {
          Collections.reverse(logArray);
        }
      } catch (IOException e) {
        Log.e("Android-Logs", Arrays.toString(e.getStackTrace()));
      }

      if (!Thread.currentThread().isInterrupted()) {
        runOnUiThread(() -> {
          if (!isFinishing() && !isDestroyed()) {
            setLogsInListView(logArray);
          }
        });
      }
    });
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    return false;
  }

  @Override
  public boolean onQueryTextChange(String newText) {
    if (itemsAdapter != null) {
      itemsAdapter.getFilter().filter(newText);
    }
    return true;
  }
}
