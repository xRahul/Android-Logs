package in.rahulja.getlogs;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class LogLocationIntentService extends IntentService implements ConnectionCallbacks, OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    String mLatitude;
    String mLongitude;

    final static String locationAction = "in.rahulja.getlogs.LAST_LOCATION";

    public LogLocationIntentService() {
        super("LogLocationIntentService");
    }

    @Override
    public void onCreate() {

        initializeGoogleApiClient();

        super.onCreate();
    }

    private void initializeGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Intent intent = createLocationIntent();
        sendBroadcast(intent);
    }

    private Intent createLocationIntent() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED
                ) {
            mLatitude = "ERROR_GETTING";
            mLongitude = "ERROR_GETTING";
        } else {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation != null) {
                mLatitude = String.valueOf(mLastLocation.getLatitude());
                mLongitude = String.valueOf(mLastLocation.getLongitude());
            }
        }

        Intent intent = new Intent();
        intent.setAction(locationAction);
        intent.putExtra("latitude", mLatitude);
        intent.putExtra("longitude", mLongitude);

        return intent;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Called called when there was an error connecting the client to the service.
        Log.i(
            "LOG_LIFE",
                "onConnectionFailed:" +
                connectionResult.getErrorCode() +
                "," +
                connectionResult.getErrorMessage()
        );
    }
}
