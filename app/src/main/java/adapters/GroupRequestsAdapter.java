package adapters;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.github.jorgecastilloprz.FABProgressCircle;
import com.github.jorgecastilloprz.listeners.FABProgressListener;
import com.squareup.picasso.Picasso;

import database_classes.GroupRequest;
import de.hdodenhof.circleimageview.CircleImageView;
import gis.hereim.R;

import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class GroupRequestsAdapter extends FirebaseRecyclerAdapter<GroupRequest, GroupRequestsAdapter.GroupRequestViewHolder> {

    private OnItemsCountChangeListener mItemsCountChangeListener;

    public GroupRequestsAdapter(@NonNull FirebaseRecyclerOptions<GroupRequest> options, OnItemsCountChangeListener itemsCountChangeListener) {
        super(options);
        this.mItemsCountChangeListener = itemsCountChangeListener;
    }

    @Override
    public int getItemCount() {
        int count = super.getItemCount();
        mItemsCountChangeListener.onItemsCountChange(count);
        return count;
    }

    @NonNull
    @Override
    public GroupRequestViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_group_request, viewGroup, false);

        return new GroupRequestViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull GroupRequestViewHolder holder, int position, @NonNull GroupRequest groupRequest) {
        holder.BindView(groupRequest);
    }

    class GroupRequestViewHolder extends BaseViewHolder<GroupRequest>{

        private CircleImageView mSenderPhoto;

        private TextView mSenderName;
        private TextView mGroupToJoinName;

        private ImageButton mAcceptRequestBtn;
        private ImageButton mDenyRequestBtn;

        GroupRequestViewHolder(@NonNull View itemView) {
            super(itemView);

            mSenderPhoto = itemView.findViewById(R.id.group_request_cell_sender_photo);
            mSenderName = itemView.findViewById(R.id.group_request_cell_sender_name);
            mGroupToJoinName = itemView.findViewById(R.id.group_request_cell_group_name);
            mAcceptRequestBtn = itemView.findViewById(R.id.group_request_cell_accept_btn);
            mDenyRequestBtn = itemView.findViewById(R.id.group_request_cell_deny_btn);
        }

        @Override
        void BindView(final GroupRequest groupRequest) {

            Picasso.get().load(groupRequest.getSenderPhoto()).placeholder(R.drawable.img_blank_profile).into(mSenderPhoto);
            mSenderName.setText(groupRequest.getSenderName());
            mGroupToJoinName.setText(groupRequest.getGroupName());

            mAcceptRequestBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sDatabaseManager.AddUserToGroup(groupRequest.getSenderId(), groupRequest.getGroupId());
                    sCurrentFirebaseUser.GroupRequestNotificationsDbRef().child(groupRequest.getSenderId()).setValue(null);
                }
            });

            mDenyRequestBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sCurrentFirebaseUser.GroupRequestNotificationsDbRef().child(groupRequest.getSenderId()).setValue(null);
                }
            });
        }
    }
}
