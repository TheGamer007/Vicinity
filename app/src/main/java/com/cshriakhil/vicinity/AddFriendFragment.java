package com.cshriakhil.vicinity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.chip.Chip;
import com.hootsuite.nachos.terminator.ChipTerminatorHandler;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;

import java.util.ArrayList;

public class AddFriendFragment extends Fragment {
    private static final String TAG = AddFriendFragment.class.getSimpleName();
    NachoTextView ntv;

    DatabaseReference mRequestsRef;
    DatabaseReference mUsersRef;

    String mUserUid = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRequestsRef = FirebaseDatabase.getInstance().getReference().child("requests");
        mUsersRef = FirebaseDatabase.getInstance().getReference().child("users");
        mUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ntv = view.findViewById(R.id.tv_nacho);
        ntv.addChipTerminator(' ', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_TO_TERMINATOR);
        ntv.enableEditChipOnTouch(false, false);

        view.findViewById(R.id.btn_add_email).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewFriends(v);
            }
        });
    }

    public void addNewFriends(View v) {
        ArrayList<String> failed = new ArrayList<>();
        for (Chip c: ntv.getAllChips()) {
            CharSequence tmp_email = c.getText();
            if (!TextUtils.isEmpty(tmp_email) && Patterns.EMAIL_ADDRESS.matcher(tmp_email).matches()) {
                // this means the CharSequence is a valid email address
                // Send request
                sendRequest(tmp_email.toString());
            } else {
                Log.i(TAG, "addNewFriends: Invalid email: " + tmp_email);
                failed.add(tmp_email.toString());
            }
        }

        if (failed.size() == 0) {
            // All succeeded
            Toast.makeText(getContext(), "All requests sent", Toast.LENGTH_SHORT).show();
            ntv.setText("");
        } else if (failed.size() <= 20) {
            // display the emails
            StringBuffer msg = new StringBuffer("Requests to the following " + failed.size() + " addresses failed.\n");
            msg.append("Please check if the emails are valid. \n");
            for (String s : failed) {
                msg.append(s);
                msg.append("\n");
            }

            new LovelyInfoDialog(this.getContext())
                    .setTopColorRes(R.color.colorPrimaryDark)
                    .setTopTitle("Requests Status")
                    .setTopTitleColor(Color.WHITE)
                    .setConfirmButtonText("OK")
                    .setMessage(msg)
                    .show();
        } else {
            // display number of failed and ask to use less than 20 emails at a time for details
            new LovelyInfoDialog(this.getContext())
                    .setTopColorRes(R.color.colorPrimaryDark)
                    .setTopTitle("Requests Status")
                    .setTopTitleColor(Color.WHITE)
                    .setConfirmButtonText("OK")
                    .setMessage("Requests to " + failed.size() + " addresses failed.\nA list of invalid emails is displayed if their count is less than 20.\n")
                    .show();
        }
    }

    public void sendRequest(String email) {
        // first get the proper key of the user
        Log.i(TAG, "sendRequest: with " + email);
        mUsersRef
                .orderByChild("email")
                .equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // there should only be one
                        for (DataSnapshot target : dataSnapshot.getChildren()) {
                            //Log.i(TAG, "onDataChange: " + child.getValue(UserData.class).email + " === " + child.getKey());
                            // add request, value is timestamp
                            mRequestsRef.child(target.getKey()).child(mUserUid).setValue(ServerValue.TIMESTAMP);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.i(TAG, "sendRequests/onCancelled " + databaseError.getMessage());
                    }
                });
    }
}
