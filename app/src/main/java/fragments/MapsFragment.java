package fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.Writer;

import activities.GroupChatActivity;
import gis.hereim.R;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private static final int RC_PERM = 124;

    private Context mContext;

    private GoogleMap mMap;

    private Location mCurrentLocation;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    private Handler mDelayHandler = new Handler();

    private String[] mPermissions = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};

    public MapsFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_maps, container, false);

        if (!EasyPermissions.hasPermissions(mContext, mPermissions)) {
            EasyPermissions.requestPermissions(this, getString(R.string.permissions), RC_PERM, mPermissions);
        } else {
            initGoogleMap();
        }

        return fragmentView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);

        if (!EasyPermissions.hasPermissions(mContext, mPermissions)) {
            if (getActivity() != null) {
                FragmentManager supportFragmentManager;
                supportFragmentManager = getActivity().getSupportFragmentManager();
                supportFragmentManager.beginTransaction().remove(this).commit();
                supportFragmentManager.popBackStack();
                Toast.makeText(mContext, getString(R.string.permissions_denied), Toast.LENGTH_SHORT).show();
            }
        } else {
            initGoogleMap();
        }
    }

    private void initGoogleMap() {
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (supportMapFragment != null) {
            supportMapFragment.getMapAsync(MapsFragment.this);
        }
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext);
        mMap.setMyLocationEnabled(true);

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {

                Task<Location> task = mFusedLocationProviderClient.getLastLocation();

                task.addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            mCurrentLocation = location;
                            LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12), 3000, null);
                        } else {
                            Toast.makeText(mContext, "Can׳t find your location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}
