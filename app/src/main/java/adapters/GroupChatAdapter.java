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

    private Context mContext;

    private Map<String, ValueEventListener> mMsgNotificationsValueEventListenerMap = new HashMap<>();
    private Map<String, ValueEventListener> mTypingValueEventListenerMap = new HashMap<>();

    private OnItemsCountChangeListener mItemsCountChangeListener;

    private OnGroupChatClickListener mGroupChatClickListener;
    private OnGroupChatPhotoClickListener mGroupChatPhotoClickListener;

    public interface OnGroupChatClickListener {
        void onGroupChatClick(View view, GroupChat groupChat);
    }

    public interface OnGroupChatPhotoClickListener {
        void onGroupChatPhotoClick(View view, GroupChat groupChat);
    }

    public void setGroupChatClickListener(OnGroupChatClickListener iGroupChatClickListener) {
        this.mGroupChatClickListener = iGroupChatClickListener;
    }

    public void setGroupChatPhotoClickListener(OnGroupChatPhotoClickListener iGroupChatPhotoClickListener) {
        this.mGroupChatPhotoClickListener = iGroupChatPhotoClickListener;
    }

    @Override
    public int getItemCount() {
        int count = super.getItemCount();
        mItemsCountChangeListener.onItemsCountChange(count);
        return count;
    }

    public GroupChatAdapter(@NonNull FirebaseRecyclerOptions<GroupChat> options, OnItemsCountChangeListener itemsCountChangeListener, Context context) {
        super(options);
        this.mItemsCountChangeListener = itemsCountChangeListener;
        mContext = context;
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
        groupChatViewHolder.BindView(groupChat);
    }

    public void removeAllListeners() {

        for (String groupId : mMsgNotificationsValueEventListenerMap.keySet()) {
            sCurrentFirebaseUser.MessageNotificationsDbRef().child(groupId).child("notificationCount")
                    .removeEventListener(Objects.requireNonNull(mMsgNotificationsValueEventListenerMap.get(groupId)));
        }

        for (String groupId : mTypingValueEventListenerMap.keySet()) {
            sDatabaseManager.GroupChatsDbRef().child(groupId).child("typing")
                    .removeEventListener(Objects.requireNonNull(mTypingValueEventListenerMap.get(groupId)));
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull GroupChatViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        sCurrentFirebaseUser.MessageNotificationsDbRef().child(holder.getViewHolderId()).child("notificationCount")
                .removeEventListener(Objects.requireNonNull(mMsgNotificationsValueEventListenerMap.get(holder.getViewHolderId())));

        sDatabaseManager.GroupChatsDbRef().child(holder.getViewHolderId()).child("typing")
                .removeEventListener(Objects.requireNonNull(mTypingValueEventListenerMap.get(holder.getViewHolderId())));
    }

    class GroupChatViewHolder extends BaseViewHolder<GroupChat> {

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
        void BindView(final GroupChat iGroupChat) {

            mGroupName.setText(iGroupChat.getGroupName());
            mLastMsg.setText(iGroupChat.getLastMsg());
            mMsgTimeStamp.setText(TimeStampParser.AccurateParse(iGroupChat.getTimeStamp()));

            sDatabaseManager.FetchGroupPhotoUrl(iGroupChat.getGroupId(), new DatabaseManager.FetchGroupPhotoCallback() {
                @Override
                public void onPhotoUrlFetched(final String photoUrl) {
                    Picasso.get().load(photoUrl).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.img_blank_group_chat).into(mGroupPhoto, new Callback() {
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
                    sDatabaseManager.FetchGroupById(iGroupChat.getGroupId(), new DatabaseManager.FetchGroupChatCallback() {
                        @Override
                        public void onGroupChatFetched(GroupChat groupChat) {
                            mGroupChatClickListener.onGroupChatClick(view, groupChat);
                        }
                    });
                }
            });

            mGroupPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    sDatabaseManager.FetchGroupById(iGroupChat.getGroupId(), new DatabaseManager.FetchGroupChatCallback() {
                        @Override
                        public void onGroupChatFetched(GroupChat groupChat) {
                            mGroupChatPhotoClickListener.onGroupChatPhotoClick(view, groupChat);
                        }
                    });
                }
            });

            this.setViewHolderId(iGroupChat.getGroupId());
            listenToMessageNotifications(iGroupChat);
            listenToUsersTypingEvents(iGroupChat);
        }

        private void listenToMessageNotifications(GroupChat iGroupChat) {

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

            mMsgNotificationsValueEventListenerMap.put(iGroupChat.getGroupId(), notificationValueEventListener);
            sCurrentFirebaseUser.MessageNotificationsDbRef().child(iGroupChat.getGroupId()).child("notificationCount")
                    .addValueEventListener(Objects.requireNonNull(mMsgNotificationsValueEventListenerMap.get(iGroupChat.getGroupId())));
        }

        private void listenToUsersTypingEvents(final GroupChat iGroupChat) {

            ValueEventListener typingEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String typingUserName = dataSnapshot.getValue(String.class);

                    if(typingUserName != null && !typingUserName.equals(sCurrentFirebaseUser.GetFullName()) && !typingUserName.equals("nobody")){
                        String msgToDisplay = dataSnapshot.getValue(String.class) + " " + mContext.getResources().getString(R.string.is_typing);
                        mLastMsg.setText(msgToDisplay);
                        mLastMsg.setTextColor(mContext.getResources().getColor(R.color.green));
                    } else {
                        mLastMsg.setText(iGroupChat.getLastMsg());
                        mLastMsg.setTextColor(mContext.getResources().getColor(R.color.default_text_color));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            };

            mTypingValueEventListenerMap.put(iGroupChat.getGroupId(), typingEventListener);
            sDatabaseManager.GroupChatsDbRef().child(iGroupChat.getGroupId()).child("typing")
                    .addValueEventListener(Objects.requireNonNull(mTypingValueEventListenerMap.get(iGroupChat.getGroupId())));
        }
    }
}
