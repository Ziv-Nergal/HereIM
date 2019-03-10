package activities;

import android.content.Intent;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import adapters.MessageAdapter;
import de.hdodenhof.circleimageview.CircleImageView;
import database_classes.GroupChat;
import database_classes.Message;
import firebase_utils.DatabaseManager;
import gis.hereim.R;

import static activities.MainActivity.GROUP_CHAT_INTENT_EXTRA_KEY;
import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class GroupChatActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private GroupChat mCurrentGroup;

    private EditText mMsgEditText;

    private RecyclerView mMessagesRecyclerView;

    private MessageAdapter mMessageAdapter;

    private LinearLayoutManager mLinearLayoutManager;

    private int mMessagesToRead = 20;

    private Toolbar mToolBar;

    private String mGroupUserNames;

    private TextView mGroupTypingUpdatesTextView;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private long mDelay = 1000;
    private long mLastTextEdit = 0;

    private Handler mDelayHandler = new Handler();

    private Runnable mStopTypingRunnable = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() > (mLastTextEdit + mDelay - 500)) {
                sDatabaseManager.groupChatsDbRef().child(mCurrentGroup.getGroupId()).child("typing").setValue("nobody");
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        sCurrentFirebaseUser.messageNotificationsDbRef().child(mCurrentGroup.getGroupId()).setValue(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        sCurrentFirebaseUser.messageNotificationsDbRef().child(mCurrentGroup.getGroupId()).setValue(null);
        sCurrentFirebaseUser.setIsOnline(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(sCurrentFirebaseUser.isLoggedIn()){
            mCurrentGroup = (GroupChat)getIntent().getSerializableExtra(GROUP_CHAT_INTENT_EXTRA_KEY);
            setActivityUI();
            startListeningToTypingEvents();
        } else{
            goToLoginPage();
        }
    }

    private void setActivityUI() {

        setContentView(R.layout.activity_group_chat);

        setChatWallpaper();

        mMsgEditText = findViewById(R.id.group_chat_message_edit_text);
        mMessagesRecyclerView = findViewById(R.id.chat_messages_recycler_view);
        mSwipeRefreshLayout = findViewById(R.id.chat_swipe_layout);

        mSwipeRefreshLayout.setOnRefreshListener(this);

        loadToolBarDetails();
        loadMessagesToRecyclerView();
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
        }
    }

    private void loadToolBarDetails() {

        mToolBar = findViewById(R.id.chat_tool_bar);
        setSupportActionBar(mToolBar);

        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ((TextView)mToolBar.findViewById(R.id.chat_tool_bar_group_name)).setText(mCurrentGroup.getGroupName());
        mGroupTypingUpdatesTextView = mToolBar.findViewById(R.id.chat_tool_bar_group_members);

        sDatabaseManager.fetchGroupPhotoUrl(mCurrentGroup.getGroupId(), new DatabaseManager.FetchGroupPhotoCallback() {
            @Override
            public void onPhotoUrlFetched(final String photoUrl) {
                Picasso.get().load(photoUrl).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.img_blank_profile)
                        .into(((CircleImageView)mToolBar.findViewById(R.id.chat_tool_bar_group_photo)), new Callback() {
                    @Override
                    public void onSuccess() { }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(photoUrl).placeholder(R.drawable.img_blank_profile).into(((CircleImageView)mToolBar.findViewById(R.id.chat_tool_bar_group_photo)));
                    }
                });
            }
        });

        sDatabaseManager.fetchGroupUsersNameList(mCurrentGroup.getGroupId(), new DatabaseManager.OnGroupNamesChangeListener() {
            @Override
            public void onNamesChange(String names) {
                mGroupUserNames = names;
                mGroupTypingUpdatesTextView.setText(names);
            }
        });
    }

    private void loadMessagesToRecyclerView() {

        FirebaseRecyclerOptions<Message> options = new FirebaseRecyclerOptions.Builder<Message>().setLifecycleOwner(this)
                .setQuery(sDatabaseManager.groupChatsDbRef().child(mCurrentGroup.getGroupId())
                        .child("messages").orderByChild("timeStamp").limitToLast(mMessagesToRead), Message.class).build();

        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);

        mMessageAdapter = new MessageAdapter(options);
        mMessageAdapter.startListening();

        mMessageAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                mLinearLayoutManager.smoothScrollToPosition(mMessagesRecyclerView, null, mMessageAdapter.getItemCount());
            }
        });

        mMessagesRecyclerView.setHasFixedSize(true);
        mMessagesRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessagesRecyclerView.setAdapter(mMessageAdapter);

        mMessagesRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, final int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if(bottom < oldBottom){
                    mLinearLayoutManager.smoothScrollToPosition(mMessagesRecyclerView, null, bottom);
                }
            }
        });
    }

    // Load previous messages to recyclerView
    @Override
    public void onRefresh() {

        mMessagesToRead *= 2;

        FirebaseRecyclerOptions<Message> options = new FirebaseRecyclerOptions.Builder<Message>().setLifecycleOwner(this)
                .setQuery(sDatabaseManager.groupChatsDbRef().child(mCurrentGroup.getGroupId())
                        .child("messages").orderByChild("timeStamp").limitToLast(mMessagesToRead), Message.class).build();

        mMessageAdapter.stopListening();
        mMessageAdapter = new MessageAdapter(options);
        mMessageAdapter.startListening();

        mMessagesRecyclerView.setAdapter(mMessageAdapter);

        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void startListeningToTypingEvents() {

        sDatabaseManager.listenToTypingStatus(mCurrentGroup.getGroupId(), new DatabaseManager.OnTypingStatusChangeListener() {
            @Override
            public void onSomeoneTyping(String name) {
                String msgToDisplay = name + " " + getString(R.string.is_typing);
                mGroupTypingUpdatesTextView.setText(msgToDisplay);
            }

            @Override
            public void onNobodyIsTyping() {
                mGroupTypingUpdatesTextView.setText(mGroupUserNames);
            }
        });

        mMsgEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s.toString()) && count > before) {
                    sDatabaseManager.groupChatsDbRef().child(mCurrentGroup.getGroupId()).child("typing").setValue(sCurrentFirebaseUser.getFullName());
                    mDelayHandler.removeCallbacks(mStopTypingRunnable);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() > 0) {
                    mLastTextEdit = System.currentTimeMillis();
                    mDelayHandler.postDelayed(mStopTypingRunnable, mDelay);
                }
            }
        });
    }

    public void sendMessageBtnClick(View view) {

        String msg = mMsgEditText.getText().toString().trim();

        if(!TextUtils.isEmpty(msg)){
            sDatabaseManager.sendMessageToGroup(mCurrentGroup.getGroupId(), msg);
            mMsgEditText.setText("");
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

    private void goToLoginPage() {
        startActivity(new Intent(GroupChatActivity.this, LoginActivity.class));
        finish();
    }
}


