package activities;

import android.content.Intent;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.Map;

import adapters.MessageAdapter;
import de.hdodenhof.circleimageview.CircleImageView;
import database_classes.GroupChat;
import database_classes.Message;
import firebase_utils.DatabaseManager;
import fragments.MapsFragment;
import gis.hereim.R;
import utils.TypingTextWatcher;

import static activities.MainActivity.GROUP_CHAT_INTENT_EXTRA_KEY;
import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class GroupChatActivity extends AppCompatActivity implements
        SwipeRefreshLayout.OnRefreshListener, CompoundButton.OnCheckedChangeListener,
        DatabaseManager.OnGroupNamesChangeListener, DatabaseManager.OnTypingStatusChangeListener {

    private GroupChat mCurrentGroup;

    private EditText mMessageET;

    private int mMessagesToRead = 20;

    private Toolbar mToolBar;

    private String mGroupUserNames;

    private TextView mGroupChatHeaderTV;

    private Switch mMapSwitch;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private static Fragment mMapFragment;

    private static TypingTextWatcher mTypingTextWatcher;

    @Override
    protected void onPause() {
        super.onPause();
        sCurrentFirebaseUser.messageNotificationsDbRef().child(mCurrentGroup.getGroupId()).setValue(null);
        stopListeningToChatEvents();
    }

    @Override
    protected void onStart() {
        super.onStart();
        sCurrentFirebaseUser.messageNotificationsDbRef().child(mCurrentGroup.getGroupId()).setValue(null);
        sCurrentFirebaseUser.setIsOnline(true);
        startListeningToChatEvents();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentGroup = (GroupChat)getIntent().getSerializableExtra(GROUP_CHAT_INTENT_EXTRA_KEY);
        mMapFragment = MapsFragment.newInstance(mCurrentGroup);

        if(sCurrentFirebaseUser.isLoggedIn()){
            setActivityUI();
        } else{
            goToLoginPage();
        }
    }

    private void setActivityUI() {
        setContentView(R.layout.activity_group_chat);

        mMessageET = findViewById(R.id.group_chat_message_edit_text);
        mSwipeRefreshLayout = findViewById(R.id.chat_swipe_layout);
        mToolBar = findViewById(R.id.chat_tool_bar);
        mMapSwitch = mToolBar.findViewById(R.id.chat_tool_bar_go_to_map_switch);
        mGroupChatHeaderTV = mToolBar.findViewById(R.id.chat_tool_bar_group_members);

        mTypingTextWatcher = new TypingTextWatcher(mCurrentGroup.getGroupId());

        setChatWallpaper();
        loadToolBarDetails();
        loadMessagesToRecyclerView();

        mSwipeRefreshLayout.setOnRefreshListener(this);

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if(getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    mMapSwitch.setChecked(false);
                }
            }
        });
    }

    private void setChatWallpaper() {

        String backgroundImg = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_looks_chat_background", "bg_chat_default_wallpaper");

        View chatView = findViewById(R.id.chat_root_layout);

        switch (backgroundImg){
            case "res/drawable/bg_chat_default_wallpaper": chatView.setBackgroundResource(R.drawable.bg_chat_default_wallpaper); break;
            case "res/drawable/wallpaper_1.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_1); break;
            case "res/drawable/wallpaper_2.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_2); break;
            case "res/drawable/wallpaper_3.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_3); break;
            case "res/drawable/wallpaper_4.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_4); break;
            case "res/drawable/wallpaper_5.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_5); break;
            case "res/drawable/wallpaper_6.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_6); break;
            case "res/drawable/wallpaper_7.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_7); break;
            case "res/drawable/wallpaper_8.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_8); break;
            case "res/drawable/wallpaper_9.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_9); break;
            case "res/drawable/wallpaper_10.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_10); break;
            case "res/drawable/wallpaper_11.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_11); break;
            case "res/drawable/wallpaper_12.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_12); break;
            case "res/drawable/wallpaper_13.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_13); break;
            case "res/drawable/wallpaper_14.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_14); break;
            case "res/drawable/wallpaper_15.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_15); break;
            case "res/drawable/wallpaper_16.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_16); break;
            case "res/drawable/wallpaper_17.jpg": chatView.setBackgroundResource(R.drawable.wallpaper_17); break;
        }
    }

    private void loadToolBarDetails() {

        setSupportActionBar(mToolBar);

        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ((TextView)mToolBar.findViewById(R.id.chat_tool_bar_group_name))
                .setText(mCurrentGroup.getGroupName());

        mMapSwitch.setOnCheckedChangeListener(this);

        sDatabaseManager.fetchGroupPhotoUrl(mCurrentGroup.getGroupId(), new DatabaseManager.FetchGroupPhotoCallback() {
            @Override
            public void onPhotoUrlFetched(final String photoUrl) {
                Picasso.get().load(photoUrl)
                        .networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.drawable.img_blank_profile)
                        .into(((CircleImageView)mToolBar.findViewById(R.id.chat_tool_bar_group_photo)),
                                new Callback() {
                    @Override
                    public void onSuccess() { }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(photoUrl).placeholder(R.drawable.img_blank_profile)
                                .into(((CircleImageView)mToolBar.findViewById(R.id.chat_tool_bar_group_photo)));
                    }
                });
            }
        });
    }

    private void loadMessagesToRecyclerView() {

        final RecyclerView messagesRecyclerView = findViewById(R.id.chat_messages_recycler_view);

        final FirebaseRecyclerOptions<Message> options = new FirebaseRecyclerOptions.Builder<Message>().setLifecycleOwner(this)
                .setQuery(sDatabaseManager.messagesDbRef().child(mCurrentGroup.getGroupId())
                        .orderByChild("timeStamp").limitToLast(mMessagesToRead), Message.class).build();

        final RecyclerView.LayoutManager layoutManager = messagesRecyclerView.getLayoutManager();

        messagesRecyclerView.setHasFixedSize(true);

        sDatabaseManager.fetchGroupUsersDetails(mCurrentGroup, new DatabaseManager.FetchGroupUsersPhotosCallback() {
            @Override
            public void onPhotosFetched(Map<String, String> usersNames, Map<String, String> photosUrls) {

                final MessageAdapter messageAdapter = new MessageAdapter(options, usersNames, photosUrls);

                messageAdapter.startListening();

                messageAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onItemRangeInserted(int positionStart, int itemCount) {
                        super.onItemRangeInserted(positionStart, itemCount);
                        if (layoutManager != null) {
                            layoutManager.smoothScrollToPosition(messagesRecyclerView, null, messageAdapter.getItemCount());
                        }
                    }
                });

                messagesRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, final int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        if(bottom < oldBottom){
                            if (layoutManager != null) {
                                layoutManager.smoothScrollToPosition(messagesRecyclerView, null, bottom);
                            }
                        }
                    }
                });

                messagesRecyclerView.setAdapter(messageAdapter);
            }
        });
    }

    // Load previous messages to recyclerView
    @Override
    public void onRefresh() {
        mMessagesToRead *= 2;
        loadMessagesToRecyclerView();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void startListeningToChatEvents() {
        sDatabaseManager.listenToGroupUsersNamesChange(mCurrentGroup.getGroupId(), this);
        sDatabaseManager.listenToTypingStatus(mCurrentGroup.getGroupId(), this);
        mMessageET.addTextChangedListener(mTypingTextWatcher);
    }

    private void stopListeningToChatEvents() {
        sDatabaseManager.stopListeningToGroupUserNamesChange(mCurrentGroup.getGroupId());
        sDatabaseManager.stopListeningToTypingStatus(mCurrentGroup.getGroupId());
        mMessageET.removeTextChangedListener(mTypingTextWatcher);
    }

    public void sendMessageBtnClick(View view) {
        String msg = mMessageET.getText().toString().trim();

        if(!TextUtils.isEmpty(msg)){
            sDatabaseManager.sendMessageToGroup(mCurrentGroup, msg);
            mMessageET.setText("");
        }
    }

    public void openGroupInfoBtnClick(View view) {
        Intent groupChatIntent = new Intent(GroupChatActivity.this, GroupChatInfoActivity.class);
        groupChatIntent.putExtra(GROUP_CHAT_INTENT_EXTRA_KEY, mCurrentGroup);
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(GroupChatActivity.this,
                        mToolBar.findViewById(R.id.chat_tool_bar_group_photo),
                        getString(R.string.group_info_transition));
        startActivity(groupChatIntent, options.toBundle());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        toggleMapFragment(isChecked);
    }

    private void toggleMapFragment(boolean isChecked) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);

        if(isChecked){
            if(getSupportFragmentManager().getBackStackEntryCount() == 0){
                transaction.add(R.id.map_container, mMapFragment).addToBackStack("map").commit();
            } else {
                transaction.show(mMapFragment).commit();
            }
        } else {
            transaction.hide(mMapFragment).commit();
        }
    }

    @Override
    public void onGroupUserNamesChange(String names) {
        mGroupUserNames = names;
        mGroupChatHeaderTV.setText(names);
    }

    @Override
    public void onSomeoneTyping(String name) {
        String msgToDisplay = name + " " + getString(R.string.is_typing);
        mGroupChatHeaderTV.setText(msgToDisplay);
    }

    @Override
    public void onNobodyIsTyping() {
        mGroupChatHeaderTV.setText(mGroupUserNames);
    }

    private void goToLoginPage() {
        startActivity(new Intent(GroupChatActivity.this, LoginActivity.class));
        finish();
    }
}


