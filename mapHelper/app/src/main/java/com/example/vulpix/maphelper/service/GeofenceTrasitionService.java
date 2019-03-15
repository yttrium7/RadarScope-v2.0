package com.example.vulpix.maphelper.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.example.vulpix.maphelper.controller.activity.MainActivity;
import com.example.vulpix.maphelper.controller.activity.ShareLocationActivity;
import com.example.vulpix.maphelper.model.ChatMessage;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.CHAT_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.MESSAGES_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_IMAGE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;

/**
 * Service to check if the user entering the set geofences in NavWithSafeMessage activity
 * geofence is used to check if the user is on the right route or not
 * if he/she is, send the safe message to chosen receiver
 */
public class GeofenceTrasitionService extends IntentService {

    private static final String TAG = GeofenceTrasitionService.class.getSimpleName();

    // Firebase variables
    private String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private DatabaseReference userRef;
    private DatabaseReference messageRef;
    private DatabaseReference chatRef;

    private String receiver;
    private Double destination_lat;
    private Double destination_lon;
    private LatLng destinationCoord;
    private String destinationName;
    private String key;
    private String myName;
    private String myPhoto;

    public final static String AUTO_SAFE_MESSAGE = "I am on the right route :)";
    public final static String CHECK_ROUTE_MESSAGE = "   CLICK to Check my location";


    // date variables
    DateFormat df = DateFormat.getDateTimeInstance();
    Date date = new Date();

    public GeofenceTrasitionService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // Initialize Firebase reference
        userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);
        messageRef = FirebaseDatabase.getInstance().getReference().child(MESSAGES_CHILD);
        chatRef = FirebaseDatabase.getInstance().getReference().child(CHAT_CHILD);
        receiver = intent.getStringExtra("receiver");
        destinationName = intent.getStringExtra("destination");
        destination_lat = intent.getDoubleExtra("destination_lat", 1.0f);
        destination_lon = intent.getDoubleExtra("destination_lon", 1.0f);
        destinationCoord = new LatLng(destination_lat, destination_lon);
        key = ShareLocationActivity.hash(receiver, uid);

        Log.i(TAG, "startGeofenceIntent ");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        // Handling errors
        if ( geofencingEvent.hasError() ) {
            String errorMsg = getErrorString(geofencingEvent.getErrorCode() );
            Log.e( TAG, errorMsg );
            return;
        }

        int geoFenceTransition = geofencingEvent.getGeofenceTransition();

        // Check if the transition type is of interest
        if ( geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ) {
            sendSafeMessage();
        }
    }

    private void sendSafeMessage(){
        userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> userInfo = (Map<String, Object>) dataSnapshot.getValue();

                myName = userInfo.get(USER_NAME_CHILD).toString();
                myPhoto = userInfo.get(USER_IMAGE_CHILD).toString();

                // upload message to firebase
                ChatMessage msg = new ChatMessage(AUTO_SAFE_MESSAGE + CHECK_ROUTE_MESSAGE,
                        myName, uid, myPhoto, null, destinationCoord, df.format(date));
                MainActivity.updateMessageToFirebase(msg, uid, receiver, AUTO_SAFE_MESSAGE + CHECK_ROUTE_MESSAGE);
                MainActivity.sendNotification(uid, receiver);

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        Toast.makeText(this, "Safe message send successfully", Toast.LENGTH_LONG).show();
    }

    private static String getErrorString(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "GeoFence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many GeoFences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Unknown error.";
        }
    }
}
