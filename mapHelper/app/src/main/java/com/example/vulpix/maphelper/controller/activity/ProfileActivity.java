package com.example.vulpix.maphelper.controller.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.vulpix.maphelper.R;
import com.example.vulpix.maphelper.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import timber.log.Timber;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.FRIEND_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.FRIEND_REQ_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.NOTIFICATION_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.REQ_LIST_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.UNREAD_REQ_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;


/** Handle the friend activity
 * user can send/cancel/accept a request
 * also can unfriend if two users are friends already
 **/
public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    // local layout variables
    private Button sendFriendBtn;
    private Button declineReqBtn;
    private TextView profileName;

    // user status variables
    private String CURRENT_STATE;
    private final String NOT_FRIEND = "NotFriend";
    private final String REQUEST_SENT = "RequestSend";
    private final String REQUEST_RECEIVED = "RequestReceived";
    private final String FRIEND = "Friend";

    // firebase reference
    private DatabaseReference userRef;
    private DatabaseReference friendRequestRef;
    private DatabaseReference friendRef;
    private DatabaseReference notificationRef;

    //user info
    private String sender_user_id;
    private String receiver_user_id;
    private String key;
    private String myName;

    // date variables
    DateFormat df = DateFormat.getDateTimeInstance();
    Date date = new Date();

    // message variables
    public final static String SAY_HELLO_MESSAGE = "Request Accepted! We are friends now :)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // user info
        receiver_user_id = Objects.requireNonNull(Objects.requireNonNull(getIntent().
                getExtras()).get("visit_user_id")).toString();
        sender_user_id = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        key = ShareLocationActivity.hash(receiver_user_id, sender_user_id);

        // initialize firebase refs
        friendRequestRef = FirebaseDatabase.getInstance().getReference().child(FRIEND_REQ_CHILD);
        friendRef = FirebaseDatabase.getInstance().getReference().child(FRIEND_CHILD);
        userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);
        notificationRef = FirebaseDatabase.getInstance().getReference().child(NOTIFICATION_CHILD);
        notificationRef.keepSynced(true);

        // actionbar edit
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.profile_title);

        //initialize view
        sendFriendBtn = findViewById(R.id.send_req_btn);
        declineReqBtn = findViewById(R.id.decline_req_btn);
        profileName = findViewById(R.id.visit_user_name_txt);
        declineReqBtn.setVisibility(View.INVISIBLE);
        declineReqBtn.setEnabled(false);

        //record the state of two users
        CURRENT_STATE = NOT_FRIEND;

        // get userName from firebaseDB
        userRef.child(sender_user_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                myName = dataSnapshot.child(USER_NAME_CHILD).getValue().toString();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        //handle the friend request and check current state
        userRef.child(receiver_user_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String name = dataSnapshot.child(USER_NAME_CHILD).getValue().toString();
                profileName.setText(name);

                //check the state of two users
                friendRequestRef.child(sender_user_id).child(REQ_LIST_CHILD)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                //if not friends, can send/accept/cancel a request
                                if(dataSnapshot.exists()){
                                    if(dataSnapshot.hasChild(receiver_user_id)){

                                        String req_type = Objects.requireNonNull(dataSnapshot.child(receiver_user_id)
                                                .child("request_type").getValue()).toString();
                                        if(req_type.equals("sent")){

                                            //set the state and button
                                            CURRENT_STATE = REQUEST_SENT;
                                            sendFriendBtn.setText(R.string.send_request);

                                            declineReqBtn.setVisibility(View.INVISIBLE);
                                            declineReqBtn.setEnabled(false);

                                        }else if(req_type.equals("received")){

                                            //change the state and button
                                            CURRENT_STATE = REQUEST_RECEIVED;
                                            sendFriendBtn.setText(R.string.accept_request);

                                            declineReqBtn.setVisibility(View.VISIBLE);
                                            declineReqBtn.setEnabled(true);
                                            declineReqBtn.setBackground(getResources().getDrawable(R.drawable.button_with_radius_red));

                                            //if user choose to decline the friend request
                                            declineReqBtn.setOnClickListener(v -> DeclineFriReq());

                                        }
                                    }
                                }

                                //check friend and unfriend
                                else{
                                    friendRef.child(sender_user_id)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                    //change state
                                                    if(dataSnapshot.hasChild(receiver_user_id)){
                                                        CURRENT_STATE = FRIEND;
                                                        sendFriendBtn.setText(R.string.unfriend);
                                                        sendFriendBtn.setBackground(getResources().getDrawable(R.drawable.button_with_radius_red));

                                                        // set declineBtn as chat button to start chatting
                                                        declineReqBtn.setText(R.string.send_message);
                                                        declineReqBtn.setVisibility(View.VISIBLE);
                                                        declineReqBtn.setEnabled(true);

                                                        declineReqBtn.setOnClickListener(view -> {
                                                            Intent chatIntent = new Intent(getApplicationContext(), ChatActivity.class);
                                                            chatIntent.putExtra("participant", receiver_user_id);
                                                            chatIntent.putExtra("friendName", name);
                                                            startActivity(chatIntent);
                                                            finish();
                                                        });
                                                    }
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



        //set decline request button to be invisible when do not receive any request
        //and enable to be false
        declineReqBtn.setVisibility(View.INVISIBLE);
        declineReqBtn.setEnabled(false);


        //check if users send a friend request to themselves
        if(!sender_user_id.equals(receiver_user_id)){

            //if not same account and  want to send a new request to a user
            sendFriendBtn.setOnClickListener(v -> {

                //create random ref into firebase
                sendFriendBtn.setEnabled(false);

                //if click the button, check current state
                if(CURRENT_STATE.equals(NOT_FRIEND)){
                    SendFriendRequest();
                }

                if(CURRENT_STATE.equals(REQUEST_SENT)){
                    CancelFriendRequest();
                }

                if(CURRENT_STATE.equals(REQUEST_RECEIVED)){
                    AcceptFriendRequest();
                }

                if(CURRENT_STATE.equals(FRIEND)){
                    UnFriend();
                }
            });
        }else{
            //if is same as user's account, the send/decline friend request button will be invisible
            declineReqBtn.setVisibility(View.INVISIBLE);
            sendFriendBtn.setVisibility(View.INVISIBLE);
        }

    }

    //user decline the friend request
    private void DeclineFriReq() {

        //find the request
        friendRequestRef.child(sender_user_id).child(REQ_LIST_CHILD).child(receiver_user_id)
                .removeValue().addOnCompleteListener(task -> {

                    //remove the request from database
                    if(task.isSuccessful()){

                        updateUnreadReqValue(sender_user_id, -1);

                        friendRequestRef.child(receiver_user_id).child(REQ_LIST_CHILD).child(sender_user_id)
                                .removeValue().addOnCompleteListener(task1 -> {

                                    //if cancel request is successful, change the state and button visibility
                                    if(task1.isSuccessful()){
                                        sendFriendBtn.setEnabled(true);
                                        CURRENT_STATE = NOT_FRIEND;
                                        sendFriendBtn.setText(R.string.send_request);

                                        declineReqBtn.setVisibility(View.INVISIBLE);
                                        declineReqBtn.setEnabled(false);
                                    }
                                });
                    }
                });
    }


    //unfriend a person, remove from db and change state
    private void UnFriend() {
        friendRef.child(sender_user_id).child(receiver_user_id).removeValue()
                .addOnCompleteListener(task -> {

                    if(task.isSuccessful()){
                        friendRef.child(receiver_user_id).child(sender_user_id).removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if(task1.isSuccessful()){

                                        //set button invisible and current state
                                        sendFriendBtn.setEnabled(true);
                                        CURRENT_STATE = NOT_FRIEND;
                                        sendFriendBtn.setText(R.string.send_request);

                                        declineReqBtn.setVisibility(View.INVISIBLE);
                                        declineReqBtn.setEnabled(false);
                                    }
                                });
                    }
                });
    }

    private void AcceptFriendRequest() {


        //store friend in each user root, sender and receiver become friend at this date
        friendRef.child(sender_user_id).child(receiver_user_id).child("date").setValue(df.format(date))
                .addOnSuccessListener(aVoid ->
                        friendRef.child(receiver_user_id).child(sender_user_id).child("date")
                        .setValue(df.format(date))
                        .addOnSuccessListener(aVoid1 ->
                                friendRequestRef.child(sender_user_id).child(REQ_LIST_CHILD).child(receiver_user_id)
                                .removeValue().addOnCompleteListener(task -> {

                                    //become a friend and remove the friend request from database
                                    if(task.isSuccessful()){
                                        friendRequestRef.child(receiver_user_id).child(REQ_LIST_CHILD).child(sender_user_id)
                                                .removeValue().addOnCompleteListener(task1 -> {
                                                    if(task1.isSuccessful()){

                                                        updateUnreadReqValue(sender_user_id, -1);
                                                        sendHelloMsg();

                                                        //change the button and state
                                                        sendFriendBtn.setEnabled(true);
                                                        CURRENT_STATE = FRIEND;
                                                        sendFriendBtn.setText(R.string.unfriend);

                                                        declineReqBtn.setVisibility(View.INVISIBLE);
                                                        declineReqBtn.setEnabled(false);
                                                    }

                                                });
                                    }

                                })));

    }


    //cancel a friend request
    private void CancelFriendRequest() {

        //find the request
        friendRequestRef.child(sender_user_id).child(REQ_LIST_CHILD).child(receiver_user_id)
                .removeValue().addOnCompleteListener(task -> {

                    //remove the request from database
                 if(task.isSuccessful()){
                     friendRequestRef.child(receiver_user_id).child(REQ_LIST_CHILD).child(sender_user_id)
                             .removeValue().addOnCompleteListener(task1 -> {

                                 //if cancel request is successful, change the state and button visibility
                                 if(task1.isSuccessful()){

                                     updateUnreadReqValue(receiver_user_id, -1);

                                     sendFriendBtn.setEnabled(true);
                                     CURRENT_STATE = NOT_FRIEND;
                                     sendFriendBtn.setText(R.string.send_request);

                                     declineReqBtn.setVisibility(View.INVISIBLE);
                                     declineReqBtn.setEnabled(false);
                                 }

                             });
                 }

                });
    }




    private void SendFriendRequest() {

        //find the database reference
        friendRequestRef.child(sender_user_id).child(REQ_LIST_CHILD).child(receiver_user_id)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(task -> {

                    //if already sent a request, change the state
                    if(task.isSuccessful()){
                        friendRequestRef.child(receiver_user_id).child(REQ_LIST_CHILD).child(sender_user_id)
                                .child("request_type").setValue("received")
                                .addOnCompleteListener(task1 -> {

                                    if(task1.isSuccessful()){

                                        updateUnreadReqValue(receiver_user_id, 1);

                                        //push the notification to users who receive th request
                                        HashMap<String, String> notificationData
                                                = new HashMap<String, String>();
                                        notificationData.put("from", sender_user_id);
                                        notificationData.put("type", "request");

                                        //push data into firebase
                                        notificationRef.child(receiver_user_id).push()
                                                .setValue(notificationData)
                                                .addOnCompleteListener(task11 -> {

                                                    if(task11.isSuccessful()){

                                                        //set the button and state
                                                        sendFriendBtn.setEnabled(true);
                                                        CURRENT_STATE = REQUEST_SENT;
                                                        sendFriendBtn.setText(R.string.cancel_request);

                                                        declineReqBtn.setVisibility(View.INVISIBLE);
                                                        declineReqBtn.setEnabled(false);
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }


    private void sendHelloMsg(){
        // new chat message
        ChatMessage msg = new ChatMessage(SAY_HELLO_MESSAGE, myName, sender_user_id,
                null,
                null,
                null,
                df.format(date));

        MainActivity.updateMessageToFirebase(msg, sender_user_id, receiver_user_id, SAY_HELLO_MESSAGE);
    }

    private void updateUnreadReqValue(String userId, int count){
        // update unread request value
        friendRequestRef.child(userId).child(UNREAD_REQ_CHILD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int unreadReq = Integer.parseInt(Objects.requireNonNull(dataSnapshot.getValue()).toString());
                friendRequestRef.child(userId).child(UNREAD_REQ_CHILD).setValue(unreadReq+count);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
