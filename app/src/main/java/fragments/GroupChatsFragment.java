package fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.tooltip.Tooltip;

import activities.CreateNewGroupActivity;
import adapters.OnItemsCountChangeListener;
import database_classes.GroupChat;
import activities.GroupChatActivity;
import gis.hereim.R;
import adapters.GroupChatAdapter;

import static activities.MainActivity.GROUP_CHAT_INTENT_EXTRA_KEY;
import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class GroupChatsFragment extends Fragment implements GroupChatAdapter.OnGroupChatClickListener, GroupChatAdapter.OnGroupChatPhotoClickListener, OnItemsCountChangeListener {

    private Context mContext;

    private RecyclerView mGroupChatsRecyclerView;

    private GroupChatAdapter mGroupChatsAdapter;

    private TextView mNoChatsTextView;

    private Tooltip mAddGroupChatToolTip;

    public GroupChatsFragment() { /* Required empty public constructor */ }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onPause() {
        super.onPause();
        sDatabaseManager.stopListeningToMessageNotifications();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_group_chats, container, false);

        fragmentView.findViewById(R.id.group_chats_fragment_create_new_group_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewGroupBtnClick();
            }
        });

        mGroupChatsRecyclerView = fragmentView.findViewById(R.id.group_chat_recycler_view);
        mNoChatsTextView = fragmentView.findViewById(R.id.group_chats_fragment_no_chats_text_view);

        mAddGroupChatToolTip = new Tooltip.Builder(fragmentView.findViewById(R.id.group_chats_fragment_create_new_group_btn))
                .setText("Click to create a new group chat!")
                .setDismissOnClick(true)
                .setArrowEnabled(true)
                .setBackgroundColor(getResources().getColor(R.color.tooltip_color))
                .setTextColor(Color.WHITE)
                .setCornerRadius((float)10)
                .setMargin(Float.parseFloat("5"))
                .setGravity(Gravity.START)
                .build();

        loadGroupChatsToRecyclerView();

        return fragmentView;
    }

    private void loadGroupChatsToRecyclerView() {

        FirebaseRecyclerOptions<GroupChat> options = new FirebaseRecyclerOptions.Builder<GroupChat>().setLifecycleOwner(this)
                .setQuery(sCurrentFirebaseUser.currentUserGroupsDbRef().orderByChild("timeStamp"), GroupChat.class).build();

        mGroupChatsAdapter = new GroupChatAdapter(mContext, options, this);
        mGroupChatsAdapter.setGroupChatClickListener(this);
        mGroupChatsAdapter.setGroupChatPhotoClickListener(this);
        mGroupChatsAdapter.startListening();

        mGroupChatsRecyclerView.setHasFixedSize(true);
        mGroupChatsRecyclerView.addItemDecoration(new DividerItemDecoration(mGroupChatsRecyclerView.getContext(), DividerItemDecoration.VERTICAL));

        mGroupChatsRecyclerView.setAdapter(mGroupChatsAdapter);
    }

    @SuppressLint("InflateParams")
    private void createNewGroupBtnClick() {
        startActivity(new Intent(mContext, CreateNewGroupActivity.class));
    }

    @Override
    public void onGroupChatClick(View view, GroupChat groupChat) {
        Intent chatIntent = new Intent(mContext, GroupChatActivity.class);
        chatIntent.putExtra(GROUP_CHAT_INTENT_EXTRA_KEY, groupChat);
        startActivity(chatIntent);
    }

    @Override
    public void onGroupChatPhotoClick(View view, GroupChat groupChat) {
        if (getFragmentManager() != null) {
            DisplayGroupPhotoFragment displayPhotoFragment = DisplayGroupPhotoFragment.newInstance(groupChat);
            displayPhotoFragment.show(getFragmentManager(), "");
        }
    }

    @Override
    public void onItemsCountChange(int count) {
        if(count == 0){
            mNoChatsTextView.setVisibility(View.VISIBLE);
            mAddGroupChatToolTip.show();

        } else {
            mNoChatsTextView.setVisibility(View.INVISIBLE);
            mAddGroupChatToolTip.dismiss();
        }
    }
}
