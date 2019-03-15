package com.example.vulpix.maphelper.controller.activity;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.example.vulpix.maphelper.R;


/**
 * Start page for users if they did not log in
 * can sign up if do not have an account
 * login if already have an account
 **/
public class StartPageActivity extends AppCompatActivity {

    private Button NewAccountBtn;
    private Button ExistAccountBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_page);

        //create and log in button
        NewAccountBtn = findViewById(R.id.sign);
        ExistAccountBtn = findViewById(R.id.login);

        //if do not have account, go to the sign up page
        NewAccountBtn.setOnClickListener(v -> {
            //go to sign up page if user do not have account
            Intent signIntent = new Intent(StartPageActivity.this, SignUpActivity.class);
            startActivity(signIntent);
        });

        //if have account, log in
        ExistAccountBtn.setOnClickListener(v -> {

            //go to the login
            Intent loginIntent = new Intent(StartPageActivity.this, LoginActivity.class);
            startActivity(loginIntent);
        });
    }
}
