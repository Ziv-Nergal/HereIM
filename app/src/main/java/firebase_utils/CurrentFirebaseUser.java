package firebase_utils;

import android.content.Context;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import database_classes.GroupUser;
import utils.ColorParser;

import static firebase_utils.DatabaseManager.NOTIFICATIONS_DB_REF_NAME;

public class CurrentFirebaseUser {

    private static CurrentFirebaseUser sInstance = null;

    private FirebaseUser mFirebaseUser;

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private DatabaseReference mCurrentUserDbRef;
    private DatabaseReference mCurrentUserGroupsDbRef;
    private DatabaseReference mMessageNotificationsDbRef;
    private DatabaseReference mGroupRequestsDbRef;

    private String mUid;
    private String mFullName;
    private String mEmailAddress;
    private String mDeviceToken;
    private String mUserColor;
    private String mUserStatus;
    private String mUserPhotoUrl;

    private boolean mIsOnline;

    private CurrentFirebaseUser() {
        initUser();
    }

    public static CurrentFirebaseUser getInstance() {
        if (sInstance == null) {
            sInstance = new CurrentFirebaseUser();
        } else {
            sInstance.initUser();
        }

        return sInstance;
    }

    private void initUser() {

        if(mAuth.getCurrentUser() != null){

            mFirebaseUser = mAuth.getCurrentUser();

            mUid = mFirebaseUser.getUid();

            mCurrentUserDbRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mUid);
            mCurrentUserGroupsDbRef = mCurrentUserDbRef.child("groups");
            mMessageNotificationsDbRef = mCurrentUserDbRef.child(NOTIFICATIONS_DB_REF_NAME).child("messages");
            mGroupRequestsDbRef = mCurrentUserDbRef.child(NOTIFICATIONS_DB_REF_NAME).child("groupRequests");

            mFullName = mFirebaseUser.getDisplayName();
            mEmailAddress = mFirebaseUser.getEmail();
            mUserColor = ColorParser.Pars(mUid);
            mCurrentUserDbRef.child("online").onDisconnect().setValue(false);

            fetchUserPhoto();
            fetchUserStatus();
            updateDeviceToken();
        }
    }

    DatabaseReference currentUserDbRef() {
        return mCurrentUserDbRef;
    }

    public DatabaseReference currentUserGroupsDbRef() {
        return mCurrentUserGroupsDbRef;
    }

    public DatabaseReference messageNotificationsDbRef() {
        return mMessageNotificationsDbRef;
    }

    public DatabaseReference groupRequestNotificationsDbRef() {
        return mGroupRequestsDbRef;
    }

    public String getUserPhotoUrl() {
        return mUserPhotoUrl;
    }

    public String getUid() {
        return mUid;
    }

    public String getFullName() {
        return mFullName;
    }

    public String getEmailAddress() {
        return mEmailAddress;
    }

    String getUserColor() {
        return mUserColor;
    }

    String getDeviceToken() {
        return mDeviceToken;
    }

    public String getUserStatus() { return mUserStatus; }

    GroupUser getFirebaseClassInstance(){
        return new GroupUser(mUid, mDeviceToken, mEmailAddress, mFullName,
                mIsOnline, mUserPhotoUrl, mUserStatus);
    }

    public void setFullName(String fullName) {
        this.mFullName = fullName;
        mCurrentUserDbRef.child("fullName").setValue(fullName);
        mFirebaseUser.updateProfile(new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName).build());
    }

    public void setUserStatus(String userStatus) {
        mUserStatus = userStatus;
        mCurrentUserDbRef.child("status").setValue(userStatus);
    }

    void setDeviceToken(String mDeviceToken) {
        this.mDeviceToken = mDeviceToken;
        mCurrentUserDbRef.child("deviceToken").setValue(mDeviceToken);
    }

    public void setIsOnline(boolean iIsOnline) {
        mIsOnline = iIsOnline;
        mCurrentUserDbRef.child("online").setValue(iIsOnline);
    }

    private void fetchUserPhoto() {
        mCurrentUserDbRef.child("photoUri").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUserPhotoUrl = dataSnapshot.getValue(String.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void fetchUserStatus() {
        mCurrentUserDbRef.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUserStatus = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    public void updatePhoto(Uri userPhotoUri) {
        mFirebaseUser.updateProfile(new UserProfileChangeRequest.Builder().setPhotoUri(userPhotoUri).build());

        final StorageReference imageStorageRef = FirebaseStorage
                .getInstance()
                .getReference()
                .child("Profile_Images")
                .child(mFirebaseUser.getUid() + ".jpg");

        UploadTask uploadTask = imageStorageRef.putFile(userPhotoUri);

        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {
                imageStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        mCurrentUserDbRef.child("photoUri").setValue(uri.toString());
                        mUserPhotoUrl = uri.toString();
                    }
                });
            }
        });
    }

    public void updateMyLocation(Location location, String addressLine) {
        mCurrentUserDbRef.child("location").child("lat").setValue(location.getLatitude());
        mCurrentUserDbRef.child("location").child("lng").setValue(location.getLongitude());
        mCurrentUserDbRef.child("location").child("address").setValue(addressLine);
    }

    private void updateDeviceToken() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                    @Override
                    public void onSuccess(InstanceIdResult instanceIdResult) {
                        mDeviceToken = instanceIdResult.getToken();
                        mCurrentUserDbRef.child("deviceToken").setValue(mDeviceToken);
                    }
                });
    }

    public boolean isLoggedIn(){
        return mFirebaseUser != null;
    }

    public void logout(){
        setIsOnline(false);
        sInstance = null;
        mAuth.signOut();
    }
}
