package activities;

import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.squareup.picasso.Picasso;

import adapters.GroupUserAdapter;
import database_classes.GroupChat;
import database_classes.GroupUser;
import de.hdodenhof.circleimageview.CircleImageView;
import firebase_utils.DatabaseManager;
import gis.hereim.R;

import static activities.MainActivity.GROUP_CHAT_INTENT_EXTRA_KEY;
import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class GroupChatInfoActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    //region Constants
    public static final int MINIMUM_DISTANCE_FROM_ADMIN = 100;
    public static final int MAXIMUM_DISTANCE_FROM_ADMIN = 4900;
    private static final String DEBUG_TAG = "SEEK_BAR";
    //endregion

    //region Class Members
    private GroupChat mCurrentGroup;
    private CircleImageView mGroupPhoto;
    private TextView mGroupNameTextView;
    private TextView mUsersCounterTextView;
    private TextView mGroupIdTextView;
    private TextView mDistanceValueTextView;
    private RecyclerView mUsersRecyclerView;
    private SeekBar mDistanceFromAdminSeekBar;
    //endregion

    //region Overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_group_chat_info);

        if(sCurrentFirebaseUser.isLoggedIn()){
            setActivityUI();
        }else{
            goToLoginPage();
        }
    }

    //______________________________________________________________________________________________

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        Log.e(DEBUG_TAG, "onProgressChanged");
        changeDistanceTextValue(i + MINIMUM_DISTANCE_FROM_ADMIN);
    }

    //______________________________________________________________________________________________

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.e(DEBUG_TAG, "onStartTrackingTouch");
    }

    //______________________________________________________________________________________________

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.e(DEBUG_TAG, "onStopTrackingTouch");
        int progress = seekBar.getProgress();
        sDatabaseManager.groupChatsDbRef().child(mCurrentGroup.getGroupId())
                .child("allowedDistanceFromAdmin").setValue(progress + MINIMUM_DISTANCE_FROM_ADMIN);
    }
    //endregion

    //region Methods
    private void setActivityUI() {
        mCurrentGroup = (GroupChat) getIntent().getSerializableExtra(GROUP_CHAT_INTENT_EXTRA_KEY);
        mGroupNameTextView = findViewById(R.id.group_info_name);
        mGroupPhoto = findViewById(R.id.group_info_photo);
        mUsersCounterTextView = findViewById(R.id.group_info_num_of_users_counter);
        mUsersRecyclerView = findViewById(R.id.group_info_users_recycler_view);
        mGroupIdTextView = findViewById(R.id.group_info_id_of_group);
        mDistanceFromAdminSeekBar = findViewById(R.id.group_info_distance_seek_bar);
        mDistanceValueTextView = findViewById(R.id.group_info_distance_text_view);

        if(!mCurrentGroup.getAdminId().equals(sCurrentFirebaseUser.getUid())) {
            findViewById(R.id.distance_layout).setVisibility(View.GONE);
        } else {
            initDistanceSeekBar();
        }

        displayGroupInfo();
        loadUsersToRecyclerView();
    }

    //______________________________________________________________________________________________

    private void initDistanceSeekBar() {

        mDistanceFromAdminSeekBar.setMax(MAXIMUM_DISTANCE_FROM_ADMIN);

        sDatabaseManager.fetchGroupDistanceFromAdmin(mCurrentGroup,
                new DatabaseManager.FetchGroupDistanceFromAdminCallback() {
            @Override
            public void onDistanceFetched(int distance) {
                    mDistanceFromAdminSeekBar.setProgress(distance);
                    changeDistanceTextValue(distance);
                }
            });

        mDistanceFromAdminSeekBar.setOnSeekBarChangeListener(this);
    }

    //______________________________________________________________________________________________

    private void changeDistanceTextValue(int value) {
        String distance = value + "m";
        mDistanceValueTextView.setText(distance);
    }

    //______________________________________________________________________________________________

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

        if(mCurrentGroup.getAdminId().equals(sCurrentFirebaseUser.getUid())){
            mGroupIdTextView.setText(mCurrentGroup.getGroupId());
        } else {
            mGroupIdTextView.setText(R.string.only_admin_can_see_this);
            findViewById(R.id.group_Info_share_id_btn).setVisibility(View.GONE);
        }
    }

    //______________________________________________________________________________________________

    private void loadUsersToRecyclerView() {

        mUsersRecyclerView.setHasFixedSize(true);

        FirebaseRecyclerOptions<GroupUser> options = new FirebaseRecyclerOptions.Builder<GroupUser>()
                .setLifecycleOwner(this)
                .setQuery(sDatabaseManager.groupChatsDbRef().child(mCurrentGroup.getGroupId())
                        .child("groupUsers"), GroupUser.class).build();

        final GroupUserAdapter userAdapter = new GroupUserAdapter(this, options,
                mCurrentGroup, GroupUserAdapter.eViewTypes.Group_Info_View);
        userAdapter.startListening();

        userAdapter.setUserClickListener(new GroupUserAdapter.OnUserClickListener() {
            @Override
            public void onClickGroupUser(final GroupUser user) {
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(GroupChatInfoActivity.this);

                builder.setMessage(String.format("kick %s from %s group chat?", user.getFullName(),
                        mCurrentGroup.getGroupName())).setPositiveButton(R.string.dialog_positive_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sDatabaseManager.removeUserFromGroup(user.getUid(),
                                mCurrentGroup.getGroupId());
                    }
                }).setNegativeButton(R.string.dialog_negative_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        });

        mUsersRecyclerView.setAdapter(userAdapter);
        mUsersRecyclerView.addItemDecoration(new DividerItemDecoration(mUsersRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL));
    }

    //______________________________________________________________________________________________

    public void leaveGroupBtnClick(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.leave_group_dialog_msg)
                .setPositiveButton(R.string.dialog_positive_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sDatabaseManager.leaveGroup(mCurrentGroup.getGroupId());
                        startActivity(new Intent(GroupChatInfoActivity.this,
                                MainActivity.class));
                        finish();
                    }
                }).setNegativeButton(R.string.dialog_negative_btn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }

    //______________________________________________________________________________________________

    public void shareGroupIdClickBtn(View view) {

        String idMessage = mCurrentGroup.getGroupId() +
                getString(R.string.group_id_share_message);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, idMessage);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "id_message");
        startActivity(Intent.createChooser(shareIntent,
                getResources().getString(R.string.group_id_instructions_short)));
    }

    //______________________________________________________________________________________________

    private void goToLoginPage() {
        startActivity(new Intent(GroupChatInfoActivity.this, LoginActivity.class));
        finish();
    }
    //endregion
}
