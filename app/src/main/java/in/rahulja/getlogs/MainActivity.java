package in.rahulja.getlogs;

import android.app.SearchManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    public ArrayAdapter<String> itemsAdapter;

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
        setLogsInListView( getLogsArray() );
    }

    private void setLogsInListView(ArrayList<String> logArray) {
        itemsAdapter = new ArrayAdapter<String>(this, R.layout.logs_list_view_item, R.id.list_view_item_text, logArray);
        ListView listView = (ListView) findViewById(R.id.list_view_logs);
        if (listView != null) {
            listView.setAdapter(itemsAdapter);
        }
    }

    private ArrayList<String> getLogsArray() {

        ArrayList<String> logArray = new ArrayList<String>();
        File file = new File(Environment.getExternalStorageDirectory(), "allLogs.txt");

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                try {
                    JSONObject lineObj = new JSONObject(line);
                    String toWrite = lineObj.get("datetime").toString();
                    toWrite += "\n";
                    String actionName = lineObj.get("action").toString();
                    int actionSeperator = actionName.lastIndexOf('.');
                    actionName = actionName.substring(0, actionSeperator)
                                    + "\n\t"
                                    + actionName.substring(actionSeperator+1);
                    toWrite += actionName;
                    if (lineObj.has("data")) {
                        JSONObject dataObj = lineObj.getJSONObject("data");
                        Iterator<String> dataObjKeys = dataObj.keys();
                        while (dataObjKeys.hasNext()) {
                            toWrite += "\n\t\t";
                            String key = dataObjKeys.next();
                            String value = dataObj.get(key).toString();
                            if (value.charAt(0) == '[') {
                                value = value.substring(1, value.length()-1);
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
        itemsAdapter.getFilter().filter(newText);
        return true;
    }
}
