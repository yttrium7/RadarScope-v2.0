package com.example.vulpix.maphelper.controller.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vulpix.maphelper.model.ChatMessage;
import com.example.vulpix.maphelper.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.text.DateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;
import timber.log.Timber;

import static com.example.vulpix.maphelper.controller.activity.AskForHelp.ASK_FOR_HELP_MSG;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.CHAT_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.MESSAGES_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.UNREAD_CHAT_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;
import static com.example.vulpix.maphelper.service.GeofenceTrasitionService.CHECK_ROUTE_MESSAGE;

/**
 * Loading the current messages sent between you and your friend
 * including normal text message, ASK FOR HELP message, CHECK LOCATION message
 * if participant's the message is not text message, then it can be clicked
 * ASK FOR HELP message links to the ShareLocationActivity
 * CHECK LOCATION message links to the TrackActivity
 */
public class ChatActivity extends AppCompatActivity {

    // item-message view holder initialize
    public class MessageViewHolder extends RecyclerView.ViewHolder {

        TextView messageText, senderText;
        CircleImageView profileImage, locationProfile;
        Button locationReceiverText, locationSenderText;
        TextView messageTime, senderTime, locationReceiverTime, locationSenderTime;
        TextView messageFrom, locationReceiverFrom;
        public RelativeLayout relativeLayout;

        MessageViewHolder(View view) {
            super(view);

            messageText = view.findViewById(R.id.message_text_layout);
            messageFrom = view.findViewById(R.id.message_from);
            profileImage = view.findViewById(R.id.message_profile_layout);
            messageTime = view.findViewById(R.id.message_time_layout);

            senderText = view.findViewById(R.id.sender_text_layout);
            senderTime = view.findViewById(R.id.sender_time_layout);

            locationReceiverText = view.findViewById(R.id.location_receiver_text_layout);
            locationReceiverTime = view.findViewById(R.id.location_receiver_time_layout);
            locationProfile = view.findViewById(R.id.location_receiver_profile_layout);
            locationReceiverFrom = view.findViewById(R.id.location_receiver_from);

            locationSenderText = view.findViewById(R.id.location_sender_text_layout);
            locationSenderTime = view.findViewById(R.id.location_sender_time_layout);

            relativeLayout = view.findViewById(R.id.message_single_layout);
        }
    }

    private static final String TAG = "ChatActivity";

    public static final int DEFAULT_MSG_LENGTH_LIMIT = 100;

    // date variables
    DateFormat df = DateFormat.getDateTimeInstance();
    Date date = new Date();

    // local view layout
    private SharedPreferences mSharedPreferences;
    private ImageButton mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;

    // local memu view
    private TextView mLastSeenView;
    private TextView mTitleView;

    // Firebase AUTH variables
    private String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private String participant;
    private String myName;
    private String friendName;
    private String key;
    private String mPhotoUrl = "empty";

    // track variables
    private String lastText;
    private LatLng destination;

    // Firebase message reference variables
    private DatabaseReference messagesRef;
    private DatabaseReference chatRef;
    private DatabaseReference userRef;

    // Firebase adapter
    private FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder> mFirebaseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // action bar edit
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.actionbar_chat, null);
        actionBar.setCustomView(action_bar_view);

        // local layout view
        mTitleView = findViewById(R.id.custom_bar_title);
        mLastSeenView = findViewById(R.id.custom_bar_seen);

        // get participant id
        participant = getIntent().getStringExtra("participant");
        friendName = getIntent().getStringExtra("friendName");
        key = ShareLocationActivity.hash(uid, participant);

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = findViewById(R.id.progressBar);
        mMessageRecyclerView = findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        // Initialize Firebase
        messagesRef = FirebaseDatabase.getInstance().getReference().child(MESSAGES_CHILD);
        chatRef = FirebaseDatabase.getInstance().getReference().child(CHAT_CHILD);
        userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);

        // clean the unread chat count
        chatRef.child(uid).child(UNREAD_CHAT_CHILD).setValue(0);

        // get userName from firebaseDB
        userRef.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                myName = dataSnapshot.child(USER_NAME_CHILD).getValue().toString();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        // set menu bar
        mTitleView.setText(friendName);
        mLastSeenView.setText(R.string.online);

        readMessage();
        sendMessage();
    }

    private void readMessage(){
        // --------- read message START ----------- //

        // initialize parser
        SnapshotParser<ChatMessage> parser = dataSnapshot -> {
            ChatMessage msg = dataSnapshot.getValue(ChatMessage.class);
            if (msg != null) {
                msg.setId(dataSnapshot.getKey());
            }
            return msg;
        };

        // built message recycler list
        FirebaseRecyclerOptions<ChatMessage> options =
                new FirebaseRecyclerOptions.Builder<ChatMessage>()
                        .setQuery(messagesRef.child(key), parser)
                        .build();

        // set adapter
        mFirebaseAdapter = new FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder>(options) {
            @NonNull
            @Override
            public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new MessageViewHolder(inflater.inflate(R.layout.item_single_message, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(@NonNull final MessageViewHolder viewHolder,
                                            int position,
                                            ChatMessage msg) {

                mProgressBar.setVisibility(ProgressBar.INVISIBLE);

                if(lastText == null){
                    lastText = msg.getText();
                }

                if(destination == null){
                    try{
                        destination = msg.getDestination();
                    }catch (NullPointerException e){
                        Timber.e("no destination value in chat");
                    }
                }

                if (msg.getName().equals(myName)) {

                    viewHolder.messageFrom.setVisibility(View.INVISIBLE);
                    viewHolder.messageText.setVisibility(View.INVISIBLE);
                    viewHolder.messageTime.setVisibility(View.INVISIBLE);
                    viewHolder.profileImage.setVisibility(View.INVISIBLE);

                    viewHolder.locationReceiverText.setVisibility(View.INVISIBLE);
                    viewHolder.locationReceiverTime.setVisibility(View.INVISIBLE);
                    viewHolder.locationReceiverFrom.setVisibility(View.INVISIBLE);
                    viewHolder.locationProfile.setVisibility(View.INVISIBLE);

                    if (msg.getText().equals(ASK_FOR_HELP_MSG) || msg.getText().contains(CHECK_ROUTE_MESSAGE)) {

                        viewHolder.senderText.setVisibility(View.INVISIBLE);
                        viewHolder.senderTime.setVisibility(View.INVISIBLE);

                        viewHolder.locationSenderText.setText(msg.getText());
                        viewHolder.locationSenderText.setClickable(false);
                        viewHolder.locationSenderTime.setText(msg.getMessageTime());



                    } else {

                        viewHolder.senderText.setText(msg.getText());
                        viewHolder.senderTime.setText(msg.getMessageTime());

                        viewHolder.locationSenderText.setVisibility(View.INVISIBLE);
                        viewHolder.locationSenderTime.setVisibility(View.INVISIBLE);
                    }

                } else {

                    viewHolder.senderText.setVisibility(View.INVISIBLE);
                    viewHolder.senderTime.setVisibility(View.INVISIBLE);

                    viewHolder.locationSenderText.setVisibility(View.INVISIBLE);
                    viewHolder.locationSenderTime.setVisibility(View.INVISIBLE);

                    if (msg.getText().equals(ASK_FOR_HELP_MSG) || msg.getText().contains(CHECK_ROUTE_MESSAGE)) {

                        viewHolder.messageFrom.setVisibility(View.INVISIBLE);
                        viewHolder.messageText.setVisibility(View.INVISIBLE);
                        viewHolder.messageTime.setVisibility(View.INVISIBLE);
                        viewHolder.profileImage.setVisibility(View.INVISIBLE);

                        viewHolder.locationReceiverText.setText(msg.getText());
                        viewHolder.locationReceiverTime.setText(msg.getMessageTime());
                        viewHolder.locationReceiverFrom.setText(msg.getName());

                    } else {

                        viewHolder.locationReceiverText.setVisibility(View.INVISIBLE);
                        viewHolder.locationReceiverTime.setVisibility(View.INVISIBLE);
                        viewHolder.locationReceiverFrom.setVisibility(View.INVISIBLE);
                        viewHolder.locationProfile.setVisibility(View.INVISIBLE);

                        viewHolder.messageText.setText(msg.getText());
                        viewHolder.messageFrom.setText(msg.getName());
                        viewHolder.messageTime.setText(msg.getMessageTime());
                    }

                }
            }
        };

        // load message when scroll screen
        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int msgCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (msgCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
        // ------ read message END --------- //
    }

    private void sendMessage(){

        // input message typing
        mMessageEditText = findViewById(R.id.chat_field);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt("normal_length", DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        // ----------- send message START -----------------//
        mSendButton = findViewById(R.id.chat_send_btn);
        mSendButton.setOnClickListener(view -> {

            // new chat message
            ChatMessage msg = new ChatMessage(mMessageEditText.getText().toString(), myName, uid,
                    mPhotoUrl,
                    null,
                    null,
                    df.format(date));
            MainActivity.updateMessageToFirebase(msg, uid, participant, mMessageEditText.getText().toString());

            // empty the text field
            mMessageEditText.setText("");
        });
        // --------------- send message END ------------//
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        mFirebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAdapter.startListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public void sendToMap(View view) {

        if(lastText.equals(ASK_FOR_HELP_MSG)){
            Intent mapIntent = new Intent(ChatActivity.this, ShareLocationActivity.class);
            mapIntent.putExtra("sender", participant);
            mapIntent.putExtra("receiver", uid);
            startActivity(mapIntent);
        }

        else if(lastText.contains(CHECK_ROUTE_MESSAGE)){

            if(destination == null){
                Toast.makeText(this, "Sorry, sender is not in journey", Toast.LENGTH_LONG).show();
            }else{
                Timber.tag(TAG).i("sendToMap(" + destination.toString() + ")");
                Intent trackIntent = new Intent(ChatActivity.this, TrackActivity.class);
                trackIntent.putExtra("sender", participant);
                trackIntent.putExtra("destination_lat", destination.getLatitude());
                trackIntent.putExtra("destination_lon", destination.getLongitude());
                startActivity(trackIntent);
            }
        }

    }
}