package com.example.vulpix.maphelper.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.util.Date;

import timber.log.Timber;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.DATE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.LATITUDE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.LONGITUDE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.TRACKING_CHILD;

/**
 * Asking for the permission from android to access the user's location
 * and upload the location to the firebase
 */

public class TrackingByGPS extends Service
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        ResultCallback<Status> {

    private static final String TAG = "TrackingByGPS";
    private GoogleApiClient googleApiClient;

    private Location lastLocation;

    //Firebase variables
    private DatabaseReference locationRef;
    private String uid;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        this.uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.locationRef = FirebaseDatabase.getInstance().getReference().child(TRACKING_CHILD);
        createGoogleApi();
        googleApiClient.connect();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Create GoogleApiClient instance
    private void createGoogleApi() {
        Timber.tag(TAG).d("createGoogleApi()");
        if ( googleApiClient == null ) {
            googleApiClient = new GoogleApiClient.Builder( this )
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .addApi( LocationServices.API )
                    .build();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Timber.d("onConnect()");
        getLastKnownLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Timber.w("onConnectionSuspended()");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Timber.w("onConnectionFailed()");
    }

    @Override
    public void onResult(@NonNull Status status) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Timber.tag(TAG).d("onLocationChanged [" + location + "]");
        lastLocation = location;
        writeActualLocation(location);
    }

    private final int REQ_PERMISSION = 999;

    // Check for permission to access Location
    private boolean checkPermission() {
        Timber.tag(TAG).d("checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }

    // Start location Updates
    private void startLocationUpdates(){
        Timber.tag(TAG).i("startLocationUpdates()");
        // Defined in mili seconds.
        // This number in extremely low, and should be used only for debug

        int UPDATE_INTERVAL = 1000;
        int FASTEST_INTERVAL = 900;
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if ( checkPermission() )
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    // Get last known location
    private void getLastKnownLocation() {
        Timber.tag(TAG).d("getLastKnownLocation()");
        if ( checkPermission() ) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if ( lastLocation != null ) {
                writeLastLocation();
                startLocationUpdates();
            } else {
                Timber.w("No location retrieved yet");
                startLocationUpdates();
            }
        }
        else {
            Timber.tag(TAG).e("Location not permitted");

        }
    }

    private void writeActualLocation(Location location) {

        locationRef.child(uid).child("Updates")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        DateFormat df = DateFormat.getDateTimeInstance();
                        Date date = new Date();
                        locationRef.child(uid).child(LATITUDE_CHILD).setValue(location.getLatitude());
                        locationRef.child(uid).child(LONGITUDE_CHILD).setValue(location.getLongitude());
                        locationRef.child(uid).child(DATE_CHILD).setValue(df.format(date));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
    }

    private void writeLastLocation() {
        writeActualLocation(lastLocation);
    }
}
