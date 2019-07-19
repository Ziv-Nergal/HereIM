package adapters;

import android.content.Context;
import androidx.annotation.NonNull;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

import database_classes.GroupChat;
import de.hdodenhof.circleimageview.CircleImageView;
import database_classes.GroupUser;
import gis.hereim.R;

import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class GroupUserAdapter extends FirebaseRecyclerAdapter<GroupUser, BaseViewHolder<GroupUser>>{

    public enum eViewTypes {
        Group_Info_View,
        Map_View
    }

    private eViewTypes mViewType;

    private Map<String, ValueEventListener> mValueEventListenerMap = new HashMap<>();

    private int mGreenColor;
    private int mRedColor;

    private String mOnlineStr;
    private String mOfflineStr;

    private GroupChat mGroupChat;

    private GroupUserAdapter.OnUserClickListener mUserClickListener;

    public interface OnUserClickListener {
        void onClickGroupUser(GroupUser user);
    }

    public void setUserClickListener(OnUserClickListener removeUserClickListener) {
        this.mUserClickListener = removeUserClickListener;
    }

    public GroupUserAdapter(Context context, @NonNull FirebaseRecyclerOptions<GroupUser> options,
                            GroupChat groupChat, eViewTypes viewType) {
        super(options);
        mGroupChat = groupChat;
        mGreenColor = context.getResources().getColor(R.color.green);
        mRedColor = context.getResources().getColor(R.color.colorAccent);
        mOnlineStr = context.getString(R.string.online);
        mOfflineStr = context.getString(R.string.offline);
        mViewType = viewType;
    }

    @NonNull
    @Override
    public BaseViewHolder<GroupUser> onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View view;
        BaseViewHolder<GroupUser> viewHolder = null;

        switch (mViewType) {
            case Map_View:
                view  = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.item_map_user, viewGroup, false);
                viewHolder = new MapUserViewHolder(view);
                break;
            case Group_Info_View:
                view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.item_group_user, viewGroup, false);
                viewHolder = new GroupUserViewHolder(view);
                break;
        }

        return viewHolder;
    }

    @Override
    protected void onBindViewHolder(@NonNull final BaseViewHolder<GroupUser> viewHolder,
                                    int position, @NonNull GroupUser user) {

        ValueEventListener groupUserValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GroupUser currentUser = dataSnapshot.getValue(GroupUser.class);

                if(currentUser != null){
                    viewHolder.bindView(currentUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
       };

        viewHolder.setViewHolderId(user.getUid());
        mValueEventListenerMap.put(user.getUid(), groupUserValueEventListener);
        sDatabaseManager.usersDbRef().child(user.getUid())
                .addValueEventListener(groupUserValueEventListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull BaseViewHolder<GroupUser> holder) {
        super.onViewDetachedFromWindow(holder);
        sDatabaseManager.usersDbRef().child(holder.getViewHolderId()).removeEventListener(
                Objects.requireNonNull(mValueEventListenerMap.get(holder.getViewHolderId())));
    }

    private class GroupUserViewHolder extends BaseViewHolder<GroupUser> {

        private CircleImageView mUserPhoto;
        private TextView mUserName;
        private TextView mUserStatus;
        private TextView mIsAdmin;
        private TextView mUserOnlineState;
        private ImageButton mRemoveUserBtn;

        GroupUserViewHolder(@NonNull View itemView) {
            super(itemView);
            mUserPhoto = itemView.findViewById(R.id.user_cell_photo);
            mUserName = itemView.findViewById(R.id.user_item_name);
            mUserStatus = itemView.findViewById(R.id.user_item_status);
            mIsAdmin = itemView.findViewById(R.id.user_item_is_admin);
            mUserOnlineState = itemView.findViewById(R.id.user_item_online_state);
            mRemoveUserBtn = itemView.findViewById(R.id.user_item_remove_user_btn);
        }

        @Override
        void bindView(final GroupUser groupUser) {

            mUserName.setText(groupUser.getFullName());
            mUserStatus.setText(groupUser.getStatus());

            if(groupUser.getOnline()){
                mUserOnlineState.setText(mOnlineStr);
                mUserOnlineState.setTextColor(mGreenColor);
            } else {
                mUserOnlineState.setText(mOfflineStr);
                mUserOnlineState.setTextColor(mRedColor);
            }

            if(groupUser.getUid().equals(mGroupChat.getAdminId())){
                mIsAdmin.setVisibility(View.VISIBLE);
            }

            if(mGroupChat.getAdminId().equals(sCurrentFirebaseUser.getUid())){
                if(!groupUser.getUid().equals(sCurrentFirebaseUser.getUid())){
                    mRemoveUserBtn.setVisibility(View.VISIBLE);
                }
            }

            Picasso.get().load(groupUser.getPhotoUri()).networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.img_blank_profile).into(mUserPhoto, new Callback() {
                @Override
                public void onSuccess() { }

                @Override
                public void onError(Exception e) {
                    Picasso.get().load(groupUser.getPhotoUri())
                            .placeholder(R.drawable.img_blank_profile).into(mUserPhoto);
                }
            });

            mRemoveUserBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mUserClickListener.onClickGroupUser(groupUser);
                }
            });
        }
    }

    private class MapUserViewHolder extends BaseViewHolder<GroupUser> {

        private CircleImageView mUserPhoto;
        private TextView mUserName;

        MapUserViewHolder(@NonNull View itemView) {
            super(itemView);
            mUserPhoto = itemView.findViewById(R.id.item_map_user_photo);
            mUserName = itemView.findViewById(R.id.item_map_user_name);
        }

        @Override
        void bindView(final GroupUser groupUser) {

            mUserName.setText(groupUser.getFullName());

            if(groupUser.getOnline()){
                mUserPhoto.setBorderColor(Color.GREEN);
            } else {
                mUserPhoto.setBorderColor(Color.RED);
            }

            Picasso.get().load(groupUser.getPhotoUri()).networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.img_blank_profile).into(mUserPhoto, new Callback() {
                @Override
                public void onSuccess() { }

                @Override
                public void onError(Exception e) {
                    Picasso.get().load(groupUser.getPhotoUri())
                            .placeholder(R.drawable.img_blank_profile).into(mUserPhoto);
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mUserClickListener.onClickGroupUser(groupUser);
                }
            });
        }
    }
}
