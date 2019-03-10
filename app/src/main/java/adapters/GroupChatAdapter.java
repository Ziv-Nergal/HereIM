package adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import database_classes.GroupChat;
import firebase_utils.DatabaseManager;
import gis.hereim.R;
import utils.TimeStampParser;

import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class GroupChatAdapter extends FirebaseRecyclerAdapter<GroupChat, GroupChatAdapter.GroupChatViewHolder> {

    private static int sDefaultTextColor;
    private static int sGreenTextColor;

    private static String sIsTypingMsg;

    private static Map<String, ValueEventListener> sMsgNotificationsValueEventListenerMap = new HashMap<>();
    private static Map<String, ValueEventListener> sTypingValueEventListenerMap = new HashMap<>();

    private static OnItemsCountChangeListener sItemsCountChangeListener;

    private static OnGroupChatClickListener sGroupChatClickListener;
    private static OnGroupChatPhotoClickListener sGroupChatPhotoClickListener;

    public interface OnGroupChatClickListener {
        void onGroupChatClick(View view, GroupChat groupChat);
    }

    public interface OnGroupChatPhotoClickListener {
        void onGroupChatPhotoClick(View view, GroupChat groupChat);
    }

    public void setGroupChatClickListener(OnGroupChatClickListener iGroupChatClickListener) {
        sGroupChatClickListener = iGroupChatClickListener;
    }

    public void setGroupChatPhotoClickListener(OnGroupChatPhotoClickListener iGroupChatPhotoClickListener) {
        sGroupChatPhotoClickListener = iGroupChatPhotoClickListener;
    }

    @Override
    public int getItemCount() {
        int count = super.getItemCount();
        sItemsCountChangeListener.onItemsCountChange(count);
        return count;
    }

    public GroupChatAdapter(Context context, @NonNull FirebaseRecyclerOptions<GroupChat> options, OnItemsCountChangeListener itemsCountChangeListener) {
        super(options);
        sItemsCountChangeListener = itemsCountChangeListener;
        sDefaultTextColor = context.getResources().getColor(R.color.default_text_color);
        sGreenTextColor = context.getResources().getColor(R.color.green);
        sIsTypingMsg =  context.getResources().getString(R.string.is_typing);
    }

    @NonNull
    @Override
    public GroupChatViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_group_chat, viewGroup, false);

        return new GroupChatViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull final GroupChatViewHolder groupChatViewHolder, final int position, @NonNull GroupChat groupChat) {
        groupChatViewHolder.bindView(groupChat);
    }

    public void removeAllListeners() {

        for (String groupId : sMsgNotificationsValueEventListenerMap.keySet()) {
            sCurrentFirebaseUser.messageNotificationsDbRef().child(groupId).child("notificationCount")
                    .removeEventListener(Objects.requireNonNull(sMsgNotificationsValueEventListenerMap.get(groupId)));
        }

        for (String groupId : sTypingValueEventListenerMap.keySet()) {
            sDatabaseManager.groupChatsDbRef().child(groupId).child("typing")
                    .removeEventListener(Objects.requireNonNull(sTypingValueEventListenerMap.get(groupId)));
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull GroupChatViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        sCurrentFirebaseUser.messageNotificationsDbRef().child(holder.getViewHolderId()).child("notificationCount")
                .removeEventListener(Objects.requireNonNull(sMsgNotificationsValueEventListenerMap.get(holder.getViewHolderId())));

        sDatabaseManager.groupChatsDbRef().child(holder.getViewHolderId()).child("typing")
                .removeEventListener(Objects.requireNonNull(sTypingValueEventListenerMap.get(holder.getViewHolderId())));
    }

    static class GroupChatViewHolder extends BaseViewHolder<GroupChat> {

        private CircleImageView mGroupPhoto;

        private TextView mGroupName;
        private TextView mLastMsg;
        private TextView mMsgTimeStamp;
        private TextView mNotificationsBubble;

        GroupChatViewHolder(@NonNull View itemView) {
            super(itemView);
            mGroupPhoto = itemView.findViewById(R.id.group_cell_photo);
            mGroupName = itemView.findViewById(R.id.group_cell_name);
            mLastMsg = itemView.findViewById(R.id.group_cell_last_msg);
            mMsgTimeStamp = itemView.findViewById(R.id.group_cell_time_stamp);
            mNotificationsBubble = itemView.findViewById(R.id.group_cell_notification_counter);
        }

        @Override
        void bindView(final GroupChat groupChat) {

            setViewHolderId(groupChat.getGroupId());

            mGroupName.setText(groupChat.getGroupName());
            mLastMsg.setText(groupChat.getLastMsg());
            mMsgTimeStamp.setText(TimeStampParser.AccurateParse(groupChat.getTimeStamp()));

            sDatabaseManager.fetchGroupPhotoUrl(groupChat.getGroupId(), new DatabaseManager.FetchGroupPhotoCallback() {
                @Override
                public void onPhotoUrlFetched(final String photoUrl) {
                    Picasso.get().load(photoUrl).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.img_blank_group_chat)
                            .into(mGroupPhoto, new Callback() {
                        @Override
                        public void onSuccess() {
                            itemView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(photoUrl).placeholder(R.drawable.img_blank_group_chat).into(mGroupPhoto);
                            itemView.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    sDatabaseManager.fetchGroupById(groupChat.getGroupId(), new DatabaseManager.FetchGroupChatCallback() {
                        @Override
                        public void onGroupChatFetched(GroupChat groupChat) {
                            sGroupChatClickListener.onGroupChatClick(view, groupChat);
                        }
                    });
                }
            });

            mGroupPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    sDatabaseManager.fetchGroupById(groupChat.getGroupId(), new DatabaseManager.FetchGroupChatCallback() {
                        @Override
                        public void onGroupChatFetched(GroupChat groupChat) {
                            sGroupChatPhotoClickListener.onGroupChatPhotoClick(view, groupChat);
                        }
                    });
                }
            });

            listenToMessageNotifications(groupChat);
            listenToUsersTypingEvents(groupChat);
        }

        private void listenToMessageNotifications(GroupChat groupChat) {

            ValueEventListener notificationValueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Integer notificationCount = dataSnapshot.getValue(Integer.class);

                    if(notificationCount != null && notificationCount > 0) {
                        mNotificationsBubble.setVisibility(View.VISIBLE);
                        mNotificationsBubble.setText(String.valueOf(notificationCount));
                    } else {
                        mNotificationsBubble.setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            };

            sMsgNotificationsValueEventListenerMap.put(groupChat.getGroupId(), notificationValueEventListener);
            sCurrentFirebaseUser.messageNotificationsDbRef().child(groupChat.getGroupId()).child("notificationCount")
                    .addValueEventListener(Objects.requireNonNull(sMsgNotificationsValueEventListenerMap.get(groupChat.getGroupId())));
        }

        private void listenToUsersTypingEvents(final GroupChat groupChat) {

            ValueEventListener typingEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String typingUserName = dataSnapshot.getValue(String.class);

                    if(typingUserName != null && !typingUserName.equals(sCurrentFirebaseUser.getFullName()) && !typingUserName.equals("nobody")){
                        String msgToDisplay = dataSnapshot.getValue(String.class) + " " +sIsTypingMsg;
                        mLastMsg.setText(msgToDisplay);
                        mLastMsg.setTextColor(sGreenTextColor);
                    } else {
                        mLastMsg.setText(groupChat.getLastMsg());
                        mLastMsg.setTextColor(sDefaultTextColor);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            };

            sTypingValueEventListenerMap.put(groupChat.getGroupId(), typingEventListener);
            sDatabaseManager.groupChatsDbRef().child(groupChat.getGroupId()).child("typing")
                    .addValueEventListener(Objects.requireNonNull(sTypingValueEventListenerMap.get(groupChat.getGroupId())));
        }
    }
}
