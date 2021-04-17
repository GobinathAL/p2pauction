package com.network.p2pauction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class JoinActivity extends createActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        P2PHandler();
        P2PInfoReceiver();
        service();
    }
    private void service() {
        groupFormation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), GroupJoin.class);
                startActivity(intent);
            }
        });
    }
}