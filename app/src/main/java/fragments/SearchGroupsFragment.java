package fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.eyalbira.loadingdots.LoadingDots;
import com.podcopic.animationlib.library.AnimationType;
import com.podcopic.animationlib.library.StartSmartAnimation;
import com.squareup.picasso.Picasso;
import com.tooltip.Tooltip;

import java.util.Map;
import java.util.Random;

import database_classes.GroupChat;
import de.hdodenhof.circleimageview.CircleImageView;
import firebase_utils.DatabaseManager;
import gis.hereim.R;

import static activities.MainActivity.sCurrentFirebaseUser;
import static activities.MainActivity.sDatabaseManager;

public class SearchGroupsFragment extends Fragment implements SearchView.OnQueryTextListener {

    private View mFragmentView;

    private GroupChat mResultGroupChat;

    private Context mContext;

    private CircleImageView mGroupPhoto;

    private TextView mGroupNameTextView;
    private TextView mUsersCounterTextView;

    private Button mRequestToJoinGroupBtn;

    private Handler mDelayHandler = new Handler();

    private LoadingDots mLoadingDotsAnimation;

    public SearchGroupsFragment() { /* Required empty public constructor */ }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mFragmentView = inflater.inflate(R.layout.fragment_search_groups, container, false);

        mGroupPhoto = mFragmentView.findViewById(R.id.search_groups_result_group_photo);
        mGroupNameTextView = mFragmentView.findViewById(R.id.search_groups_result_group_name);
        mUsersCounterTextView = mFragmentView.findViewById(R.id.search_groups_result_group_num_of_users);
        mRequestToJoinGroupBtn = mFragmentView.findViewById(R.id.search_groups_request_to_join_group_btn);
        mLoadingDotsAnimation = mFragmentView.findViewById(R.id.search_groups_loading_dots);

        SearchView groupSearchView = mFragmentView.findViewById(R.id.fragment_search_group_search_view);
        groupSearchView.setOnQueryTextListener(this);

        Tooltip tooltip = new Tooltip.Builder(groupSearchView)
                .setText(R.string.search_groups_instructions)
                .setArrowEnabled(true)
                .setBackgroundColor(getResources().getColor(R.color.tooltip_color))
                .setTextColor(Color.WHITE)
                .setCancelable(true)
                .setCornerRadius((float)10)
                .setGravity(Gravity.BOTTOM)
                .build();
        tooltip.show();

        return mFragmentView;
    }


    @Override
    public boolean onQueryTextSubmit(final String query) {

        hideGroupResultViews();

        mLoadingDotsAnimation.setVisibility(View.VISIBLE);

        sDatabaseManager.searchGroupById(query.trim(), new DatabaseManager.GroupSearchCallback() {
            @Override
            public void groupFound(GroupChat groupChat) {
                displayGroup(groupChat);
            }

            @Override
            public void groupNotFound() {
                mDelayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, R.string.group_not_found, Toast.LENGTH_SHORT).show();
                        mLoadingDotsAnimation.setVisibility(View.GONE);
                    }
                }, 1500);
            }
        });

        return false;
    }

    private void displayGroup(final GroupChat groupChat) {

        Picasso
                .get()
                .load(groupChat.getGroupPhoto())
                .placeholder(R.drawable.img_blank_group_chat)
                .into(mGroupPhoto);

        mGroupNameTextView.setText(groupChat.getGroupName());
        mUsersCounterTextView.setText(String.valueOf(groupChat.getGroupUsers().size()));

        mDelayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLoadingDotsAnimation.setVisibility(View.GONE);
                showGroupResultViews();
            }
        }, new Random().nextInt(1500) + 2000);

        mRequestToJoinGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Map<String, Object> groupUsers = groupChat.getGroupUsers();

                if (groupUsers.containsKey(sCurrentFirebaseUser.getUid())) {
                    Toast.makeText(mContext, "You are already a member in this group!", Toast.LENGTH_SHORT).show();
                } else if(!groupUsers.containsKey(groupChat.getAdminId())) {
                    Toast.makeText(mContext, "No admin for this group so no body can join it!", Toast.LENGTH_SHORT).show();
                } else {
                    sDatabaseManager.sendGroupRequest(groupChat);
                    Toast.makeText(mContext, "Request sent!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showGroupResultViews() {
        mGroupPhoto.setVisibility(View.VISIBLE);
        mGroupNameTextView.setVisibility(View.VISIBLE);
        mFragmentView.findViewById(R.id.search_groups_counter_layout).setVisibility(View.VISIBLE);
        mRequestToJoinGroupBtn.setVisibility(View.VISIBLE);

        StartSmartAnimation.startAnimation(mGroupPhoto, AnimationType.FadeInLeft, 500, 0, false);
        StartSmartAnimation.startAnimation(mGroupNameTextView, AnimationType.FadeInRight, 500, 0, false);
        StartSmartAnimation.startAnimation(mFragmentView.findViewById(R.id.search_groups_counter_layout), AnimationType.FadeInLeft, 500, 0, false);
        StartSmartAnimation.startAnimation(mRequestToJoinGroupBtn, AnimationType.FadeInRight, 500, 0, false);
    }

    private void hideGroupResultViews() {
        mGroupPhoto.setVisibility(View.INVISIBLE);
        mGroupNameTextView.setVisibility(View.INVISIBLE);
        mFragmentView.findViewById(R.id.search_groups_counter_layout).setVisibility(View.INVISIBLE);
        mRequestToJoinGroupBtn.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onQueryTextChange(String newText) { return false; }
}
