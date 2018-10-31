package com.cshriakhil.vicinity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123; // Request Code for Sign In Activity

    private FirebaseAuth mFirebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();

        if (mFirebaseAuth.getCurrentUser() != null) {
            // already signed in, proceed to MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            // not signed in, launch sign in UI
            Intent i = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build()
                    ))
                    .build();
            startActivityForResult(i, RC_SIGN_IN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // sign in successful
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                // Sign in failed

                if (response == null) {
                    // LocData cancelled it
                    Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, "No network connection", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    Toast.makeText(this, "Unkown Error encountered", Toast.LENGTH_SHORT).show();
                    return;
                }

            }
        }
    }
}
