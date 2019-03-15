package com.example.vulpix.maphelper.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.vulpix.maphelper.R;
import com.example.vulpix.maphelper.controller.activity.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

import timber.log.Timber;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_ADDRESS_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_PHONE_CHILD;

/**
 * One of 4 main fragments - Account fragment
 * showing the user's name, address, phone
 * with edit function
 */
public class AccountFragment extends Fragment {
    private static final String TAG = "AccountFragment";

    protected View mView;
    protected Context mContext;
    private TextView myName;
    private TextView myId;
    private String uid;

    private String name;
    private String phone;
    private String address;

    private ImageButton editAddress;
    private ImageButton editPhone;

    private TextView addressView;
    private TextView phoneView;

    private String updateValue;
    private EditText inputText;

    // Initialize Firebase Auth
    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private DatabaseReference userRef;

    public final static String EDIT_ADDRESS = "editAddress";
    public final static String EDIT_PHONE = "editPhone";


    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        mView = inflater.inflate(R.layout.fragment_account, container, false);

        //initial frame
        editAddress = mView.findViewById(R.id.edit_address);
        editPhone = mView.findViewById(R.id.edit_phone);
        addressView = mView.findViewById(R.id.address_view);
        phoneView = mView.findViewById(R.id.phone_view);
        myName = mView.findViewById(R.id.display_name);
        myId = mView.findViewById(R.id.user_id);


        //initialize firebase
        uid = Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);


        //initialize view
        myId.setText("ID: " + uid);
        userRef.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                name = Objects.requireNonNull(dataSnapshot.child(USER_NAME_CHILD).getValue()).toString();
                try {
                    phone = Objects.requireNonNull(dataSnapshot.child(USER_PHONE_CHILD).getValue()).toString();
                    address = Objects.requireNonNull(dataSnapshot.child(USER_ADDRESS_CHILD).getValue()).toString();
                }catch (NullPointerException e){
                    Timber.tag(TAG).e("no phone and address_edit value");
                }
                myName.setText(name);
                addressView.setText(address);
                phoneView.setText(phone);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        editAddress.setOnClickListener(v -> {
            showEditDialog(EDIT_ADDRESS);
        });
        editPhone.setOnClickListener(v -> {
            showEditDialog(EDIT_PHONE);
        });

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) Objects.requireNonNull(getActivity()))
                .setActionBarTitle("RadarScope");
    }

    private void showEditDialog(String check){

        View dialog_view = LayoutInflater.from(mContext).inflate(R.layout.dialog_edit, null);
        inputText = dialog_view.findViewById(R.id.new_value);
        TextView title = dialog_view.findViewById(R.id.title);
        if(check.equals(EDIT_PHONE)){
            title.setText(R.string.type_phone);
        }

        AlertDialog.Builder builder =
                new AlertDialog.Builder(mContext);
        builder.setView(dialog_view);
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            updateToFirebase(check);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> { });
        builder.show();
    }

    private void updateToFirebase(String check){

        updateValue  = inputText.getText().toString();
        if(check.equals(EDIT_ADDRESS)){
            userRef.child(uid).child(USER_ADDRESS_CHILD).setValue(updateValue);

        }else{
            userRef.child(uid).child(USER_PHONE_CHILD).setValue(updateValue);
        }
    }
}