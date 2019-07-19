package utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

import static activities.MainActivity.sCurrentFirebaseUser;

public class LocationListener implements android.location.LocationListener {

    //region Constants
    private static final String TAG = "LOCATION_LISTENER";
    //endregion

    //region Class members
    private String mProvider;
    private Location mLastLocation;
    private Context mContext;
    //endregion

    LocationListener(String provider, Context context) {
        Log.e(TAG, "LocationListener " + provider);
        mProvider = provider;
        mLastLocation = new Location(provider);
        mContext = context;
    }

    String getProvider() {
        return mProvider;
    }

    //region Overrides
    @Override
    public void onLocationChanged(Location location) {

        mLastLocation.set(location);

        Geocoder geocoder = new Geocoder(mContext);

        try {
            Address address = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1).get(0);

            sCurrentFirebaseUser.updateMyLocation(location, address.getAddressLine(0));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e(TAG, "onLocationChanged: " + location);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e(TAG, "onProviderDisabled: " + provider);
    }
    @Override
    public void onProviderEnabled(String provider) {
        Log.e(TAG, "onProviderEnabled: " + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.e(TAG, "onStatusChanged: " + provider);
    }
    //endregion
}