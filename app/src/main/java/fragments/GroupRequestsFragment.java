package fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerOptions;

import adapters.OnItemsCountChangeListener;
import adapters.GroupRequestsAdapter;
import database_classes.GroupRequest;
import gis.hereim.R;

import static activities.MainActivity.sCurrentFirebaseUser;

public class GroupRequestsFragment extends Fragment implements OnItemsCountChangeListener {

    private RecyclerView mGroupRequestsRecyclerView;

    private TextView mNoRequestsTextView;

    public GroupRequestsFragment() { /* Required empty public constructor */ }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_group_requests, container, false);

        mGroupRequestsRecyclerView = fragmentView.findViewById(R.id.group_requests_fragment_recycler_view);
        mNoRequestsTextView = fragmentView.findViewById(R.id.group_requests_fragment_no_requests_message);

        loadRequestsToRecyclerView();

        return fragmentView;
    }

    private void loadRequestsToRecyclerView() {

        mGroupRequestsRecyclerView.setHasFixedSize(true);
        mGroupRequestsRecyclerView.addItemDecoration(new DividerItemDecoration(mGroupRequestsRecyclerView.getContext(), DividerItemDecoration.VERTICAL));

        FirebaseRecyclerOptions<GroupRequest> options = new FirebaseRecyclerOptions.Builder<GroupRequest>().setLifecycleOwner(this)
                .setQuery(sCurrentFirebaseUser.groupRequestNotificationsDbRef(), GroupRequest.class).build();

        GroupRequestsAdapter groupRequestsAdapter = new GroupRequestsAdapter(options, this);

        groupRequestsAdapter.startListening();

        mGroupRequestsRecyclerView.setAdapter(groupRequestsAdapter);
    }

    @Override
    public void onItemsCountChange(int count) {
        if(count == 0){
            mNoRequestsTextView.setVisibility(View.VISIBLE);
        } else {
            mNoRequestsTextView.setVisibility(View.INVISIBLE);
        }
    }
}
