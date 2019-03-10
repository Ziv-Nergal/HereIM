package activities;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.Objects;

import firebase_utils.DatabaseManager;
import firebase_utils.FirebaseUser;
import fragments.GroupChatsFragment;
import fragments.GroupRequestsFragment;
import fragments.MyProfileFragment;
import fragments.SearchGroupsFragment;
import gis.hereim.R;
import gis.hereim.SettingsActivity;
import utils.SoundFxManager;

public class MainActivity extends AppCompatActivity {

    public static final String GROUP_CHAT_INTENT_EXTRA_KEY = "group_chat";

    public static FirebaseUser sCurrentFirebaseUser;
    public static DatabaseManager sDatabaseManager;

    private BottomNavigationView mBottomNavView;

    @Override
    protected void onStart() {
        super.onStart();
        ((NotificationManager) Objects.requireNonNull(getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE))).cancelAll();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            Fragment selectedFragment;

            switch (item.getItemId()) {
                default:
                case R.id.navigation_group_chats:
                    selectedFragment = new GroupChatsFragment();
                    break;
                case R.id.navigation_my_profile:
                    selectedFragment = new MyProfileFragment();
                    break;
                case R.id.navigation_group_requests:
                    selectedFragment = new GroupRequestsFragment();
                    break;
                case R.id.navigation_search_groups:
                    selectedFragment = new SearchGroupsFragment();
                    break;
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.main_fragment_container, selectedFragment)
                    .commit();

            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sCurrentFirebaseUser = FirebaseUser.GetInstance();

        if(sCurrentFirebaseUser.IsLoggedIn()){
            sCurrentFirebaseUser.SetIsOnline(true);
            sDatabaseManager = DatabaseManager.GetInstance();
            SoundFxManager.InitManager(this);
            setActivityUI();
        }else {
            goToLoginPage();
        }
    }

    private void setActivityUI() {

        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.main_tool_bar));

        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mBottomNavView = findViewById(R.id.bottom_navigation);
        mBottomNavView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        listenToGroupRequests();

        if(getIntent().getStringExtra("group_request") != null){
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_container, new GroupRequestsFragment()).commit();
            mBottomNavView.setSelectedItemId(R.id.navigation_group_requests);
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_container, new GroupChatsFragment()).commit();
        }

    }

    private void listenToGroupRequests() {

        sDatabaseManager.ListenToGroupRequestNotification(new DatabaseManager.GroupRequestStateListener() {
            @Override
            public void onStateChanged(boolean haveGroupRequests) {

                if(haveGroupRequests){
                    mBottomNavView.getMenu().findItem(R.id.navigation_group_requests).setIcon(R.drawable.ic_bottom_nav_notifications_accent);
                } else {
                    mBottomNavView.getMenu().findItem(R.id.navigation_group_requests).setIcon(R.drawable.ic_bottom_nav_notifications);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.log_out_menu_item:
                logOutMenuItemClick();
                return true;
            case R.id.settings_menu_item:
                openSettingsMenuItemClick();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openSettingsMenuItemClick() {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
    }

    private void logOutMenuItemClick() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.log_out_msg).setPositiveButton(R.string.dialog_positive_btn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sCurrentFirebaseUser.Logout();
                goToLoginPage();
                dialog.dismiss();
            }
        }).setNegativeButton(R.string.dialog_negative_btn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }

    private void goToLoginPage() {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}
