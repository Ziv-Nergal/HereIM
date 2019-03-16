package firebase_utils;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
import java.util.Map;

import database_classes.GroupChat;
import database_classes.GroupUser;

import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class DatabaseManager {

    private static final Uri DEFAULT_GROUP_PHOTO_URI = Uri.parse("android.resource://gis.hereim/drawable/img_blank_group_chat");

    private static final String APP_USERS_DB_REF_NAME = "Users";

    private static final String GROUP_CHATS_DB_REF_NAME = "Group Chats";

    static final String NOTIFICATIONS_DB_REF_NAME = "Notifications";

    private static DatabaseManager sInstance = null;

    private DatabaseReference mUsersDbRef;
    private DatabaseReference mGroupChatsDbRef;

    public interface OnGroupCreatedListener {
        void onCreated(String groupId);
    }

    public interface OnGroupPhotoUploadedListener {
        void onPhotoUploaded();
    }

    public interface OnGroupNamesChangeListener {
        void onNamesChange(String names);
    }

    public interface OnTypingStatusChangeListener {
        void onSomeoneTyping(String name);
        void onNobodyIsTyping();
    }

    public interface GroupRequestStateListener {
        void onStateChanged(boolean haveGroupRequests);
    }

    public interface FetchGroupPhotoCallback {
        void onPhotoUrlFetched(String photoUrl);
    }

    public interface FetchGroupChatCallback {
        void onGroupChatFetched(GroupChat groupChat);
    }

    public interface GroupSearchCallback {
        void groupFound(GroupChat groupChat);
        void groupNotFound();
    }

    private DatabaseManager() {
        init();
    }

    private void init() {
        mUsersDbRef = FirebaseDatabase.getInstance().getReference().child(APP_USERS_DB_REF_NAME);
        mUsersDbRef.keepSynced(true);
        mGroupChatsDbRef = FirebaseDatabase.getInstance().getReference().child(GROUP_CHATS_DB_REF_NAME);
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

    public void createNewGroup(String iGroupName, Uri iGroupPhotoUri, final OnGroupCreatedListener groupCreatedListener){

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

            // Adding group details to groups node in database
            mGroupChatsDbRef.child(groupId).updateChildren(groupDetails);

            // Adding group details to my user database reference
            sCurrentFirebaseUser.currentUserGroupsDbRef().child(groupId).updateChildren(groupDetails);

            // Adding myself as a member of the group
            mGroupChatsDbRef.child(groupId).child("groupUsers").child(sCurrentFirebaseUser.getUid()).setValue(sCurrentFirebaseUser.getFirebaseClassInstance());

            if(iGroupPhotoUri != null){
                uploadGroupPhoto(iGroupPhotoUri, groupId, new OnGroupPhotoUploadedListener() {
                    @Override
                    public void onPhotoUploaded() {
                        groupCreatedListener.onCreated(groupId);
                    }
                });
            } else {
                groupCreatedListener.onCreated(groupId);
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

    public void uploadGroupPhoto(Uri iGroupPhoto, final String iGroupId, final OnGroupPhotoUploadedListener photoUploadedListener) {

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
                        photoUploadedListener.onPhotoUploaded();
                    }
                });
            }
        });
    }

    public void sendMessageToGroup(final String iGroupId, final String iMsg){

        String messageId = mGroupChatsDbRef.child(iGroupId).child("messages").push().getKey();

        if(messageId != null) {

            Map<String, Object> msgDetails  = new HashMap<>();

            final Map<String, String> timeStamp = ServerValue.TIMESTAMP;

            msgDetails.put("msgId", messageId);
            msgDetails.put("msgText", iMsg);
            msgDetails.put("msgType", "text message");
            msgDetails.put("senderId", sCurrentFirebaseUser.getUid());
            msgDetails.put("senderName", sCurrentFirebaseUser.getFullName());
            msgDetails.put("senderPhotoUri", sCurrentFirebaseUser.getUserPhotoUrl());
            msgDetails.put("senderColor", sCurrentFirebaseUser.getUserColor());
            msgDetails.put("timeStamp", timeStamp);

            mGroupChatsDbRef.child(iGroupId).child("messages").child(messageId)
                    .updateChildren(msgDetails, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            // Update group last msg content and timeStamp to this msg content and timeStamp
                            mGroupChatsDbRef.child(iGroupId).child("lastMsg").setValue(sCurrentFirebaseUser.getFullName() + ": " + iMsg);
                            mGroupChatsDbRef.child(iGroupId).child("timeStamp").setValue(timeStamp);

                            // Update group last msg content and timeStamp on my groups node because the groups are sorted by my group nodes
                            sCurrentFirebaseUser.currentUserGroupsDbRef().child(iGroupId).child("lastMsg").setValue(sCurrentFirebaseUser.getFullName() + ": " + iMsg);
                            sCurrentFirebaseUser.currentUserGroupsDbRef().child(iGroupId).child("timeStamp").setValue(timeStamp);

                            fetchGroupById(iGroupId, new FetchGroupChatCallback() {
                                @Override
                                public void onGroupChatFetched(GroupChat groupChat) {
                                    // Send msg notification to all group users
                                    notifyGroupUsers(groupChat, iMsg, timeStamp);
                                }
                            });
                        }
                    });
        }
    }

    public void fetchGroupById(String groupId, final FetchGroupChatCallback fetchGroupChatCallback) {

        mGroupChatsDbRef.child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                fetchGroupChatCallback.onGroupChatFetched(dataSnapshot.getValue(GroupChat.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
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

    public void fetchGroupUsersNameList(String groupId, final OnGroupNamesChangeListener onGroupNamesChangeListener){

        mGroupChatsDbRef.child(groupId).child("groupUsers").addValueEventListener(new ValueEventListener() {
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

                    onGroupNamesChangeListener.onNamesChange(groupUserNames);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    public void listenToTypingStatus(String groupId, final OnTypingStatusChangeListener onTypingStatusChangeListener) {

        mGroupChatsDbRef.child(groupId).child("typing").addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String typingUserName = dataSnapshot.getValue(String.class);

                if(typingUserName != null && !typingUserName.equals(sCurrentFirebaseUser.getFullName()) && !typingUserName.equals("nobody")){
                    onTypingStatusChangeListener.onSomeoneTyping(typingUserName);
                } else {
                    onTypingStatusChangeListener.onNobodyIsTyping();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void listenToGroupRequestNotification(final GroupRequestStateListener groupRequestStateListener) {

        sCurrentFirebaseUser.currentUserDbRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.hasChild(NOTIFICATIONS_DB_REF_NAME)){
                    groupRequestStateListener.onStateChanged(true);
                } else {
                    groupRequestStateListener.onStateChanged(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    public void fetchGroupPhotoUrl(String groupId, final FetchGroupPhotoCallback fetchGroupPhotoCallback) {

        mGroupChatsDbRef.child(groupId).child("groupPhoto").addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                fetchGroupPhotoCallback.onPhotoUrlFetched(dataSnapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    public void searchGroupById(final String groupId, final GroupSearchCallback groupSearchCallback) {

        mGroupChatsDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(groupId)){
                    groupSearchCallback.groupFound(dataSnapshot.child(groupId).getValue(GroupChat.class));
                } else {
                    groupSearchCallback.groupNotFound();
                }
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
}