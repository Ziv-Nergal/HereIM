package firebase_utils;

import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import database_classes.GroupChat;
import database_classes.GroupUser;

import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class DatabaseManager {

    //region Constants
    private static final Uri DEFAULT_GROUP_PHOTO_URI =
            Uri.parse("android.resource://gis.hereim/drawable/img_blank_group_chat");

    private static final String APP_USERS_DB_REF_NAME = "Users";
    private static final String GROUP_CHATS_DB_REF_NAME = "Group Chats";
    private static final String MESSAGES_DB_REF_NAME = "Messages";

    static final String NOTIFICATIONS_DB_REF_NAME = "Notifications";
    //endregion

    //region Class Members
    private static DatabaseManager sInstance = null;

    private DatabaseReference mUsersDbRef;
    private DatabaseReference mGroupChatsDbRef;
    private DatabaseReference mMessagesDbRef;

    private static OnGroupNamesChangeListener mGroupNamesChangeListener;
    private static OnTypingStatusChangeListener mTypingStatusChangeListener;
    private static GroupRequestStateListener mGroupRequestStateListener;

    private static Map<String, ValueEventListener> mMessageValueEventListenerMap = new HashMap<>();

    private static ValueEventListener sGroupUserNamesValueListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            String groupUserNames = "";
            ArrayList<String> names = new ArrayList<>();

            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                names.add(snapshot.child("fullName").getValue(String.class));
            }

            if(names.size() > 0){
                int i = 0;

                for (; i < names.size() - 1 ; i++) {
                    groupUserNames = groupUserNames.concat(names.get(i) + ", ");
                }

                groupUserNames = groupUserNames.concat(names.get(i));

                if(mGroupNamesChangeListener != null) {
                    mGroupNamesChangeListener.onGroupUserNamesChange(groupUserNames);
                }
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {}
    };
    private static ValueEventListener sTypingValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

            String typingUserName = dataSnapshot.getValue(String.class);

            if(typingUserName != null && !typingUserName.equals(sCurrentFirebaseUser.getFullName()) && !typingUserName.equals("nobody")){
                mTypingStatusChangeListener.onSomeoneTyping(typingUserName);
            } else {
                mTypingStatusChangeListener.onNobodyIsTyping();
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {}
    };
    private static ValueEventListener sGroupRequestsValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

            boolean hasRequests = false;

            if(dataSnapshot.hasChild(NOTIFICATIONS_DB_REF_NAME)){
                hasRequests = true;
            }

            mGroupRequestStateListener.onGroupRequestStateChanged(hasRequests);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) { }
    };
    //endregion

    //region Listeners
    public interface MessageNotificationsListener {
        void onGotNotification(int numOfNotifications);
    }

    public interface OnGroupNamesChangeListener {
        void onGroupUserNamesChange(String names);
    }

    public interface OnTypingStatusChangeListener {
        void onSomeoneTyping(String name);
        void onNobodyIsTyping();
    }

    public interface GroupRequestStateListener {
        void onGroupRequestStateChanged(boolean haveGroupRequests);
    }
    //endregion

    //region Callbacks
    public interface GroupCreatedCallback {
        void onGroupCreated(String groupId);
    }

    public interface GroupPhotoUploadedCallback {
        void onPhotoUploaded();
    }

    public interface FetchGroupPhotoCallback {
        void onPhotoUrlFetched(String photoUrl);
    }
    public interface FetchGroupChatCallback {
        void onGroupChatFetched(GroupChat groupChat);
    }

    public interface FetchGroupUsersPhotosCallback {
        void onPhotosFetched(Map<String, String> usersPhotosUrl, Map<String, String> photosUrls);
    }

    public interface GroupSearchCallback {
        void groupFound(GroupChat groupChat);
        void groupNotFound();
    }
    //endregion

    private DatabaseManager() {
        init();
    }

    //region Methods
    private void init() {
        mUsersDbRef = FirebaseDatabase.getInstance().getReference().child(APP_USERS_DB_REF_NAME);
        mUsersDbRef.keepSynced(true);
        mGroupChatsDbRef = FirebaseDatabase.getInstance().getReference().child(GROUP_CHATS_DB_REF_NAME);
        mGroupChatsDbRef.keepSynced(true);
        mMessagesDbRef = FirebaseDatabase.getInstance().getReference().child(MESSAGES_DB_REF_NAME);
        mGroupChatsDbRef.keepSynced(true);
    }

    public static DatabaseManager getInstance(){
        if(sInstance == null){
            sInstance = new DatabaseManager();
        }

        return sInstance;
    }

    public DatabaseReference usersDbRef() {
        return mUsersDbRef;
    }

    public DatabaseReference groupChatsDbRef() {
        return mGroupChatsDbRef;
    }

    public DatabaseReference messagesDbRef() { return mMessagesDbRef; }

    public void createNewGroup(String iGroupName, Uri iGroupPhotoUri, final GroupCreatedCallback callback){

        final String groupId = mGroupChatsDbRef.push().getKey();

        if(groupId != null) {

            Map<String, Object> groupDetails = new HashMap<>();

            groupDetails.put("groupId", groupId);
            groupDetails.put("groupName", iGroupName);
            groupDetails.put("adminId", sCurrentFirebaseUser.getUid());
            groupDetails.put("adminName", sCurrentFirebaseUser.getFullName());
            groupDetails.put("adminDeviceToken", sCurrentFirebaseUser.getDeviceToken());
            groupDetails.put("lastMsg", sCurrentFirebaseUser.getFullName() + " Created the group");
            groupDetails.put("groupPhoto", DEFAULT_GROUP_PHOTO_URI.toString());
            groupDetails.put("timeStamp", ServerValue.TIMESTAMP);

            // Adding group details to groups database reference
            mGroupChatsDbRef.child(groupId).updateChildren(groupDetails);

            // Adding group details to my user database reference
            sCurrentFirebaseUser.currentUserGroupsDbRef().child(groupId).updateChildren(groupDetails);

            // Adding myself as a member of the group
            mGroupChatsDbRef.child(groupId).child("groupUsers").child(sCurrentFirebaseUser.getUid()).setValue(sCurrentFirebaseUser.getFirebaseClassInstance());

            if(iGroupPhotoUri != null){
                uploadGroupPhoto(iGroupPhotoUri, groupId, new GroupPhotoUploadedCallback() {
                    @Override
                    public void onPhotoUploaded() {
                        callback.onGroupCreated(groupId);
                    }
                });
            } else {
                callback.onGroupCreated(groupId);
            }
        }
    }

    public void addUserToGroup(final String userId, final String groupId) {

        mGroupChatsDbRef.child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final GroupChat groupChat = dataSnapshot.getValue(GroupChat.class);

                mUsersDbRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        GroupUser user = dataSnapshot.getValue(GroupUser.class);

                        mGroupChatsDbRef.child(groupId).child("groupUsers").child(userId).setValue(user);
                        mUsersDbRef.child(userId).child("groups").child(groupId).setValue(groupChat);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    // Remember to finish this!
    public void removeUserFromGroup(final String userId, final String groupId) {
        mGroupChatsDbRef.child(groupId).child("groupUsers").child(userId).setValue(null);
        mUsersDbRef.child(userId).child("groups").child(groupId).setValue(null);
    }

    public void uploadGroupPhoto(Uri iGroupPhoto, final String iGroupId, final GroupPhotoUploadedCallback callback) {

        final StorageReference imageStorageRef = FirebaseStorage
                .getInstance()
                .getReference()
                .child("Group_Photos")
                .child(iGroupId + ".jpg");

        UploadTask uploadTask = imageStorageRef.putFile(iGroupPhoto);

        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {
                imageStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        mGroupChatsDbRef.child(iGroupId).child("groupPhoto") .setValue(uri.toString());
                        callback.onPhotoUploaded();
                    }
                });
            }
        });
    }

    public void sendMessageToGroup(final GroupChat groupChat, final String msg){

        String messageId = mGroupChatsDbRef.child(groupChat.getGroupId()).child("messages").push().getKey();

        if(messageId != null) {

            Map<String, Object> msgDetails  = new HashMap<>();

            final Map<String, String> timeStamp = ServerValue.TIMESTAMP;

            msgDetails.put("msgId", messageId);
            msgDetails.put("msgText", msg);
            msgDetails.put("msgType", "text message");
            msgDetails.put("senderId", sCurrentFirebaseUser.getUid());
            msgDetails.put("senderName", sCurrentFirebaseUser.getFullName());
            msgDetails.put("senderPhotoUri", sCurrentFirebaseUser.getUserPhotoUrl());
            msgDetails.put("senderColor", sCurrentFirebaseUser.getUserColor());
            msgDetails.put("timeStamp", timeStamp);

            mMessagesDbRef.child(groupChat.getGroupId()).child(messageId)
                    .updateChildren(msgDetails, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            // Update group last msg content and timeStamp to this msg content and timeStamp
                            mGroupChatsDbRef.child(groupChat.getGroupId()).child("lastMsg").setValue(sCurrentFirebaseUser.getFullName() + ": " + msg);
                            mGroupChatsDbRef.child(groupChat.getGroupId()).child("timeStamp").setValue(timeStamp);

                            // Update group last msg content and timeStamp on my groups node because the groups are sorted by my group nodes
                            sCurrentFirebaseUser.currentUserGroupsDbRef().child(groupChat.getGroupId()).child("lastMsg").setValue(sCurrentFirebaseUser.getFullName() + ": " + msg);
                            sCurrentFirebaseUser.currentUserGroupsDbRef().child(groupChat.getGroupId()).child("timeStamp").setValue(timeStamp);

                            notifyGroupUsers(groupChat, msg, timeStamp);
                        }
                    });
        }
    }

    private void notifyGroupUsers(GroupChat groupChat, final String msgContent, final Map<String, String> timeStamp) {

        final Map<String, Object> notificationDetailsMap = new HashMap<>();

        notificationDetailsMap.put("groupName", groupChat.getGroupName());
        notificationDetailsMap.put("content", msgContent);
        notificationDetailsMap.put("groupId", groupChat.getGroupId());
        notificationDetailsMap.put("senderName", sCurrentFirebaseUser.getFullName());

        // Iterate through all group user's id's
        for (String userToSendToId : groupChat.getGroupUsers().keySet()) {

            // Get the node of the message notifications of the user that you want to notify
            final DatabaseReference notificationRef = mUsersDbRef.child(userToSendToId).child(NOTIFICATIONS_DB_REF_NAME).child("messages").child(groupChat.getGroupId());

            // Update the msg content and timeStamp on the user groups node because the groups are sorted by my group nodes
            mUsersDbRef.child(userToSendToId).child("groups").child(groupChat.getGroupId()).child("lastMsg").setValue(sCurrentFirebaseUser.getFullName() + ": " + msgContent);
            mUsersDbRef.child(userToSendToId).child("groups").child(groupChat.getGroupId()).child("timeStamp").setValue(timeStamp);

            final String notificationId = notificationRef.push().getKey();

            // Make sure to not notify yourself
            if (notificationId != null && !userToSendToId.equals(sCurrentFirebaseUser.getUid())) {

                mUsersDbRef.child(userToSendToId).child("deviceToken").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        String deviceToken = dataSnapshot.getValue(String.class);

                        if (deviceToken != null) {
                            notificationDetailsMap.put("deviceToken", deviceToken);

                            // Create a notification node in the destination user's database
                            notificationRef.child(notificationId).updateChildren(notificationDetailsMap);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                });
            }
        }
    }

    public void fetchGroupById(String groupId, final FetchGroupChatCallback callback) {

        mGroupChatsDbRef.child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                callback.onGroupChatFetched(dataSnapshot.getValue(GroupChat.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    public void listenToMessageNotifications(String groupId, final MessageNotificationsListener listener) {

        ValueEventListener messageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer notificationCount = dataSnapshot.getValue(Integer.class);

                if(notificationCount != null) {
                    listener.onGotNotification(notificationCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMessageValueEventListenerMap.put(groupId, messageListener);
        }

        sCurrentFirebaseUser.messageNotificationsDbRef().child(groupId).child("notificationCount")
                .addValueEventListener(messageListener);
    }

    public void stopListeningToMessageNotifications() {

        Iterator iterator = mMessageValueEventListenerMap.entrySet().iterator();

        while (iterator.hasNext())  {
            Map.Entry item = (Map.Entry) iterator.next();

            String id = item.getKey().toString();

            ValueEventListener toRemove = mMessageValueEventListenerMap.get(id);

            if(toRemove != null){
                sCurrentFirebaseUser.messageNotificationsDbRef().child(id).child("notificationCount")
                        .removeEventListener(toRemove);
                iterator.remove();
            }
        }
    }

    public void listenToGroupUsersNamesChange(String groupId, final OnGroupNamesChangeListener listener){
        mGroupNamesChangeListener = listener;
        mGroupChatsDbRef.child(groupId).child("groupUsers").addValueEventListener(sGroupUserNamesValueListener);
    }

    public void stopListeningToGroupUserNamesChange(String groupId) {
        mGroupChatsDbRef.child(groupId).child("groupUsers").removeEventListener(sGroupUserNamesValueListener);
    }

    public void listenToTypingStatus(String groupId, final OnTypingStatusChangeListener listener) {
        mTypingStatusChangeListener = listener;
        mGroupChatsDbRef.child(groupId).child("typing").addValueEventListener(sTypingValueEventListener);
    }

    public void stopListeningToTypingStatus(String groupId) {
        mGroupChatsDbRef.child(groupId).child("typing").removeEventListener(sTypingValueEventListener);
    }
//
    public void listenToGroupRequestNotification(final GroupRequestStateListener listener) {
        mGroupRequestStateListener = listener;
        sCurrentFirebaseUser.currentUserDbRef().addValueEventListener(sGroupRequestsValueEventListener);
    }

    public void stopListeningToGroupRequestNotification() {
        sCurrentFirebaseUser.currentUserDbRef().removeEventListener(sGroupRequestsValueEventListener);
    }

    public void fetchGroupPhotoUrl(String groupId, final FetchGroupPhotoCallback callback) {

        mGroupChatsDbRef.child(groupId).child("groupPhoto").addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                callback.onPhotoUrlFetched(dataSnapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    public void searchGroupById(final String searchString, final GroupSearchCallback callback) {

        mGroupChatsDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    String id = snapshot.getKey();

                    if(id != null && id.contains(searchString)) {
                        callback.groupFound(snapshot.getValue(GroupChat.class));
                        return;
                    }
                }

                //callback.groupNotFound();
                searchGroupByName(searchString, callback);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void searchGroupByName(final String searchString, final GroupSearchCallback callback) {
        mGroupChatsDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    String groupName = snapshot.child("groupName").getValue(String.class);

                    if(groupName != null && groupName.contains(searchString)) {
                        callback.groupFound(snapshot.getValue(GroupChat.class));
                        return;
                    }
                }

                callback.groupNotFound();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    public void sendGroupRequest(GroupChat groupChat) {

        Map<String, Object> requestDetails = new HashMap<>();

        String adminDeviceToken = groupChat.getAdminDeviceToken();

        requestDetails.put("deviceToken", adminDeviceToken);
        requestDetails.put("senderName", sCurrentFirebaseUser.getFullName());
        requestDetails.put("groupName", groupChat.getGroupName());
        requestDetails.put("groupId", groupChat.getGroupId());
        requestDetails.put("senderId", sCurrentFirebaseUser.getUid());
        requestDetails.put("senderPhoto", sCurrentFirebaseUser.getUserPhotoUrl());

        // Create a group request node in the group's admin database
        mUsersDbRef.child(groupChat.getAdminId()).child(NOTIFICATIONS_DB_REF_NAME).child("groupRequests")
                .child(sCurrentFirebaseUser.getUid()).updateChildren(requestDetails);
    }

    public void fetchGroupUsersDetails(GroupChat groupChat, FetchGroupUsersPhotosCallback callback) {

        final Map<String, String> usersPhotosUrl = new HashMap<>();
        final Map<String, String> userNames = new HashMap<>();

        for (final String userId : groupChat.getGroupUsers().keySet()) {
            mUsersDbRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    String url = dataSnapshot.child("photoUri").getValue(String.class);
                    String fullName = dataSnapshot.child("fullName").getValue(String.class);

                    if(url != null) {
                        usersPhotosUrl.put(userId, url);
                    }

                    if(fullName != null) {
                        userNames.put(userId, fullName);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            });
        }

        callback.onPhotosFetched(userNames, usersPhotosUrl);
    }

    public void leaveGroup(String groupId) {

        // Remove myself from group database
        sDatabaseManager.groupChatsDbRef().child(groupId).child("groupUsers").child(sCurrentFirebaseUser.getUid()).setValue(null);

        // Remove all message notifications from this group from my database
        sCurrentFirebaseUser.messageNotificationsDbRef().child(groupId).setValue(null);

        // Remove all group requests notifications from this group from my database
        sCurrentFirebaseUser.groupRequestNotificationsDbRef().child(groupId).setValue(null);

        // Remove group key reference from my database
        sCurrentFirebaseUser.currentUserGroupsDbRef().child(groupId).setValue(null);
    }
    //endregion
}