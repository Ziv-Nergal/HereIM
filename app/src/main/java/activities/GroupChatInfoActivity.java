package activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.squareup.picasso.Picasso;

import adapters.GroupUserAdapter;
import database_classes.GroupChat;
import database_classes.GroupUser;
import de.hdodenhof.circleimageview.CircleImageView;
import gis.hereim.R;

import static activities.MainActivity.GROUP_CHAT_INTENT_EXTRA_KEY;
import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class GroupChatInfoActivity extends AppCompatActivity {

    private GroupChat mCurrentGroup;

    private CircleImageView mGroupPhoto;

    private TextView mGroupNameTextView;
    private TextView mUsersCounterTextView;
    private TextView mGroupIdTextView;

    private RecyclerView mUsersRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_group_chat_info);

        if(sCurrentFirebaseUser.IsLoggedIn()){
            setActivityUI();
        }else{
            goToLoginPage();
        }
    }

    private void setActivityUI() {
        mCurrentGroup = (GroupChat) getIntent().getSerializableExtra(GROUP_CHAT_INTENT_EXTRA_KEY);
        mGroupNameTextView = findViewById(R.id.group_info_name);
        mGroupPhoto = findViewById(R.id.group_info_photo);
        mUsersCounterTextView = findViewById(R.id.group_info_num_of_users_counter);
        mUsersRecyclerView = findViewById(R.id.group_info_users_recycler_view);
        mGroupIdTextView = findViewById(R.id.group_info_id_of_group);

        displayGroupInfo();
        loadUsersToRecyclerView();
    }

    private void displayGroupInfo() {

        mGroupNameTextView.setText(mCurrentGroup.getGroupName());

        if(!mCurrentGroup.getGroupPhoto().equals("default")){
            Picasso
                    .get()
                    .load(mCurrentGroup.getGroupPhoto())
                    .placeholder(R.drawable.img_blank_group_chat)
                    .into(mGroupPhoto);
        }

        mUsersCounterTextView.setText(String.valueOf(mCurrentGroup.getGroupUsers().size()));

        if(mCurrentGroup.getAdminId().equals(sCurrentFirebaseUser.GetUid())){
            mGroupIdTextView.setText(mCurrentGroup.getGroupId());
        } else {
            mGroupIdTextView.setText(R.string.only_admin_can_see_this);
            findViewById(R.id.group_Info_share_id_btn).setVisibility(View.GONE);
        }
    }

    private void loadUsersToRecyclerView() {

        mUsersRecyclerView.setHasFixedSize(true);

        FirebaseRecyclerOptions<GroupUser> options = new FirebaseRecyclerOptions.Builder<GroupUser>().setLifecycleOwner(this)
                .setQuery(sDatabaseManager.GroupChatsDbRef().child(mCurrentGroup.getGroupId())
                        .child("groupUsers"), GroupUser.class).build();

        final GroupUserAdapter userAdapter = new GroupUserAdapter(this, options, mCurrentGroup);
        userAdapter.startListening();

        mUsersRecyclerView.setAdapter(userAdapter);
        mUsersRecyclerView.addItemDecoration(new DividerItemDecoration(mUsersRecyclerView.getContext(), DividerItemDecoration.VERTICAL));
    }

    public void LeaveGroupBtnClick(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.leave_group_dialog_msg)
                .setPositiveButton(R.string.dialog_positive_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sDatabaseManager.LeaveGroup(mCurrentGroup.getGroupId());
                        startActivity(new Intent(GroupChatInfoActivity.this, MainActivity.class));
                        finish();
                    }
                }).setNegativeButton(R.string.dialog_negative_btn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }

    public void ShareGroupIdClickBtn(View view) {

        String idMessage = mCurrentGroup.getGroupId() + getString(R.string.group_id_share_message);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, idMessage);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "id_message");
        startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.group_id_instructions_short)));
    }

    private void goToLoginPage() {
        startActivity(new Intent(GroupChatInfoActivity.this, LoginActivity.class));
        finish();
    }
}
