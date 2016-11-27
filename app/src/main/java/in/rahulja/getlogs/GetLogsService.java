package in.rahulja.getlogs;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.text.format.DateFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class GetLogsService extends IntentService {

    private SensorManager mSensorManager;
    private SensorEventListener sensorListener;
    private List<Sensor> deviceSensors;
    private HashMap<Integer, String> deviceSensorData;
    private Sensor aSensor;
    private ArrayList<Sensor> sensorsList;

    public static final String PARAM_IN_MSG = "in.rahulja.getlogs.imsg";
    public static final String PARAM_OUT_MSG = "in.rahulja.getlogs.omsg";

    public GetLogsService() {
        super("GetLogsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        String msg = intent.getStringExtra(PARAM_IN_MSG);
        SystemClock.sleep(1000); // 1 second
        String resultTxt = msg + " "
                + DateFormat.format("MM/dd/yy h:mmaa", System.currentTimeMillis());

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        sensorsList = new ArrayList<Sensor>();
        deviceSensorData = new HashMap<Integer, String>();

        sensorListener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor arg0, int arg1) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                String asdf = "asdf";
                for (int i = 0; i < event.values.length; i++) {
                    asdf += event.values[i] + "\t";
                }
                deviceSensorData.put(event.sensor.getType(), asdf);
            }
        };


        for (int i = 0; i < deviceSensors.size(); i++) {

            sensorsList.add(i, mSensorManager.getDefaultSensor(deviceSensors.get(i).getType()));
            mSensorManager.registerListener(sensorListener, sensorsList.get(i), SensorManager.SENSOR_DELAY_NORMAL);
            deviceSensorData.put(sensorsList.get(i).getType(), "none");
        }
        SystemClock.sleep(2000);
        for (int i = 0; i < deviceSensors.size(); i++) {
            resultTxt += "\n" + deviceSensors.get(i).getName();
//            resultTxt += "\n" + deviceSensors.get(i).getVendor();
//            resultTxt += "\n" + deviceSensors.get(i).getMinDelay();
//            resultTxt += "\n" + deviceSensors.get(i).getResolution();
//            resultTxt += "\n" + deviceSensors.get(i).getType();
//            resultTxt += "\n" + deviceSensors.get(i).getMaximumRange();

            if(! (deviceSensorData.get(i) == null) ) {
                resultTxt += "\n" + deviceSensorData.get(i).toString();
            }
            resultTxt += "\n" + sensorsList.get(i).toString();

            resultTxt += "\n\n";
//            resultTxt += "\n" + deviceSensorData.toString();
        }

        String filename = "testFilenew.csv";
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_APPEND);
            resultTxt += "\n\n" + outputStream.getChannel().toString() + "\n\n";
            resultTxt += "\n\n" + getFilesDir() + "\n\n";

            File directory = new File(getFilesDir().toURI());
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                resultTxt += "\n\n" + files[i].getName() + "\n\n";
            }
            outputStream.write(resultTxt.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.ShowLogsReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
        sendBroadcast(broadcastIntent);
    }


}
