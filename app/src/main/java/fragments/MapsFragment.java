package fragments;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import adapters.GroupUserAdapter;
import database_classes.GroupChat;
import database_classes.GroupUser;
import database_classes.UserLocation;
import firebase_utils.DatabaseManager;
import gis.hereim.R;
import pub.devrel.easypermissions.EasyPermissions;
import utils.CircleTransform;
import location_utils.LocationUpdateService;
import utils.SoundFxManager;

import static activities.MainActivity.GROUP_CHAT_INTENT_EXTRA_KEY;
import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;
import static android.location.LocationManager.*;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    //region Constants
    private static final String[] LOCATION_PERMISSIONS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};
    private static final float MAP_MARKERS_TRANSPARENCY = 0.7f;
    private static final int RC_PERM = 124;
    private static final String NOTIFICATION_CHANNEL_ID = "HERE_I_AM_LOCATION_VIOLATION";
    //endregion

    //region Class Members
    public static Location mCurrentLocation;
    private Context mContext;
    private GoogleMap mMap;
    private GroupChat mCurrentGroup;
    private Map<String, Marker> mMarkers = new HashMap<>();
    private RecyclerView mUsersRecyclerView;
    //endregion

    public MapsFragment() { }

    //______________________________________________________________________________________________

    public static MapsFragment newInstance(GroupChat groupChat) {
        Bundle args = new Bundle();
        args.putSerializable(GROUP_CHAT_INTENT_EXTRA_KEY, groupChat);
        MapsFragment fragment = new MapsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    //region Overrides
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    //______________________________________________________________________________________________

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_maps, container, false);

        mUsersRecyclerView = fragmentView.findViewById(R.id.map_users_recycler_view);

        if(getArguments() != null){
            mCurrentGroup = (GroupChat) getArguments().getSerializable(GROUP_CHAT_INTENT_EXTRA_KEY);
        }

        if (!EasyPermissions.hasPermissions(mContext, LOCATION_PERMISSIONS)) {
            EasyPermissions.requestPermissions(this,
                    getString(R.string.permissions),
                    RC_PERM, LOCATION_PERMISSIONS);
        } else {
            initGoogleMap();
        }

        return fragmentView;
    }

    //______________________________________________________________________________________________

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults,
                this);

        if (!EasyPermissions.hasPermissions(mContext, LOCATION_PERMISSIONS)) {
            closeFragment();
        } else {
            initGoogleMap();
        }
    }

    //______________________________________________________________________________________________

    @Override
    @SuppressLint("MissingPermission")
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        updateLastLocation();
        showGroupMembersOnMap();
        showGroupMembersOnRecyclerView();
        mContext.startService(new Intent(mContext, LocationUpdateService.class));
    }
    //endregion

    //region Methods
    @SuppressLint("MissingPermission")
    private void updateLastLocation() {

        LocationManager mLocationManager =
                (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        if (mLocationManager != null) {

            String provider;

            if(mLocationManager.isProviderEnabled(GPS_PROVIDER)) {
                provider = GPS_PROVIDER;
            }
            else if(mLocationManager.isProviderEnabled(NETWORK_PROVIDER)) {
                provider = NETWORK_PROVIDER;
            } else if(mLocationManager.isProviderEnabled(PASSIVE_PROVIDER)) {
                provider = PASSIVE_PROVIDER;
            } else {
                Toast.makeText(mContext, "Cant find location", Toast.LENGTH_SHORT).show();
                closeFragment();
                return;
            }

            Location lastLocation = mLocationManager.getLastKnownLocation(provider);

            if(lastLocation != null) {
                updateCurrentLocation(lastLocation);
                LatLng latLng = new LatLng(mCurrentLocation.getLatitude(),
                        mCurrentLocation.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12),
                        3000, null);
            }
        } else {
            Toast.makeText(mContext, R.string.no_location_msg,
                    Toast.LENGTH_SHORT).show();
            closeFragment();
        }
    }

    //______________________________________________________________________________________________

    private void closeFragment() {
        if (getActivity() != null) {
            FragmentManager supportFragmentManager;
            supportFragmentManager = getActivity().getSupportFragmentManager();
            supportFragmentManager.beginTransaction().remove(this).commit();
            supportFragmentManager.popBackStack();
            Toast.makeText(mContext, getString(R.string.permissions_denied),
                    Toast.LENGTH_SHORT).show();
        }
    }

    //______________________________________________________________________________________________

    private void initGoogleMap() {
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);

        if (supportMapFragment != null) {
            supportMapFragment.getMapAsync(MapsFragment.this);
        }
    }

    //______________________________________________________________________________________________

    private void showGroupMembersOnRecyclerView() {

        mUsersRecyclerView.setHasFixedSize(true);

        FirebaseRecyclerOptions<GroupUser> options = new FirebaseRecyclerOptions.Builder<GroupUser>()
                .setLifecycleOwner(this)
                .setQuery(sDatabaseManager.groupChatsDbRef().child(mCurrentGroup.getGroupId())
                        .child("groupUsers"), GroupUser.class).build();

        final GroupUserAdapter userAdapter = new GroupUserAdapter(mContext, options,
                mCurrentGroup, GroupUserAdapter.eViewTypes.Map_View);

        userAdapter.startListening();

        userAdapter.setUserClickListener(new GroupUserAdapter.OnUserClickListener() {
            @Override
            public void onClickGroupUser(final GroupUser user) {
                if(user.getIsSharingLocation()){
                    UserLocation location = user.getLocation();
                    LatLng latLng = new LatLng(location.getLat(), location.getLng());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18),
                            2000, null);
                }
            }
        });

        mUsersRecyclerView.addItemDecoration(new DividerItemDecoration(mUsersRecyclerView.getContext(),
                DividerItemDecoration.HORIZONTAL));

        mUsersRecyclerView.setAdapter(userAdapter);
    }

    //______________________________________________________________________________________________

    private void showGroupMembersOnMap() {

        for (final String groupUserId : mCurrentGroup.getGroupUsers().keySet()) {

            if(groupUserId.equals(sCurrentFirebaseUser.getUid())) {
                continue;
            }

            sDatabaseManager.listenToUserLocation(groupUserId,
                    new DatabaseManager.UserLocationListener() {
                @Override
                public void onLocationFetched(GroupUser user) {

                    if(mMarkers.containsKey(groupUserId)) {

                        Marker markerToUpdate = mMarkers.get(groupUserId);

                        if (user.getIsSharingLocation()) {
                            markerToUpdate.setVisible(true);
                        } else {
                            markerToUpdate.setVisible(false);
                        }

                        markerToUpdate.setPosition(user.getLocation().getLatLng());
                    } else {
                        addNewUserMarker(user, groupUserId);
                    }

                    // If i am the admin of the group BOOM BOOM CHAKALAKA
                    if(mCurrentGroup.getAdminId().equals(sCurrentFirebaseUser.getUid())) {
                        checkForDistanceViolation(user.getLocation(), groupUserId);
                    }
                }
            });
        }
    }

    //______________________________________________________________________________________________

    private void checkForDistanceViolation(UserLocation groupUserLocation, String groupUserId) {

        float[] results = new float[1];
        Location.distanceBetween(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(),
                groupUserLocation.getLat(), groupUserLocation.getLng(), results);

        long resultInMeters = (long)results[0] / 1000;

        if(resultInMeters > mCurrentGroup.getAllowedDistanceFromAdmin()) {

            Log.e("DISTANCE_VIOLATION", "Distance found: " + results[0]);

            Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

            if (vibrator != null) {
                vibrator.vibrate(100);
            }

            if(PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext())
                    .getBoolean("pref_notification_sound", true)) {
                SoundFxManager.PlaySoundFx(SoundFxManager.eSoundEffect.DISTANCE_ALERT);
            }

            HashMap<String, String> users =
                    (HashMap<String, String>)mCurrentGroup.getGroupUsers().get(groupUserId);

            String userName = users.get("fullName");

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_app_logo_color_primary)
                    .setContentTitle(mCurrentGroup.getGroupName())
                    .setContentText(userName + " Is Out Of Range!!!")
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel();
            }

            notificationManager.notify(0, builder.build());
        }
    }

    //______________________________________________________________________________________________

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel =
                    new NotificationChannel(NOTIFICATION_CHANNEL_ID,"Distance Alert" , importance);
            channel.setDescription("When a user steps out of the defined boundary in map");

            NotificationManager notificationManager =
                    mContext.getSystemService(NotificationManager.class);

            if(notificationManager != null){
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    //______________________________________________________________________________________________

    private void addNewUserMarker(final GroupUser groupUser, final String groupUserId) {

        sDatabaseManager.fetchUser(groupUserId, new DatabaseManager.FetchUserCallback() {
            @Override
            public void onUserFetched(final GroupUser user) {
                final MarkerOptions options = new MarkerOptions()
                        .alpha(MAP_MARKERS_TRANSPARENCY)
                        .position(groupUser.getLocation().getLatLng())
                        .title(user.getFullName());

                final ImageView userImg = new ImageView(mContext);

                Picasso.get().load(user.getPhotoUri())
                        .resize(100, 100)
                        .transform(new CircleTransform())
                        .networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.drawable.img_blank_profile).into(userImg, new Callback() {
                    @Override
                    public void onSuccess() {
                        Bitmap bitmap = ((BitmapDrawable)userImg.getDrawable()).getBitmap();
                        options.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
                        Marker marker = mMap.addMarker(options);

                        if(!user.getIsSharingLocation()) {
                            marker.setVisible(false);
                        }

                        mMarkers.put(groupUserId, marker);
                    }

                    @Override
                    public void onError(Exception e) {
                        mMarkers.put(groupUserId, mMap.addMarker(options));
                    }
                });
            }
        });
    }

    //______________________________________________________________________________________________

    private void updateCurrentLocation(Location location) {

        mCurrentLocation = location;

        Geocoder geocoder = new Geocoder(mContext.getApplicationContext());

        try {
            Address address = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1).get(0);

            sCurrentFirebaseUser.updateMyLocation(location, address.getAddressLine(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //endregion
}
