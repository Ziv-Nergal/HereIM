package firebase_utils;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import database_classes.GroupUser;
import utils.ColorParser;

import static firebase_utils.DatabaseManager.NOTIFICATIONS_DB_REF_NAME;

public class FirebaseUser {

    private static FirebaseUser sInstance = null;

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private com.google.firebase.auth.FirebaseUser mFirebaseUser;

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
    private Uri mUserPhotoUri;
    private String mUserPhotoUrl;
    private boolean mIsOnline;

    private FirebaseUser() {
        initUser();
    }

    public static FirebaseUser getInstance() {
        if (sInstance == null) {
            sInstance = new FirebaseUser();
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

            mUserPhotoUri = mFirebaseUser.getPhotoUrl();
            downloadUserPhoto();

            mFullName = mFirebaseUser.getDisplayName();
            mEmailAddress = mFirebaseUser.getEmail();
            mDeviceToken = FirebaseInstanceId.getInstance().getToken();
            mCurrentUserDbRef.child("deviceToken").setValue(mDeviceToken);
            mUserColor = ColorParser.Pars(mUid);
            mCurrentUserDbRef.child("online").onDisconnect().setValue(false);

            retrieveUserStatus();
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

    private void downloadUserPhoto() {
        mCurrentUserDbRef.child("photoUri").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUserPhotoUrl = dataSnapshot.getValue(String.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void updatePhoto(Uri iUserPhotoUri) {
        mUserPhotoUri = iUserPhotoUri;
        mFirebaseUser.updateProfile(new UserProfileChangeRequest.Builder().setPhotoUri(iUserPhotoUri).build());
        uploadUserPhoto(iUserPhotoUri);
    }

    private void uploadUserPhoto(Uri iUserPhotoUri) {

        final StorageReference imageStorageRef = FirebaseStorage
                .getInstance()
                .getReference()
                .child("Profile_Images")
                .child(mFirebaseUser.getUid() + ".jpg");

        UploadTask uploadTask = imageStorageRef.putFile(iUserPhotoUri);

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

    String getUserPhotoUrl() {
        return mUserPhotoUrl;
    }

    public Uri getUserPhotoUri() {
        return mUserPhotoUri;
    }

    public String getUid() {
        return mUid;
    }

    public String getFullName() {
        return mFullName;
    }

    public void setFullName(String fullName) {
        this.mFullName = fullName;
        mCurrentUserDbRef.child("fullName").setValue(fullName);
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

    public String getUserStatus() {
        return mUserStatus;
    }

    public void setUserStatus(String userStatus) {
        this.mUserStatus = userStatus;
        mCurrentUserDbRef.child("status").setValue(userStatus);
    }

    private void retrieveUserStatus() {
        mCurrentUserDbRef.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUserStatus = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    GroupUser getFirebaseClassInstance(){

        GroupUser me = new GroupUser();

        me.setUid(mUid);
        me.setDeviceToken(mDeviceToken);
        me.setEmail(mEmailAddress);
        me.setFullName(mFullName);
        me.setOnline(mIsOnline);
        me.setPhotoUri(mUserPhotoUrl);
        me.setStatus(mUserStatus);

        return me;
    }

    public boolean isOnline() {
        return mIsOnline;
    }

    public void setIsOnline(boolean iIsOnline) {
        mIsOnline = iIsOnline;
        mCurrentUserDbRef.child("online").setValue(iIsOnline);
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
