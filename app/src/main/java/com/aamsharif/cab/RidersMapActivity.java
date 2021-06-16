package com.aamsharif.cab;

import androidx.annotation.NonNull;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RidersMapActivity extends MapActivity {
    private static final String TAG = "RidersMapActivity";

    private Location currentLocation;
    private LatLng pickUpLocation;
    private boolean requested; // Default value is false

    private Marker riderMarker;

    private int radius;
    private boolean nearbyDriverFound; // Default value is false
    private String nearbyDriverID;
    private GeoQuery geoQuery;

    private DatabaseReference requestedRidersReference;
    private DatabaseReference driversReference;
    private DatabaseReference availableDriversReference;

    private Marker driverMarker;
    private ValueEventListener workingDriversLocationChangeListener;
    private DatabaseReference driversWorkingReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestedRidersReference = FirebaseDatabase.getInstance(databaseUrl).getReference().child("Requested Riders");
        driversReference = FirebaseDatabase.getInstance(databaseUrl).getReference().child("Users").child("Drivers");
        availableDriversReference = FirebaseDatabase.getInstance(databaseUrl).getReference().child("Available Drivers");
        driversWorkingReference = FirebaseDatabase.getInstance(databaseUrl).getReference().child("Drivers Working");

        callButton.setOnClickListener(view -> {
            if(!requested){
                requested = true;

                GeoFire geoFire = new GeoFire(requestedRidersReference);
                geoFire.setLocation(userID, new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()));

                pickUpLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                riderMarker = mMap.addMarker(new MarkerOptions()
                        .position(pickUpLocation)
                        .title("You are here")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                callButton.setText("Getting your driver...");
                radius = 1; // initializing radius to search for a nearby driver within 1 units of radius
                getNearbyDriver();
            } else { // reverting all the operations
                requested = false;
                geoQuery.removeAllListeners();

                if(workingDriversLocationChangeListener != null)
                    driversWorkingReference.child(nearbyDriverID)
                            .child("l")
                            .removeEventListener(workingDriversLocationChangeListener);

                if(nearbyDriverFound){
                    driversReference.child(nearbyDriverID)
                            .child("Requested Rider's ID")
                            .removeValue();
                    nearbyDriverID = null;
                    nearbyDriverFound = false;
                }
                radius = 1;

                GeoFire geoFire = new GeoFire(requestedRidersReference);
                geoFire.removeLocation(userID);

                if(riderMarker != null)
                    riderMarker.remove();

                if(driverMarker != null)
                    driverMarker.remove();

                callButton.setText("Call a Cab");
                connectedUserInfoLayout.setVisibility(View.GONE);
            }
        });
    }

    private void getNearbyDriver() {
        GeoFire geoFire = new GeoFire(availableDriversReference);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickUpLocation.latitude, pickUpLocation.longitude), radius);

        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() { // addGeoQueryEventListener() also fires an event after adding
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!nearbyDriverFound && requested){
                    nearbyDriverID = key;
                    nearbyDriverFound = true;

                    // for notifying drivers about the request
                    Map<String, Object> request = new HashMap<>();
                    request.put("Requested Rider's ID", userID);
                    driversReference.child(nearbyDriverID)
                            .updateChildren(request);

                    // showing drivers location on the map
                    getDriverLocation();
                    callButton.setText("Locating nearby driver...");
                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!nearbyDriverFound){
                    radius++;
                    getNearbyDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e(TAG, "onGeoQueryError: ", error.toException());
            }
        });
    }

    private void getDriverLocation() {
        workingDriversLocationChangeListener =  driversWorkingReference.child(nearbyDriverID)
                .child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists() && requested){
                            List<Object> driverLocationAsList = (List<Object>) snapshot.getValue();

                            callButton.setText("Driver Found");
                            showConnectedUserInfo();

                            double latitude;
                            double longitude;
                            if(driverLocationAsList.get(0) != null)
                                latitude = Double.parseDouble(driverLocationAsList.get(0).toString());

                            if(driverLocationAsList.get(1) != null)
                                longitude = Double.parseDouble(driverLocationAsList.get(1).toString());

                            LatLng driversLatLng = new LatLng(latitude, longitude);
                            if(driverMarker != null)
                                driverMarker.remove();

                            Location ridersLocation = new Location(""); // custom empty string is a valid provider
                            ridersLocation.setLatitude(pickUpLocation.latitude);
                            ridersLocation.setLongitude(pickUpLocation.longitude);

                            Location driversLocation = new Location(""); // custom empty string is a valid provider
                            driversLocation.setLatitude(driversLatLng.latitude);
                            driversLocation.setLongitude(driversLatLng.longitude);

                            float distance = ridersLocation.distanceTo(driversLocation);
                            if(distance < 50)
                                callButton.setText("Driver Arrived");
                            else
                                callButton.setText("Driver Found: " + distance);

                            driverMarker = mMap.addMarker(new MarkerOptions()
                                    .position(driversLatLng)
                                    .title("Your driver is here")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "onCancelled: ", error.toException());
                    }
                });
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        super.onLocationChanged(location);
        currentLocation = location;
    }

    @Override
    protected ValueEventListener showConnectedUserInfo() {
        return driversReference.child(nearbyDriverID)
                .addValueEventListener(super.showConnectedUserInfo());
    }
}