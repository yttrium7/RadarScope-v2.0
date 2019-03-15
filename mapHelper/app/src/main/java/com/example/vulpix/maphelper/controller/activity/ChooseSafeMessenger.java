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

import com.example.vulpix.maphelper.R;
import com.example.vulpix.maphelper.model.Friends;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.FRIEND_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;

/**
 * Choose a message receiver when you choose to send the safe message to that particular person
 * once clicked that person, you can get into self navigation as normal
 */

public class ChooseSafeMessenger extends AppCompatActivity {

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

        // action bar edit
        Objects.requireNonNull(getSupportActionBar()).setTitle("Choose a message receiver");

        // Initialize Firebase user
        mFirebaseAuth = FirebaseAuth.getInstance();
        uid = Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getUid();

        // Initialize Firebase reference
        userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);
        friRef = FirebaseDatabase.getInstance().getReference().child(FRIEND_CHILD).child(uid);

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

                assert friendId != null;
                userRef.child(friendId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        String friendName = Objects.requireNonNull(dataSnapshot.child(USER_NAME_CHILD).getValue()).toString();
                        ((TextView) v.findViewById(R.id.userName)).setText(friendName);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                v.setOnClickListener(view -> {
                    // starting shareLocation activity by tracking on GPS
                    Intent intent = new Intent(ChooseSafeMessenger.this, NavWithSafeMessage.class);
                    intent.putExtra("receiver",friendId);
                    intent.putExtra("sender",uid);
                    startActivity(intent);
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
}
