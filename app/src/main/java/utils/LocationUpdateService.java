package utils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;

public class LocationUpdateService extends Service {

    //region Constants
    private static final String TAG = "LOCATION_UPDATE_SERVICE";
    private static final int LOCATION_INTERVAL = 2500;
    private static final float LOCATION_DISTANCE = 10f;
    //endregion

    //region Class Members
    private LocationManager mLocationManager = null;
    private LocationListener[] mLocationListeners;
    //endregion

    //region Overrides
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {

        Log.e(TAG, "onCreate");
        mLocationListeners = new LocationListener[] {
                new LocationListener(LocationManager.NETWORK_PROVIDER, getApplicationContext()),
                new LocationListener(LocationManager.GPS_PROVIDER, getApplicationContext())
        };

        initializeLocationManager();

        for (LocationListener listener : mLocationListeners) {
            requestLocationUpdates(listener.getProvider(), listener);
        }
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (LocationListener mLocationListener : mLocationListeners) {
                try {
                    mLocationManager.removeUpdates(mLocationListener);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }
    //endregion

    //region Methods
    private void requestLocationUpdates(String provider, LocationListener listener) {
        try {
            mLocationManager.requestLocationUpdates(
                    provider, LOCATION_INTERVAL, LOCATION_DISTANCE,listener);
        } catch (SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, provider + "not available" + ex.getMessage());
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext()
                    .getSystemService(Context.LOCATION_SERVICE);
        }
    }
    //endregion
}
