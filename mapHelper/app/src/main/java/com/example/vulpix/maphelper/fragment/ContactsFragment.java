package com.example.vulpix.maphelper.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.vulpix.maphelper.R;
import com.example.vulpix.maphelper.controller.activity.SearchFriendActivity;
import com.example.vulpix.maphelper.model.Friends;
import com.example.vulpix.maphelper.controller.activity.MainActivity;
import com.example.vulpix.maphelper.controller.activity.MyRequestActivity;
import com.example.vulpix.maphelper.controller.activity.ProfileActivity;
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
import static com.example.vulpix.maphelper.controller.activity.MainActivity.FRIEND_REQ_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.UNREAD_REQ_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;

/**
 * One of 4 main fragments - Contacts fragment
 * showing the user's current friends list, new friend requests
 * once click the friend, showing the friend profile
 */
public class ContactsFragment extends Fragment {

    private static final String TAG = "ContactsFragment";
    // local layout view
    private ListView myFriendList;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private Button friendReqBtn;
    private TextView noFriend;
    private ImageButton addFriend;
    private ImageView newReq;

    // Firebase user variables
    private FirebaseAuth mFirebaseAuth;
    private String uid;
    private DatabaseReference friRef;
    private DatabaseReference userRef;
    private DatabaseReference friendRequestRef;

    private int unreadReq;

    protected View mView;
    protected Context mContext;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContext = getActivity();
        mView = inflater.inflate(R.layout.fragment_contacts, container, false);
        friendReqBtn = mView.findViewById(R.id.new_friend_request);

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = mView.findViewById(R.id.progressBar);
        myFriendList = mView.findViewById(R.id.my_friend_list);
        mLinearLayoutManager = new LinearLayoutManager(mContext);
        mLinearLayoutManager.setStackFromEnd(false);

        // textView layout
        noFriend = mView.findViewById(R.id.no_friend);
        addFriend = mView.findViewById(R.id.add_friend_image);
        newReq = mView.findViewById(R.id.new_request);

        // Intialize Firebase user
        mFirebaseAuth = FirebaseAuth.getInstance();
        uid = Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);
        friRef = FirebaseDatabase.getInstance().getReference().child(FRIEND_CHILD).child(uid);

        initUnreadNotification();
        initView();
        initListener();

        return mView;
    }

    private void initUnreadNotification(){

        friendRequestRef = FirebaseDatabase.getInstance().getReference().child(FRIEND_REQ_CHILD);
        friendRequestRef.child(uid).child(UNREAD_REQ_CHILD).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    unreadReq = Integer.parseInt(dataSnapshot.getValue().toString());
                    friendReqBtn.setText("You have  " + unreadReq + " new friend requests");
                    if(unreadReq > 0){
                        newReq.setVisibility(ImageView.VISIBLE);
                    }else{
                        newReq.setVisibility(ImageView.INVISIBLE);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void initView(){

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
                String visit_user_id = getRef(position).getKey();

                assert visit_user_id != null;
                userRef.child(visit_user_id).addValueEventListener(new ValueEventListener() {
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
                    Intent profileIntent = new Intent(mContext,ProfileActivity.class);
                    profileIntent.putExtra("visit_user_id",visit_user_id);
                    startActivity(profileIntent);
                });
            }
        };
        adapter.startListening();
        myFriendList.setAdapter(adapter);

        // set the progress bar and button's visibility depending on the existence of data
        if(mProgressBar.getVisibility() == ProgressBar.VISIBLE){
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            noFriend.setVisibility(TextView.VISIBLE);
            addFriend.setVisibility(TextView.VISIBLE);
            addFriend.setClickable(true);
            addFriend.setOnClickListener(v -> {
                Intent intent = new Intent(mContext, SearchFriendActivity.class);
                startActivity(intent);
            });
        }
    }

    private void initListener(){

        friendReqBtn.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, MyRequestActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) Objects.requireNonNull(getActivity()))
                .setActionBarTitle("Contacts List");
    }

}


