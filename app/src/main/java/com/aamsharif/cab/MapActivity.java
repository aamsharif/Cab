package com.aamsharif.cab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public abstract class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private static final String TAG = "MapActivity";

    private short userType;

    protected GoogleMap mMap;
    private GoogleApiClient googleApiClient;

    protected final String databaseUrl = "https://cabapp-a0b1b-default-rtdb.asia-southeast1.firebasedatabase.app/";

    protected FirebaseAuth mAuth;
    protected String userID;

    protected boolean logOutClicked; // default value is false

    protected Button callButton;
    private TextView txtName;
    private TextView txtPhone;
    private TextView txtCarName;
    private CircleImageView profilePic;
    protected RelativeLayout connectedUserInfoLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        userType = getIntent().getShortExtra(User.TYPE, User.RIDER);

        callButton = findViewById(R.id.call_button);
        txtName = findViewById(R.id.info_name);
        txtPhone = findViewById(R.id.info_phone);
        txtCarName = findViewById(R.id.info_car_name);
        profilePic = findViewById(R.id.info_image);
        connectedUserInfoLayout = findViewById(R.id.info);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildGoogleApiClient();
    }

    @Override
    protected void onPause() {
        super.onPause();
        googleApiClient.disconnect();
    }

    @Override
    protected void onStop() {
        if (logOutClicked)
            mAuth.signOut();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Permission is already granted when the following line gets executed
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "onConnectionSuspended: GoogleApiClient's connection to Google Maps API is suspended with " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: " + connectionResult.getErrorMessage()
                + " Error Code: " + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // for permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Dexter.withContext(this)
                        .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        .withListener(new BaseMultiplePermissionsListener() {
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                                if (multiplePermissionsReport.areAllPermissionsGranted()) { // permissions granted
                                    mMap.setMyLocationEnabled(true);
                                } else { // permissions denied
                                    Toast.makeText(MapActivity.this,
                                            "These permissions are required to locate your current position in the map",
                                            Toast.LENGTH_SHORT).show();
                                    logout();
                                }
                            }
                        })
                        .onSameThread()
                        .check();
            } else { // permission is already granted
                mMap.setMyLocationEnabled(true);
            }
        } else { // for versions prior M
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.settings_option){
            Intent settings = new Intent(MapActivity.this, SettingsActivity.class);
            settings.putExtra(User.TYPE, userType);
            startActivity(settings);
        } else if(id == R.id.logout_option)
            logout();
        return true;
    }

    private void logout() {
        logOutClicked = true;
        Intent loginRegister = new Intent(MapActivity.this, LoginRegisterActivity.class);
        loginRegister.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        loginRegister.putExtra(User.TYPE, userType);
        startActivity(loginRegister); // onPause() and then onStop() is getting called
        finish();
    }

    protected synchronized void buildGoogleApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    protected ValueEventListener showConnectedUserInfo(){
        connectedUserInfoLayout.setVisibility(View.VISIBLE);
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0) {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();
                    txtName.setText(name);
                    txtPhone.setText(phone);

                    if(dataSnapshot.hasChild("car number")){ // in case connected user is a driver
                        txtCarName.setVisibility(View.VISIBLE);
                        String car = dataSnapshot.child("car number").getValue().toString();
                        txtCarName.setText(car);
                    } else
                        txtCarName.setVisibility(View.GONE);

                    if (dataSnapshot.hasChild("image")) { // if user has a profile pic
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profilePic);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "onCancelled: ", databaseError.toException());
            }
        };
    }
}