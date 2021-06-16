package com.aamsharif.cab;

import androidx.annotation.NonNull;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class DriversMapActivity extends MapActivity {
    private static final String TAG = "DriversMapActivity";

    private String riderID ;

    private Marker riderPickUpLocationMarker;
    private DatabaseReference requestedRidersLocationReference;
    private ValueEventListener requestedRidersLocationChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callButton.setVisibility(View.GONE);
        getRidersRequestAssigned();
    }

    private void getRidersRequestAssigned() {
        DatabaseReference assignedRidersRef = FirebaseDatabase.getInstance(databaseUrl).getReference().child("Users").child("Drivers")
                .child(userID).child("Requested Rider's ID");
        assignedRidersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    riderID = snapshot.getValue().toString();
                    getRidersPickUpLocation();
                    showConnectedUserInfo();
                } else {
                    riderID = null;

                    if(riderPickUpLocationMarker != null)
                        riderPickUpLocationMarker.remove();

                    if(requestedRidersLocationChangeListener != null)
                        requestedRidersLocationReference.removeEventListener(requestedRidersLocationChangeListener);

                    connectedUserInfoLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: ", error.toException());
            }
        });
    }

    private void getRidersPickUpLocation() {
        requestedRidersLocationReference = FirebaseDatabase.getInstance(databaseUrl).getReference().child("Requested Riders")
                .child(riderID).child("l");

        requestedRidersLocationChangeListener = requestedRidersLocationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    List<Object> riderLocationAsList = (List<Object>) snapshot.getValue();
                    double latitude;
                    double longitude;
                    if (riderLocationAsList.get(0) != null)
                        latitude = Double.parseDouble(riderLocationAsList.get(0).toString());

                    if (riderLocationAsList.get(1) != null)
                        longitude = Double.parseDouble(riderLocationAsList.get(1).toString());

                    LatLng riderLatLng = new LatLng(latitude, longitude);
                    riderPickUpLocationMarker = mMap.addMarker(new MarkerOptions().position(riderLatLng).title("Pick me up")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: ", error.toException());
            }
        });
    }

    @Override
    protected void onStop() {
        removeDriversAvailability();
        super.onStop();
    }

    private void removeDriversAvailability() {
        DatabaseReference availableDriversRef = FirebaseDatabase.getInstance(databaseUrl).getReference().child("Available Drivers");
        GeoFire geoFire = new GeoFire(availableDriversRef);
        geoFire.removeLocation(userID);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        super.onLocationChanged(location);
        addDriversAvailability(location);
    }

    private void addDriversAvailability(@NonNull Location location) {
        DatabaseReference availableDriversRef = FirebaseDatabase.getInstance(databaseUrl).getReference().child("Available Drivers");
        GeoFire availableDrivers = new GeoFire(availableDriversRef);

        DatabaseReference driversWorkingRef = FirebaseDatabase.getInstance(databaseUrl).getReference().child("Drivers Working");
        GeoFire driversWorking = new GeoFire(driversWorkingRef);

        if(riderID == null){
            driversWorking.removeLocation(userID);
            availableDrivers.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
        } else {
            availableDrivers.removeLocation(userID);
            driversWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
        }
    }

    @Override
    protected ValueEventListener showConnectedUserInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance(databaseUrl).getReference()
                .child("Users").child("Riders").child(riderID);
        return reference.addValueEventListener(super.showConnectedUserInfo());
    }
}