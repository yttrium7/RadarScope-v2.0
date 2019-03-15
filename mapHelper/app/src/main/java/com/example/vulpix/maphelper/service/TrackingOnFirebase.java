package com.example.vulpix.maphelper.service;

import com.example.vulpix.maphelper.controller.activity.ShareLocationActivity;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.DATE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.DESTINATION_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.DESTINATION_NAME_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.LATITUDE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.LONGITUDE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.PARTICIPANTS_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.SHARING_LOCATION_BEHAVIOR_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.SHARING_LOCATION_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_IMAGE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_STATUS_CHILD;

/**
 * Update the current data to the firebase to enable the share location activity
 * the data includes:
 * user destination, user & participant's user information(status and identity), last online date
 */
public class TrackingOnFirebase extends Service {

    private static final String TAG = TrackingOnFirebase.class.getSimpleName();

    //Firebase variables
    private DatabaseReference shareLocationRef;
    private DatabaseReference userRef;
    private String uid;

    // User id
    private String senderId;
    private String receiverId;
    private String key;
    String senderName;
    String senderImg;
    String receiverName;
    String receiverImg;

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {

        Timber.i(" startTrackingOnFirebaseIntent ");

        senderId = Objects.requireNonNull(intent.getExtras()).getString("sender");
        receiverId = intent.getExtras().getString("receiver");
        assert receiverId != null;
        key = ShareLocationActivity.hash(senderId, receiverId);
        initializeFirebase();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initializeFirebase(){

        // initialize Firebase variables
        shareLocationRef = FirebaseDatabase.getInstance().getReference().child(SHARING_LOCATION_CHILD);
        userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);
        uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // get sender's name
        userRef.child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> userInfo = (Map<String, Object>) dataSnapshot.getValue();
                assert userInfo != null;
                senderName = Objects.requireNonNull(userInfo.get(USER_NAME_CHILD)).toString();
                senderImg = Objects.requireNonNull(userInfo.get(USER_IMAGE_CHILD)).toString();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        // get receiver's name
        userRef.child(receiverId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> userInfo = (Map<String, Object>) dataSnapshot.getValue();
                assert userInfo != null;
                receiverName = Objects.requireNonNull(userInfo.get(USER_NAME_CHILD)).toString();
                receiverImg = Objects.requireNonNull(userInfo.get(USER_IMAGE_CHILD)).toString();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        // update destination on Firebase
        shareLocationRef.child(key).child("Updates").addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                DateFormat df = DateFormat.getDateTimeInstance();
                Date date = new Date();

                shareLocationRef.child(key).child(SHARING_LOCATION_BEHAVIOR_CHILD).child("setRoute").setValue(false);
                shareLocationRef.child(key).child(SHARING_LOCATION_BEHAVIOR_CHILD).child("setLiveStream").setValue(false);

                shareLocationRef.child(key).child(DESTINATION_CHILD).child(DESTINATION_NAME_CHILD).setValue("empty");
                shareLocationRef.child(key).child(DESTINATION_CHILD).child(LATITUDE_CHILD).setValue(0);
                shareLocationRef.child(key).child(DESTINATION_CHILD).child(LONGITUDE_CHILD).setValue(0);

                shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(senderId).child(USER_NAME_CHILD).setValue(senderName);
                shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(senderId).child(USER_IMAGE_CHILD).setValue(senderImg);
                shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(senderId).child(USER_STATUS_CHILD).setValue(false);
                shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(senderId).child(DATE_CHILD).setValue(df.format(date));
                shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(senderId).child("Identity").setValue("sender");

                shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(receiverId).child(USER_NAME_CHILD).setValue(receiverName);
                shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(receiverId).child(USER_IMAGE_CHILD).setValue(receiverImg);
                shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(receiverId).child(USER_STATUS_CHILD).setValue(false);
                shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(receiverId).child(DATE_CHILD).setValue(df.format(date));
                shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(receiverId).child("Identity").setValue("receiver");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }
}
