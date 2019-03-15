
package com.example.vulpix.maphelper.controller.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.vulpix.maphelper.R;
import com.example.vulpix.maphelper.model.AllUsers;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.FRIEND_REQ_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.REQ_LIST_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;

/**
 * Loading the friend-requests that you have
 */

public class MyRequestActivity extends AppCompatActivity {

    // local layout variables
    private ListView myRequestList;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private TextView noFriReq;

    // Firebase message reference variables
    private DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);

    // Firebase user variables
    private FirebaseAuth mFirebaseAuth;
    private String current_uid;
    private DatabaseReference reqRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_request);

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = findViewById(R.id.my_request_progressBar);
        myRequestList = findViewById(R.id.my_request_list);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(false);

        // textView layout
        noFriReq = findViewById(R.id.no_friend_request);

        // action bar edit
        getSupportActionBar().setTitle("My friend request");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Intialize Firebase user
        mFirebaseAuth = FirebaseAuth.getInstance();
        current_uid = mFirebaseAuth.getCurrentUser().getUid();
        reqRef = FirebaseDatabase.getInstance().getReference().child(FRIEND_REQ_CHILD);


        FirebaseListOptions<AllUsers> options = new FirebaseListOptions.Builder<AllUsers>()
                .setLayout(R.layout.item_user)
                .setQuery(reqRef.child(current_uid).child(REQ_LIST_CHILD),AllUsers.class)
                .build();

        FirebaseListAdapter<AllUsers> adapter = new FirebaseListAdapter<AllUsers>(options) {
            @Override
            protected void populateView(View v, AllUsers model, int position) {

                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                noFriReq.setVisibility(TextView.INVISIBLE);
                //find a friend and get information
                String visit_user_id = getRef(position).getKey();

                assert visit_user_id != null;
                userRef.child(visit_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        String friendName = Objects.requireNonNull(dataSnapshot.child("user_name").getValue()).toString();
                        ((TextView) v.findViewById(R.id.userName)).setText(friendName);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                v.setOnClickListener(view -> {
                    //todo: add profile intent here
                    Intent profileIntent = new Intent(MyRequestActivity.this,ProfileActivity.class);
                    profileIntent.putExtra("visit_user_id",visit_user_id);
                    startActivity(profileIntent);
                });
            }
        };


        if(mProgressBar.getVisibility() == View.VISIBLE){
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            noFriReq.setVisibility(TextView.VISIBLE);
        }

        adapter.startListening();
        myRequestList.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}

