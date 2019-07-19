package fragments;

import android.annotation.SuppressLint;
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
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
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
import gis.hereim.R;
import pub.devrel.easypermissions.EasyPermissions;
import utils.CircleTransform;
import utils.LocationUpdateService;

import static activities.MainActivity.GROUP_CHAT_INTENT_EXTRA_KEY;
import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;
import static android.location.LocationManager.*;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    //region Constants
    private static final String[] LOCATION_PERMISSIONS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};

    private static final int RC_PERM = 124;
    //endregion

    //region Class Members
    private Context mContext;
    private GoogleMap mMap;
    private Location mCurrentLocation;
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
                UserLocation location = user.getLocation();
                LatLng latLng = new LatLng(location.getLat(), location.getLng());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18),
                        2000, null);
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

            sDatabaseManager.usersDbRef().child(groupUserId)
                    .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if(dataSnapshot.child("location").exists()) {

                        double lat = (double)dataSnapshot.child("location").child("lat").getValue();
                        double lng = (double)dataSnapshot.child("location").child("lng").getValue();
                        LatLng latLng = new LatLng(lat, lng);

                        if(mMarkers.containsKey(groupUserId)) {

                            Marker markerToUpdate = mMarkers.get(groupUserId);

                            if(markerToUpdate != null) {
                                markerToUpdate.setPosition(latLng);
                            }
                        } else {
                            addNewUserMarker(dataSnapshot, latLng, groupUserId);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            });
        }
    }

    //______________________________________________________________________________________________

    private void addNewUserMarker(@NonNull DataSnapshot dataSnapshot,
                                  LatLng latLng, final String groupUserId) {

        GroupUser user = dataSnapshot.getValue(GroupUser.class);

        if(user == null) {
            return;
        }

        final MarkerOptions options = new MarkerOptions().alpha(0.7f).position(latLng)
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
                mMarkers.put(groupUserId, mMap.addMarker(options));
            }

            @Override
            public void onError(Exception e) {
                mMarkers.put(groupUserId, mMap.addMarker(options));
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
