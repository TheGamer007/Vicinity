package com.cshriakhil.vicinity;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.wafflecopter.multicontactpicker.ContactResult;
import com.wafflecopter.multicontactpicker.MultiContactPicker;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private String TAG = getClass().getSimpleName();

    private final int LOCATION_PERMISSION_REQUEST = 110;
    private final int LOCATION_SETTINGS_REQUEST = 111;
    private final int CONTACT_PICKER_REQUEST = 112;

    private final int LOCATION_REQUEST_INTERVAL = 5 * 1000;
    private final int LOCATION_REQUEST_FASTEST_INTERVAL = 2 * 1000;

    private final double search_radius = 0.5; // in km

    private final String KEY_USER_LOCATION = "locations";
    private final String KEY_USER_FRIENDS = "friends";

    private final int FLAG_USER_FRIEND = 1;
    private final int FLAG_USER_BLOCKED = -1;

    private GoogleMap mMap;
    Location mLastLocation;
    LatLng mTargetLatLng;
//    Marker mCurrMarker;
    Marker mTargetMarker;
    Circle mTargetCircle;
    TextView mLatitudeDisplay, mLongitudeDisplay, mIdDisplay;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    // to check state of req, so that there is no dialog loop
    private boolean locationRequestStarted = false;
    private boolean locationRequestCompleted = false;

    private FirebaseDatabase mDatabase;
    private FirebaseAuth mAuth;
    private DatabaseReference mLocationsReference;
    private DatabaseReference mFriendsReference;

    private String mCurrUserName = "default";
    private String mCurrUserEmail = "default@default.com";
    private String mCurrUserUid = "123default456";

    protected void createLocationRequest() {
        // assign the state variables
        locationRequestStarted = true;
        locationRequestCompleted = false;

        // Build the location request
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_REQUEST_INTERVAL); // interval at which location requests will be sent; might be faster if other apps are also requesting location
        mLocationRequest.setFastestInterval(LOCATION_REQUEST_FASTEST_INTERVAL); // fastest speed your app can handle without UI issues
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // Use the location request object to check location settings state (whether request can be sent)
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build())
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        // Location settings satisfied, finally send the location request here.
                        try {
                            Log.i(TAG, "onSuccess: requesting loc updates");
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
                        } catch (SecurityException secEx) {
                            // Permissions not given (mostly)
                            Log.i(TAG, "onSuccess: " + secEx.getMessage());
                        }
                        locationRequestCompleted = true;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "onFailure: LocationSettingsResponse failure reached");
                        if (e instanceof ResolvableApiException) {
                            // Location settings are not satisfied
                            // Show user a dialog
                            try {
                                ResolvableApiException resEx = (ResolvableApiException) e;
                                resEx.startResolutionForResult(MainActivity.this, LOCATION_SETTINGS_REQUEST);
                            } catch (IntentSender.SendIntentException ex) {
                                // ignore exception
                            }
                        }
                        locationRequestCompleted = false; // since we want users to definitely enable location services
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: reached");
        // TODO check if crashing due to the async
        /* Possible States:
        *   Start   End
        *   false   false   => First time after opening activity => Show dialog (true)
        *   true    false   => The previous request failed => Show dialog (true)
        *   false   true    => State should not be possible to reach => Don't care
        *   true    true    => The previous request succeeded => Don't show dialog (false)
        *
        *   Suitable conditions: start NAND end; equivalent to !start OR !end
        * */
        if (!locationRequestStarted || !locationRequestCompleted) {
            // onResume is called after onCreate, where the permission checks take place.
            // however, onRequestPermissionsResult seems to return asynchronously
            createLocationRequest();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: reached");
        // stop location updates
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Database, and references
        mDatabase = FirebaseDatabase.getInstance();
        mLocationsReference = mDatabase.getReference().child(KEY_USER_LOCATION);
        mFriendsReference = mDatabase.getReference().child(KEY_USER_FRIENDS);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        getUserInfo();
        addUserIfNotExists();

        // Location Client that will be handling the requests
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Location Callback that is triggered when location updates are successfully received.
        // Most of the functional app code will be called through this block.
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // get LocationResult object with list of results
                handleLocationUpdate(locationResult.getLastLocation());
            }
        };

        // Get the map fragment from layout
        SupportMapFragment mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));

        // Get the Map object from fragment in an asynchronous manner
        mapFragment.getMapAsync(this);

        // Obtain the various display elements
        mLatitudeDisplay = (TextView) findViewById(R.id.tv_lat);
        mLongitudeDisplay = (TextView) findViewById(R.id.tv_lng);
        mIdDisplay = (TextView) findViewById(R.id.tv_android_id);
        mIdDisplay.setText(mCurrUserEmail);

        // Check necessary permissions / request them if needed
        requestPerms();
    }

    void requestPerms() {
        Log.i(TAG, "requestPerms: reached");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission_group.LOCATION)
                == PackageManager.PERMISSION_DENIED) {
            // Location permissions not granted; request them
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // TODO show a dialog with rationale
                Log.i(TAG, "requestPerms: this is my rationale request log");
            } else {
                // requesting perms from user by displaying the standard dialog
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult: reached");
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permissions have been granted
                    Log.i(TAG, "onRequestPermissionsResult: permissions granted");
                } else {
                    // permissions have not been granted. TODO what to do here? Close app?
                    Log.i(TAG, "onRequestPermissionsResult: perms not granted.");
                }
                return;
            }

            default:
                return;
        }
    }

    protected void handleLocationUpdate(Location newLoc) {
        Log.i(TAG, "handleLocationUpdate: reached");
        // Remove current marker if exists, so that markers don't stack up
//        if (mCurrMarker != null) {
//            mCurrMarker.remove();
//        }

        // Get new location details
        double updatedLatitude = newLoc.getLatitude();
        double updatedLongitude = newLoc.getLongitude();
        mLatitudeDisplay.setText("Lat: " + updatedLatitude);
        mLongitudeDisplay.setText("Lng: " + updatedLongitude);
        LatLng updatedLatLng = new LatLng(updatedLatitude, updatedLongitude);

        // Send update request to server
        updateUserLocation(updatedLatitude, updatedLongitude);

//        // Build current location marker
//        MarkerOptions markerOptions = new MarkerOptions();
//        markerOptions.position(updatedLatLng);
//        markerOptions.title("Current Position");
//        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));

        //Place current location marker
//        mCurrMarker = mMap.addMarker(markerOptions);

        //move map camera only at start
        if (mLastLocation == null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(updatedLatLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15.5f));
        }

        // update the value of the last known location
        mLastLocation = newLoc;
    }

    // OnClick method for "Find nearby Friends" button
    public void findNearby(View v) {
        // mLastLocation is null iff GPS is disabled/super slow. By this point, permissions should have already been granted
        if (mLastLocation != null) {
            // clear all markers when executing new request
            mMap.clear();
            // re-add the target markers and circle
            addTargetMarker(mTargetLatLng);
            addTargetCircle(mTargetLatLng);

            // execute request to obtain friends of user, then loop over them to obtain locations
            // and set markers on map.
            mFriendsReference
                    .child(mCurrUserUid)
                    .orderByValue()
                    .equalTo(FLAG_USER_FRIEND)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot friendUid : dataSnapshot.getChildren()) {

                                mLocationsReference.child(friendUid.getKey())
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                LocData userLocData = dataSnapshot.getValue(LocData.class);
                                                Log.i(TAG, "mLocRef/onDataChange: Adding " + userLocData.name);
                                                if (Haversine.distance(mTargetLatLng.latitude, mTargetLatLng.longitude,
                                                        userLocData.latitude,userLocData.longitude) <= search_radius) {
                                                    addFriendMarker(userLocData);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Log.i(TAG, "findNearby/mLocRef/onCancelled: " + databaseError.getMessage());
                                            }
                                        });

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.i(TAG, "findNearby/mFriendsRef/onCancelled: " + databaseError.getMessage());
                        }
                    });
        } else {
            // checking if the cause is a disabled GPS or some other location related setting
            createLocationRequest();
            Log.i(TAG, "findNearby: please switch the GPS on");
        }
    }

    public void addFriendMarker(LocData user) {
        Log.i(TAG, "addMarker: " + user.toString());
        double latitude = user.getLatitude();
        double longitude = user.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(user.getName());
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        mMap.addMarker(markerOptions);
    }

    public void addTargetMarker(LatLng latLng) {
        // Remove marker if exists, so that markers don't stack up
        if (mTargetMarker != null) {
            mTargetMarker.remove();
        }

        // add marker
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        mTargetMarker = mMap.addMarker(markerOptions);
    }

    public void addTargetCircle (LatLng latLng) {
        // clear circle if exists
        if (mTargetCircle != null) {
            mTargetCircle.remove();
        }

        // add circle to show search range
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(mTargetLatLng);
        circleOptions.radius(search_radius * 1000); // in meters
        circleOptions.strokeColor(0xFF00E5FF);
        circleOptions.fillColor(0x3018FFFF);
        circleOptions.strokeWidth(2); // in pixels
        mTargetCircle = mMap.addCircle(circleOptions);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "onMapReady: assigning map");
        // assign the returned GoogleMap object to mMap.
        // Map manipulations can now be safely performed.
        mMap = googleMap;
        // still need to add this if check because AndroidStudio demands it
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnMapClickListener(MainActivity.this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        mTargetLatLng = latLng;
        addTargetMarker(latLng);
        addTargetCircle(latLng);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CONTACT_PICKER_REQUEST: {
                if (resultCode == RESULT_OK) {
                    List<ContactResult> results = MultiContactPicker.obtainResult(data);
                    for (ContactResult contact : results) {
                        Log.i(TAG, "onActivityResult: user selected " + contact.getDisplayName());
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Log.i(TAG, "onActivityResult: No contacts selected");
                }
            }
        }
    }

    public void displayFriendsScreen(MenuItem item) {
//        Toast.makeText(this, "Contacts button clicked.", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, FriendsCenterActivity.class));
    }

    public void toggleDevMode(MenuItem item) {
        // inverse because isChecked returns current value, not toggled
        if (!item.isChecked()) {
            // enabling dev mode
            item.setChecked(true);
            mIdDisplay.setVisibility(View.VISIBLE);
            mLatitudeDisplay.setVisibility(View.VISIBLE);
            mLongitudeDisplay.setVisibility(View.VISIBLE);
        } else {
            // disabling dev mode
            item.setChecked(false);
            mIdDisplay.setVisibility(View.GONE);
            mLatitudeDisplay.setVisibility(View.GONE);
            mLongitudeDisplay.setVisibility(View.GONE);
        }
    }

    public void signOut(MenuItem item) {
        AuthUI.getInstance()
                .signOut(MainActivity.this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        startActivity(new Intent(MainActivity.this,LoginActivity.class));
                        finish();
                    }
                });
    }

    public void showAccountDetails(MenuItem item) {
        String msg = mCurrUserName + "\n"
                + mCurrUserEmail;

        new LovelyInfoDialog(this)
                .setTopColorRes(R.color.colorPrimaryDark)
                .setTopTitle("Account Details")
                .setTopTitleColor(Color.WHITE)
                .setConfirmButtonText("OK")
                .setMessage(msg)
                .show();
    }

    public void getUserInfo() {
        if (mAuth.getCurrentUser().getDisplayName() != null)
            mCurrUserName = mAuth.getCurrentUser().getDisplayName();
        mCurrUserEmail = mAuth.getCurrentUser().getEmail();
        mCurrUserUid = mAuth.getCurrentUser().getUid();
    }

    private void updateUserLocation(double lat, double lon) {
        DatabaseReference userRef = mLocationsReference.child(mCurrUserUid);
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("latitude", lat);
        userUpdates.put("longitude", lon);
        userUpdates.put("timestamp", ServerValue.TIMESTAMP);
        userRef.updateChildren(userUpdates);
    }

    private void addUserIfNotExists() {
        // send single value event at expected node, check if snapshot is empty
        mLocationsReference.child(mCurrUserUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // if user doesn't exist, add basic info
                if (!dataSnapshot.exists()) {
                    LocData temp = new LocData(mCurrUserName, -1, -1);
                    mLocationsReference.child(mCurrUserUid).setValue(temp);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i(TAG, "onCancelled: " + databaseError.getMessage());
            }
        });
    }
}
