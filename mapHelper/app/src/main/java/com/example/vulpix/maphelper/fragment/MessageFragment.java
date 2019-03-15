package com.example.vulpix.maphelper.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vulpix.maphelper.R;
import com.example.vulpix.maphelper.controller.activity.ChatActivity;
import com.example.vulpix.maphelper.model.ChatSelectItem;
import com.example.vulpix.maphelper.controller.activity.MainActivity;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;
import java.util.Objects;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.CHAT_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.CHAT_LIST_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.CHAT_READ_STATUS;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;

/**
 * One of 4 main fragments - Message fragment
 * showing the user's recent chat with different chat
 * choose one of theses chat to enter the chat activity with that friend
 */

public class MessageFragment extends Fragment{

    // view holder class
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView chatName;
        TextView chatLastMsg;
        TextView chatLastTime;
        ImageView newMessage;

        public ChatViewHolder(View v) {
            super(v);
            chatName = itemView.findViewById(R.id.chat_name);
            chatLastMsg = itemView.findViewById(R.id.chat_last_msg);
            chatLastTime = itemView.findViewById(R.id.chat_last_msg_time);
            newMessage = itemView.findViewById(R.id.new_message);
        }
    }

    // fragment variables
    protected View mView;
    protected Context mContext;
    private ProgressBar mProgressBar;
    private TextView noChat;

    // firebase variables
    private FirebaseUser mFirebaseUser;
    private String uid;
    private String friendId;
    private String friendName;
    private DatabaseReference chatRef;
    private DatabaseReference userRef;

    // Firebase adapter
    private FirebaseRecyclerAdapter<ChatSelectItem, ChatViewHolder> mFirebaseAdapter;

    // local variables
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // initial frames
        mContext = getActivity();
        mView = inflater.inflate(R.layout.fragment_message, container, false);
        mProgressBar = mView.findViewById(R.id.progressBar);
        mRecyclerView = mView.findViewById(R.id.lv_contacts);
        mLinearLayoutManager = new LinearLayoutManager(mContext);
        mLinearLayoutManager.setStackFromEnd(false);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        // textView layout
        noChat = mView.findViewById(R.id.no_chat);

        // initial firebase
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        assert mFirebaseUser != null;
        uid = mFirebaseUser.getUid();
        chatRef = FirebaseDatabase.getInstance().getReference().child(CHAT_CHILD);
        userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);

        initView();
        return mView;
    }

    private void initView(){

        SnapshotParser<ChatSelectItem> parser = dataSnapshot -> {
            ChatSelectItem chat = dataSnapshot.getValue(ChatSelectItem.class);
            if (chat != null) {
                chat.setId(dataSnapshot.getKey());
            }
            return chat;
        };

        // built message recycler list
        FirebaseRecyclerOptions<ChatSelectItem> options =
                new FirebaseRecyclerOptions.Builder<ChatSelectItem>()
                        .setQuery(chatRef.child(uid).child(CHAT_LIST_CHILD), parser)
                        .build();

        // set adapter
        mFirebaseAdapter = new FirebaseRecyclerAdapter<ChatSelectItem, ChatViewHolder>(options) {

            @NonNull
            @Override
            public ChatViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new ChatViewHolder(inflater.inflate(R.layout.item_chat, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(@NonNull final ChatViewHolder viewHolder, int position,
                                            @NonNull ChatSelectItem chat) {

                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                noChat.setVisibility(View.INVISIBLE);

                viewHolder.chatLastMsg.setText(chat.getLastMessage());
                viewHolder.chatLastTime.setText(chat.getLastMessageTime());
                if(chat.getReadStatus().equals("unread")){
                    viewHolder.newMessage.setVisibility(ImageView.VISIBLE);
                }else{
                    viewHolder.newMessage.setVisibility(ImageView.INVISIBLE);
                }

                friendId = chat.getId();
                userRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Map<String, Object> userInfo = (Map<String, Object>) dataSnapshot.getValue();
                        assert userInfo != null;
                        friendName = Objects.requireNonNull(userInfo.get(USER_NAME_CHILD)).toString();
                        viewHolder.chatName.setText(friendName);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        };

        if(mProgressBar.getVisibility() == View.VISIBLE){
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            noChat.setVisibility(TextView.VISIBLE);
        }

        // scroll screen
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
                    mRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mRecyclerView.setAdapter(mFirebaseAdapter);

        // set item click event
        class RecyclerTouchListener implements RecyclerView.OnItemTouchListener{

            private ClickListener clicklistener;
            private GestureDetector gestureDetector;

            public RecyclerTouchListener(Context context, final RecyclerView recycleView, final ClickListener clicklistener){

                this.clicklistener=clicklistener;
                gestureDetector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        View child=recycleView.findChildViewUnder(e.getX(),e.getY());
                        if(child!=null && clicklistener!=null){
                            clicklistener.onLongClick(child,recycleView.getChildAdapterPosition(child));
                        }
                    }
                });
            }

            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                View child=rv.findChildViewUnder(e.getX(),e.getY());
                if(child!=null && clicklistener!=null && gestureDetector.onTouchEvent(e)){
                    clicklistener.onClick(child,rv.getChildAdapterPosition(child));
                }

                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        }

        // set on click event listener
        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(),
                mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                Toast.makeText(getContext(), "onClick press on position :"+position,
                        Toast.LENGTH_LONG).show();
                String participant = mFirebaseAdapter.getItem(position).getId();
                userRef.child(participant).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Map<String, Object> userInfo = (Map<String, Object>) dataSnapshot.getValue();
                        assert userInfo != null;
                        friendName = Objects.requireNonNull(userInfo.get(USER_NAME_CHILD)).toString();

                        chatRef.child(uid).child(CHAT_LIST_CHILD).child(participant).child(CHAT_READ_STATUS).setValue("read");
                        Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                        chatIntent.putExtra("participant", participant);
                        chatIntent.putExtra("friendName", friendName);
                        startActivity(chatIntent);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onLongClick(View view, int position) {
                Toast.makeText(getContext(), "Long press on position :"+position,
                        Toast.LENGTH_LONG).show();
            }
        }));

    }


    public interface ClickListener{
        void onClick(View view, int position);
        void onLongClick(View view, int position);
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
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.child(uid).hasChild(CHAT_LIST_CHILD)) {
                    mFirebaseAdapter.startListening();
                }else{
                    noChat.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
