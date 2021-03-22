package com.network.p2pauction;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class JoinActivity extends createActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        P2PHandler();
        P2PInfoReceiver();
    }
}