package in.rahulja.getlogs;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.ExecutionException;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;

public class LogLocationWorker extends Worker {

    private static final String LOCATION_ACTION = "in.rahulja.getlogs.LAST_LOCATION";

    public LogLocationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
             broadcastLocation("ERROR_GETTING", "ERROR_GETTING");
             return Result.failure();
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        try {
            Location location = Tasks.await(fusedLocationClient.getLastLocation());
            if (location != null) {
                broadcastLocation(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
            } else {
                 broadcastLocation(null, null);
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            broadcastLocation("ERROR_GETTING", "ERROR_GETTING");
            return Result.failure();
        }

        return Result.success();
    }

    private void broadcastLocation(String lat, String lng) {
        Intent intent = new Intent();
        intent.setAction(LOCATION_ACTION);
        intent.putExtra("latitude", lat);
        intent.putExtra("longitude", lng);
        getApplicationContext().sendBroadcast(intent);
    }
}
