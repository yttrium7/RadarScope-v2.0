package com.example.vulpix.maphelper.controller.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View;

import com.example.vulpix.maphelper.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_DEVICE;


/** Handle the login activity **/
public class LoginActivity extends AppCompatActivity implements Viewer{

    //connect to firebase
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    //loading bar message when user login
    private ProgressDialog loadingBar;

    //user information
    private EditText loginEmail;
    private EditText loginPsw;
    private TestPresenter presenter;


    private com.gc.materialdesign.views.ButtonRectangle loginBtn;


    public LoginActivity() {

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //get firebase instance
        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);

        //get new user information
        loginEmail = findViewById(R.id.loginEmail);
        loginPsw = findViewById(R.id.loginPsw);
        loginBtn = findViewById(R.id.logAcc);
        loadingBar = new ProgressDialog(this);
        presenter = new TestPresenter(this);

        loginBtn.setOnClickListener(v -> {
            String email = loginEmail.getText().toString();
            String psw = loginPsw.getText().toString();

            //user login
            LoginUserAcc(email, psw);

        });
    }


    //check information and login
    public void LoginUserAcc(String email, String psw) {
        //check if the information is empty
        if(TextUtils.isEmpty(email)|| TextUtils.isEmpty(psw)){

            Toast.makeText(LoginActivity.this, "please enter your information",
                    Toast.LENGTH_LONG).show();
        }
        //login
        else{

            //give message to user when logging in
            loadingBar.setTitle("Logging in your Account");
            loadingBar.setMessage("please wait for seconds, logging...");
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email, psw).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){

                        //get user id and device
                        String online_user_id = mAuth.getCurrentUser().getUid();
                        String DeviceToken = FirebaseInstanceId.getInstance().getToken();

                        //store token into firebase
                        userRef.child(online_user_id).child(USER_DEVICE).setValue(DeviceToken)
                                .addOnSuccessListener(aVoid -> {

                                    //go to the main page when finishing log in
                                    Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(mainIntent);
                                    finish();

                                });

                    }else{
                        //show error message
                        Toast.makeText(LoginActivity.this, "please check your information",
                                Toast.LENGTH_LONG).show();
                    }
                    //dismiss loading bar when complete the log in process
                    loadingBar.dismiss();
                }
            });
        }
    }



    public void onLoginClicked(Viewer view) {
        presenter.onLoginClicked();
    }

    @Override
    public String getEmail() {
        return loginEmail.getText().toString();
    }


    @Override
    public String getPsw() {
        return loginPsw.getText().toString();
    }

    @Override
    public String getName() {
        return null;
    }

}
