package com.example.vulpix.maphelper.controller.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.example.vulpix.maphelper.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.CHAT_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.FRIEND_REQ_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.UNREAD_CHAT_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.UNREAD_REQ_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_ADDRESS_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_DEVICE;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_IMAGE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_PHONE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_STATUS_CHILD;


/**
 * Handle the sign up activity
 * use can sign up for a new account
 * and get a root in firebase database
 **/
public class SignUpActivity extends AppCompatActivity implements Viewer {

    //firebase setting
    private FirebaseAuth mAuth;
    private DatabaseReference storeUserDataRef;
    private DatabaseReference chatRef;
    private DatabaseReference friendRequestRef;

    //loading bar when sign in
    private ProgressDialog loadingBar;

    //get user information
    private EditText signUserName;
    private EditText signUserEmail;
    private EditText signUserPsw;
    private com.gc.materialdesign.views.ButtonRectangle createAccBtn;
    private TestPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // initialize firebase
        mAuth = FirebaseAuth.getInstance();
        chatRef = FirebaseDatabase.getInstance().getReference().child(CHAT_CHILD);
        friendRequestRef = FirebaseDatabase.getInstance().getReference().child(FRIEND_REQ_CHILD);

        //get new user information
        signUserName = findViewById(R.id.signName);
        signUserEmail = findViewById(R.id.signEmail);
        signUserPsw = findViewById(R.id.signPsw);
        createAccBtn = findViewById(R.id.createAcc);
        loadingBar = new ProgressDialog(this);
        presenter = new TestPresenter(this);
        //create a new account into firebase
        createAccBtn.setOnClickListener(v -> {

            final String name  = signUserName.getText().toString();
            String email = signUserEmail.getText().toString();
            String psw = signUserPsw.getText().toString();

            //create account for user
            createNewAccount (name, email, psw);
        });
    }

    //check user information validation and put into firebse
    private void createNewAccount (String name, String email, String psw){

        //check if the information is empty
        if(TextUtils.isEmpty(name)|| TextUtils.isEmpty(email)|| TextUtils.isEmpty(psw)){

            Toast.makeText(SignUpActivity.this, "please enter your information",
                                                                   Toast.LENGTH_LONG).show();
        }
        //create a new account
        else{

            //message when creating a new account for user
            loadingBar.setTitle("creating a new Account");
            loadingBar.setMessage("please wait for seconds, we are creating a new account for you.");
            loadingBar.show();

            //create accout into firebase
            mAuth.createUserWithEmailAndPassword(email,psw)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){

                            //store reference to firebase
                            String current_user_id = mAuth.getCurrentUser().getUid();

                            //get the current device
                            String DeviceToken = FirebaseInstanceId.getInstance().getToken();

                            //to the firebase database root
                            storeUserDataRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD).child(current_user_id);

                            // set user info
                            storeUserDataRef.child(USER_NAME_CHILD).setValue(name);
                            storeUserDataRef.child(USER_STATUS_CHILD).setValue("Online");
                            storeUserDataRef.child(USER_PHONE_CHILD).setValue("empty");
                            storeUserDataRef.child(USER_ADDRESS_CHILD).setValue("empty");
                            storeUserDataRef.child(USER_DEVICE).setValue(DeviceToken);

                            // set user chat info
                            chatRef.child(current_user_id).child(UNREAD_CHAT_CHILD).setValue(0);
                            friendRequestRef.child(current_user_id).child(UNREAD_REQ_CHILD).setValue(0);

                            storeUserDataRef.child(USER_IMAGE_CHILD).setValue("ic_launcher_round")
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            //go to the main page
                                            Intent mainIntent = new Intent(SignUpActivity.this, MainActivity.class);
                                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(mainIntent);
                                            finish();
                                        }
                                    });
                        }

                        else{
                            //error message
                            Toast.makeText(SignUpActivity.this, "Name or Password too short, please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        //dismiss loading bar
                        loadingBar.dismiss();
                    });
        }
    }

    public void onSignUpClicked(Viewer view) {
        presenter.onSignUpClicked();
    }

    @Override
    public String getEmail() {
        return signUserEmail.getText().toString();
    }


    @Override
    public String getPsw() {
        return signUserPsw.getText().toString();
    }
    @Override
    public String getName() {
        return signUserName.getText().toString();
    }

}
