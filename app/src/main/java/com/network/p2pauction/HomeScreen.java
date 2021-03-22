package com.network.p2pauction;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

public class HomeScreen extends AppCompatActivity {

    MaterialButton createButton, joinButton;
    MaterialTextView about;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        createButton = (MaterialButton) findViewById(R.id.createButton);
        joinButton = (MaterialButton) findViewById(R.id.joinButton);
        about = (MaterialTextView) findViewById(R.id.about);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), createActivity.class);
                startActivity(intent);
            }
        });
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), JoinActivity.class);
                startActivity(intent);
            }
        });
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AboutActivity.class);
                startActivity(intent);
            }
        });
    }
}