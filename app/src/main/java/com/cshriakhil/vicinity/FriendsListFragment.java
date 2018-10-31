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

public class FriendsListFragment extends Fragment {
    private static final String TAG = FriendsListFragment.class.getSimpleName();
    private final int FLAG_USER_FRIEND = 1;

    RecyclerView mRecycler;
    FriendListAdapter mAdapter;
    SwipeRefreshLayout mSwipe;
    TextView mEmpty;

    DatabaseReference mFriendsRef;
    DatabaseReference mUsersRef;

    String mUserUid = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmpty = view.findViewById(R.id.emptyview_friends);

        mSwipe = view.findViewById(R.id.swipe_refresh_friends);
        mSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mAdapter.mFriends.clear();
                mAdapter.notifyDataSetChanged();
                getFriendsList();
            }
        });

        mRecycler = view.findViewById(R.id.recycler_friends);
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mAdapter = new FriendListAdapter();
        mRecycler.setAdapter(mAdapter);

        mFriendsRef = FirebaseDatabase.getInstance().getReference().child("friends");
        mUsersRef = FirebaseDatabase.getInstance().getReference().child("users");

        mUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        getFriendsList();
    }

    void getFriendsList() {
        mSwipe.setRefreshing(true);
        Log.i(TAG, "getting friends\' list");

        mAdapter.mFriends = new ArrayList<>();
        mFriendsRef
                .child(mUserUid)
                .orderByValue()
                .equalTo(FLAG_USER_FRIEND)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.getChildrenCount() != 0 ) {
                            // hide empty view
                            mEmpty.setVisibility(View.GONE);
                        }

                        for (final DataSnapshot friendUid : dataSnapshot.getChildren()) {
                            mUsersRef.child(friendUid.getKey())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Log.i(TAG, "onDataChange: " + dataSnapshot.getValue(UserData.class).name);
                                            mAdapter.mFriends.add(new String[] {dataSnapshot.getValue(UserData.class).name, friendUid.getKey()});
                                            mAdapter.notifyItemInserted(mAdapter.mFriends.size()-1);
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            Log.i(TAG, "getFriendsList/mLocRef/onCancelled: " + databaseError.getMessage());
                                        }
                                    });
                        }
                        mSwipe.setRefreshing(false);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.i(TAG, "getFriendsList/mFriendsRef/onCancelled: " + databaseError.getMessage());
                    }
                });
    }

    class FriendListAdapter extends RecyclerView.Adapter<FriendViewHolder> {
        ArrayList<String[]> mFriends;

        @NonNull
        @Override
        public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new FriendViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull FriendViewHolder holder, final int position) {
            holder.mName.setText(mFriends.get(position)[0]);
            holder.mRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // onClick triggered. Remove from database
                    Log.i(TAG, "onClick: Unfriend-ing " + mFriends.get(position)[0]);
                    // remove them from own list of friends
                    mFriendsRef.child(mUserUid).child(mFriends.get(position)[1]).removeValue();
                    // remove self from their list
                    mFriendsRef.child(mFriends.get(position)[1]).child(mUserUid).removeValue();
                    mFriends.remove(position);
                    mAdapter.notifyItemRemoved(position);

                    if (mFriends.size() == 0) {
                        // display empty view
                        mEmpty.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return (mFriends != null)?(mFriends.size()):0;
        }
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView mName;
        ImageView mRemove;
        public FriendViewHolder(View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.tv_name);
            mRemove = itemView.findViewById(R.id.iv_remove);
        }
    }
}
