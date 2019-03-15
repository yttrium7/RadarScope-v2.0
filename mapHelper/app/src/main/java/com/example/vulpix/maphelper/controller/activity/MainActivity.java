package com.example.vulpix.maphelper.controller.activity;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;

import com.example.vulpix.maphelper.R;
import com.example.vulpix.maphelper.fragment.AccountFragment;
import com.example.vulpix.maphelper.fragment.ContactsFragment;
import com.example.vulpix.maphelper.fragment.MainFragment;
import com.example.vulpix.maphelper.fragment.MessageFragment;
import com.example.vulpix.maphelper.model.ChatMessage;
import com.example.vulpix.maphelper.service.TrackingByGPS;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

/**
 * MainActivity handles the GPS location requirements
 * messages sending to firebase function
 * check user logging status
 * sign out function
 * link to add new friends activity
 *
 * besides, it loads 4 fragments at bottom
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Initialize Firebase Auth
    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();
    private String uid;

    // bottom tab layout
    private String[] tabText = {"Main", "Message", "Contacts", "Account"};
    private int[] normalIcon = {R.drawable.tab_main_normal, R.drawable.tab_message_normal, R.drawable.tab_contacts_normal, R.drawable.tab_account_normal};
    private int[] selectIcon = {R.drawable.tab_main_pressed, R.drawable.tab_message_pressed, R.drawable.tab_contacts_pressed, R.drawable.tab_account_pressed};
    private List<Fragment> fragments;
    private com.next.easynavigition.view.EasyNavigitionBar navigitionBar;

    // firebase child
    public static final String MESSAGES_CHILD = "Messages";
    public static final String FRIEND_REQ_CHILD = "FriendRequest";
    public static final String REQ_LIST_CHILD = "requestList";
    public static final String USER_NAME_CHILD = "user_name";
    public static final String USER_STATUS_CHILD = "user_status";
    public static final String USER_IMAGE_CHILD = "user_image";
    public static final String USER_PHONE_CHILD = "user_phone";
    public static final String USER_ADDRESS_CHILD = "user_address";
    public static final String USER_DEVICE = "device_token";
    public static final String UNREAD_CHAT_CHILD = "unreadChats";
    public static final String UNREAD_REQ_CHILD = "unreadRequests";
    public static final String CHAT_CHILD = "Chats";
    public static final String CHAT_READ_STATUS = "readStatus";
    public static final String CHAT_LIST_CHILD = "chatList";
    public static final String LAST_MSG_CHILD = "LastMessage";
    public static final String LAST_MSG_TIME_CHILD = "LastMessageTime";
    public static final String USER_CHILD = "Users";
    public static final String FRIEND_CHILD = "Friends";
    public static final String ROUTE_CHILD = "Route";
    public static final String ROUTE_CHECK_POINT_CHILD = "RouteCheckPoints";
    public static final String SHARING_LOCATION_CHILD = "SharedLocation";
    public static final String PARTICIPANTS_CHILD = "Participant";
    public static final String DESTINATION_CHILD = "Destination";
    public static final String DESTINATION_NAME_CHILD = "DestinationName";
    public static final String SHARING_LOCATION_BEHAVIOR_CHILD = "Behavior";
    public static final String NOTIFICATION_CHILD = "Notifications";
    public static final String TRACKING_CHILD = "Track";
    public static final String DATE_CHILD = "LastOnlineDate";
    public static final String LATITUDE_CHILD = "Latitude";
    public static final String LONGITUDE_CHILD = "Longitude";

    // Initialize Firebase ref
    private DatabaseReference friendRequestRef;
    public static final DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference().child(MESSAGES_CHILD);
    public static final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference().child(CHAT_CHILD);
    public static final DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference().child(NOTIFICATION_CHILD);


    // date variables
    public static final DateFormat df = DateFormat.getDateTimeInstance();
    public static final Date date = new Date();
    private static final int REQUEST_SIGN_IN = 123;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginFirebase();
        if(uid!=null){
            loadTabbar();
            loadUnreadNotification();
            startGPS();
        }
    }

    protected void loginFirebase(){

        // User hasn't sign in
        if(mFirebaseUser == null) {
            //get to the login/sign up page when user logout or do not log in
            Intent startPageIntent = new Intent(MainActivity.this, StartPageActivity.class);
            startPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(startPageIntent);

        } else {
            uid = mFirebaseUser.getUid();
            // User is already signed in. Therefore, display a welcome Toast
            Toast.makeText(this,
                    "Welcome" ,
                    Toast.LENGTH_LONG)
                    .show();
            if (mFirebaseUser.getPhotoUrl() != null) {
                String mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }
    }

    private void loadTabbar(){

        fragments = new ArrayList<>();

        navigitionBar = findViewById(R.id.navigitionBar);

        fragments.add(new MainFragment());
        fragments.add(new MessageFragment());
        fragments.add(new ContactsFragment());
        fragments.add(new AccountFragment());

        navigitionBar.titleItems(tabText)
                .normalIconItems(normalIcon)
                .selectIconItems(selectIcon)
                .fragmentList(fragments)
                .fragmentManager(getSupportFragmentManager())
                .build();

    }

    private void loadUnreadNotification(){

        // load unread chat
        chatRef.child(uid).child(UNREAD_CHAT_CHILD).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    int unreadReq = Integer.parseInt(dataSnapshot.getValue().toString());
                    navigitionBar.setMsgPointCount(1, unreadReq);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // load unread req
        friendRequestRef = FirebaseDatabase.getInstance().getReference().child(FRIEND_REQ_CHILD);
        friendRequestRef.child(uid).child(UNREAD_REQ_CHILD).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    int unreadReq = Integer.parseInt(dataSnapshot.getValue().toString());
                    navigitionBar.setMsgPointCount(2, unreadReq);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            switch (requestCode){
                case REQUEST_SIGN_IN:
                    Toast.makeText(this,
                            "Successfully signed in. Welcome!",
                            Toast.LENGTH_LONG)
                            .show();
            }
        }else{
            Toast.makeText(this,
                    "Cannot Sign in",
                    Toast.LENGTH_LONG)
                    .show();

            // close app
            finish();
        }
    }

    // load main menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top_menu, menu);
        return true;
    }

    // sign out activity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invite_menu:
                Intent intent = new Intent(MainActivity.this, SearchFriendActivity.class);
                startActivity(intent);
                return true;
            case R.id.sign_out_menu:
                showSignOutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setActionBarTitle(String title) {
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);
    }

    private void showSignOutDialog(){
        AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setIcon(R.drawable.exit);
        normalDialog.setTitle("Sign Out");
        normalDialog.setMessage("Do you really want to sign out?");
        normalDialog.setPositiveButton("Confirm",
                (dialog, which) -> {
                    AuthUI.getInstance().signOut(this)
                            .addOnCompleteListener(task -> {
                                Toast.makeText(MainActivity.this,
                                        "You have been signed out.",
                                        Toast.LENGTH_LONG)
                                        .show();

                                //get to the login/sign up page when user logout or do not log in
                                Intent startPageIntent = new Intent(MainActivity.this, StartPageActivity.class);
                                startPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(startPageIntent);
                            });
                });
        normalDialog.setNegativeButton("Cancel",
                (dialog, which) -> {
                    //...To-do
                });
        normalDialog.show();
    }

    public static void updateMessageToFirebase(ChatMessage msg, String senderId, String receiverId, String lastMsg){

        String key = ShareLocationActivity.hash(senderId, receiverId);
        String msgKey = messagesRef.child(key).push().getKey();
        messagesRef.child(key).child(msgKey).setValue(msg);

        // update chat to firebase
        chatRef.child(senderId).child(CHAT_LIST_CHILD).child(receiverId).child(LAST_MSG_CHILD).setValue(lastMsg);
        chatRef.child(senderId).child(CHAT_LIST_CHILD).child(receiverId).child(LAST_MSG_TIME_CHILD).setValue(df.format(date));
        chatRef.child(senderId).child(CHAT_LIST_CHILD).child(receiverId).child(CHAT_READ_STATUS).setValue("read");

        chatRef.child(receiverId).child(CHAT_LIST_CHILD).child(senderId).child(LAST_MSG_CHILD).setValue(lastMsg);
        chatRef.child(receiverId).child(CHAT_LIST_CHILD).child(senderId).child(LAST_MSG_TIME_CHILD).setValue(df.format(date));
        chatRef.child(receiverId).child(CHAT_LIST_CHILD).child(senderId).child(CHAT_READ_STATUS).setValue("unread");

        // update receiver unread chat count
        chatRef.child(receiverId).child(UNREAD_CHAT_CHILD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int unreadChat = Integer.parseInt(dataSnapshot.getValue().toString());
                chatRef.child(receiverId).child(UNREAD_CHAT_CHILD).setValue(unreadChat+1);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public static void sendNotification(String senderId, String receiverId){

        //push the notification to users who receive th request
        HashMap<String, String> notificationData
                = new HashMap<String, String>();
        notificationData.put("from", senderId);
        notificationData.put("type", "request");

        //push data into firebase
        notificationRef.child(receiverId).push()
                .setValue(notificationData)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        Timber.d("SendNotification Successfully");
                    }
                });
    }

    private void startGPS(){
        Intent serviceIntent = new Intent(this, TrackingByGPS.class);
        this.startService(serviceIntent);
        Toast.makeText(this,"GPS enabled", Toast.LENGTH_SHORT).show();
    }
}
