package com.example.vulpix.maphelper.controller.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vulpix.maphelper.R;
import com.example.vulpix.maphelper.model.AllUsers;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.database.Query;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;

/**
 * Search friend and get into friend's profile activity
 */
public class SearchFriendActivity extends AppCompatActivity {
    private static final String TAG = "SearchFriendActivity";

    private ListView allUserList;
    private ProgressBar mProgressBar;
    private EditText searchUserName;
    private com.gc.materialdesign.views.ButtonRectangle searchUserBtn;

    // Firebase message reference variables
    private DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // load layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_friend);

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
        allUserList = findViewById(R.id.all_user_list);
        searchUserBtn = findViewById(R.id.search_user);
        searchUserName = findViewById(R.id.enter_name);

        // action bar edit
        getSupportActionBar().setTitle("Search User");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        searchUserBtn.setOnClickListener(v -> {
            //send what friend need to search
            String newFriend = searchUserName.getText().toString();

            //check input
            if (TextUtils.isEmpty(newFriend)) {
                Toast.makeText(SearchFriendActivity.this, "please enter your friend's account",
                        Toast.LENGTH_LONG).show();
            }
            searchUser(newFriend);
        });
    }

    private void searchUser(String newFriend) {

        Query findNewFriend = userRef.orderByChild(USER_NAME_CHILD)
                .startAt(newFriend).endAt(newFriend+"\uf8ff");

        FirebaseListOptions<AllUsers> options = new FirebaseListOptions.Builder<AllUsers>()
                .setLayout(R.layout.item_user)
                .setQuery(findNewFriend, AllUsers.class)
                .build();

        FirebaseListAdapter<AllUsers> adapter = new FirebaseListAdapter<AllUsers>(options) {
            @Override
            protected void populateView(View v, AllUsers model, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                ((TextView) v.findViewById(R.id.userName)).setText(model.getUser_name());
                ((TextView) v.findViewById(R.id.userStatus)).setText(model.getUser_status());

                v.setOnClickListener(view -> {
                    //find a friend and get information
                    String visit_user_id = getRef(position).getKey();
                    Intent profileIntent = new Intent(SearchFriendActivity.this,ProfileActivity.class);
                    profileIntent.putExtra("visit_user_id",visit_user_id);
                    startActivity(profileIntent);
                });
            }
        };

        adapter.startListening();
        allUserList.setAdapter(adapter);
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
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


