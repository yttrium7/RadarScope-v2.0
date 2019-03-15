package com.example.vulpix.maphelper.controller.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.vulpix.maphelper.model.ChatMessage;
import com.example.vulpix.maphelper.R;
import com.example.vulpix.maphelper.model.Friends;
import com.example.vulpix.maphelper.service.TrackingOnFirebase;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.FRIEND_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.NOTIFICATION_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_IMAGE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;

/**
 * Ask a friend for help, showing the friend list
 * and once choose a friend, send the notification to that friend
 * says that you need his/her help
 */
public class AskForHelp extends AppCompatActivity {
    private static final String TAG = "AskForHelp";
    // local layout view
    private ListView myFriendList;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private TextView noFriend;
    private ImageButton addFriend;

    // Firebase variables
    private FirebaseAuth mFirebaseAuth;
    private String uid;
    private DatabaseReference friRef;
    private DatabaseReference userRef;
    private DatabaseReference notificationRef;

    // message variables
    public final static String ASK_FOR_HELP_MSG = "Please Help me to find the way :)";
    private String myName;
    private String myPhoto;

    // date variables
    DateFormat df = DateFormat.getDateTimeInstance();
    Date date = new Date();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_friend);

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = findViewById(R.id.progressBar);
        myFriendList = findViewById(R.id.my_friend_list);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(false);

        // textView layout
        noFriend = findViewById(R.id.no_friend);
        addFriend = findViewById(R.id.add_friend_image);

        //action bar edit
        Objects.requireNonNull(getSupportActionBar()).setTitle("Choose a friend for help");

        // Initialize Firebase user
        mFirebaseAuth = FirebaseAuth.getInstance();
        uid = Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getUid();

        // Initialize Firebase reference
        userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);
        friRef = FirebaseDatabase.getInstance().getReference().child(FRIEND_CHILD).child(uid);
        notificationRef = FirebaseDatabase.getInstance().getReference().child(NOTIFICATION_CHILD);
        notificationRef.keepSynced(true);

        // set the list view with adapter
        FirebaseListOptions<Friends> options = new FirebaseListOptions.Builder<Friends>()
                .setLayout(R.layout.item_user)
                .setQuery(friRef,Friends.class)
                .build();

        FirebaseListAdapter<Friends> adapter = new FirebaseListAdapter<Friends>(options) {
            @Override
            protected void populateView(View v, Friends model, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                noFriend.setVisibility(TextView.INVISIBLE);
                addFriend.setVisibility(TextView.INVISIBLE);
                addFriend.setClickable(false);
                //set the friend date
                ((TextView) v.findViewById(R.id.userStatus)).setText(model.getDate());

                //find a friend and get information
                String friendId = getRef(position).getKey();

                //todo: add name into the friends db
                userRef.child(friendId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        String friendName = dataSnapshot.child(USER_NAME_CHILD).getValue().toString();
                        ((TextView) v.findViewById(R.id.userName)).setText(friendName);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                v.setOnClickListener(view -> {

                    // get the key(which is the unique identifier for 2 users
                    startService(friendId);
                    sendNotification(friendId);
                    startSharingLocation(friendId);
                });
            }
        };

        adapter.startListening();
        myFriendList.setAdapter(adapter);

        if(mProgressBar.getVisibility() == View.VISIBLE){
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            noFriend.setVisibility(TextView.VISIBLE);
            addFriend.setVisibility(TextView.VISIBLE);
            addFriend.setClickable(true);
            addFriend.setOnClickListener(v -> {
                Intent intent = new Intent(this, SearchFriendActivity.class);
                startActivity(intent);
            });
        }

    }

    public void startService(String friendId){

        Intent serviceIntent = new Intent(this, TrackingOnFirebase.class);
        serviceIntent.putExtra("sender",uid);
        serviceIntent.putExtra("receiver",friendId);
        this.startService(serviceIntent);
    }

    private void sendNotification(String friendId){

        // get sender's name
        userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> userInfo = (Map<String, Object>) dataSnapshot.getValue();
                myName = Objects.requireNonNull(userInfo.get(USER_NAME_CHILD)).toString();
                myPhoto = Objects.requireNonNull(userInfo.get(USER_IMAGE_CHILD)).toString();

                // update chat to firebase
                ChatMessage msg = new ChatMessage(ASK_FOR_HELP_MSG, myName, uid, myPhoto, null, null,df.format(date));

                MainActivity.updateMessageToFirebase(msg, uid, friendId, ASK_FOR_HELP_MSG);
                MainActivity.sendNotification(uid, friendId);

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void startSharingLocation(String friendId) {

        Intent intent = new Intent(getApplicationContext(), ShareLocationActivity.class);
        intent.putExtra("sender", uid);
        intent.putExtra("receiver", friendId);
        startActivity(intent);
    }
}
