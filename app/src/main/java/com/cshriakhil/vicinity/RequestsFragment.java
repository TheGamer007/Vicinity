package com.cshriakhil.vicinity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class RequestsFragment extends Fragment {
    private static final String TAG = RequestsFragment.class.getSimpleName();
    private final int FLAG_USER_FRIEND = 1;


    RecyclerView mRecycler;
    RequestListAdapter mAdapter;
    SwipeRefreshLayout mSwipe;
    TextView mEmpty;

    DatabaseReference mFriendsRef;
    DatabaseReference mRequestsRef;
    DatabaseReference mUsersRef;

    String mUserUid = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        super.onViewCreated(view, savedInstanceState);

        mEmpty = view.findViewById(R.id.emptyview_requests);

        mSwipe = view.findViewById(R.id.swipe_refresh_requests);
        mSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mAdapter.mRequests.clear();
                mAdapter.notifyDataSetChanged();
                getRequests();
            }
        });

        mRecycler = view.findViewById(R.id.recycler_requests);
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mAdapter = new RequestListAdapter();
        mRecycler.setAdapter(mAdapter);

        mFriendsRef = FirebaseDatabase.getInstance().getReference().child("friends");
        mRequestsRef = FirebaseDatabase.getInstance().getReference().child("requests");
        mUsersRef = FirebaseDatabase.getInstance().getReference().child("users");

        mUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        getRequests();
    }

    public void getRequests() {
        mSwipe.setRefreshing(true);

        mAdapter.mRequests = new ArrayList<>();

        mRequestsRef
                .child(mUserUid)
                .orderByValue()
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.getChildrenCount() != 0 ) {
                            // hide empty view
                            mEmpty.setVisibility(View.GONE);
                        }

                        for (final DataSnapshot requesterUid : dataSnapshot.getChildren()) {
                            // insert at start because latest requests first
                            mUsersRef.child(requesterUid.getKey())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            mAdapter.mRequests.add(0, new String[] {dataSnapshot.getValue(UserData.class).name, requesterUid.getKey()});
                                            mAdapter.notifyItemInserted(0);
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            Log.i(TAG, "getRequests/mLocRef/onCancelled: " + databaseError.getMessage());
                                        }
                                    });
                        }
                        mSwipe.setRefreshing(false);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.i(TAG, "getRequests/onCancelled " + databaseError.getMessage());
                    }
                });
    }

    class RequestListAdapter extends RecyclerView.Adapter<RequestViewHolder> {
        ArrayList<String[]> mRequests;

        @NonNull
        @Override
        public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RequestViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RequestViewHolder holder, final int position) {
            holder.mName.setText(mRequests.get(position)[0]);

            holder.mAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // add user to own list
                    mFriendsRef.child(mRequests.get(position)[1]).child(mUserUid).setValue(FLAG_USER_FRIEND);
                    // add self to user's list
                    mFriendsRef.child(mUserUid).child(mRequests.get(position)[1]).setValue(FLAG_USER_FRIEND);
                    // remove request since completed
                    mRequestsRef.child(mUserUid).child(mRequests.get(position)[1]).removeValue();

                    //remove from recycler
                    mRequests.remove(position);
                    mAdapter.notifyItemRemoved(position);

                    if (mRequests.size() == 0) {
                        // display empty view
                        mEmpty.setVisibility(View.VISIBLE);
                    }
                }
            });

            holder.mReject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // remove the request node
                    mRequestsRef.child(mUserUid).child(mRequests.get(position)[1]).removeValue();

                    // remove from recycler
                    mRequests.remove(position);
                    mAdapter.notifyItemRemoved(position);

                    if (mRequests.size() == 0) {
                        // display empty view
                        mEmpty.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return (mRequests != null) ? (mRequests.size()) : 0;
        }
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView mName;
        ImageView mAccept;
        ImageView mReject;

        public RequestViewHolder(View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.tv_name);
            mAccept = itemView.findViewById(R.id.iv_accept);
            mReject = itemView.findViewById(R.id.iv_reject);
        }
    }
}
