package com.network.p2pauction;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AuctionCatalogue extends AppCompatActivity {
    MaterialButton next;
    TextInputEditText input1, input2, input3;
    public static String AUCTION_NAME;
    public static String AUCTION_CATALOGUE;
    public static Integer AUCTION_DURATION;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auction_catalogue);
        input1 = (TextInputEditText) findViewById(R.id.input1);
        input2 = (TextInputEditText) findViewById(R.id.input2);
        input3 = (TextInputEditText) findViewById(R.id.input3);
        next = (MaterialButton) findViewById(R.id.nextbtn);
        next.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), createActivity.class);
                AUCTION_NAME = String.valueOf(input1.getText());
                AUCTION_CATALOGUE = String.valueOf(input2.getText());
                AUCTION_DURATION = Integer.parseInt(String.valueOf(input3.getText()));
                Log.i("check", String.valueOf(input1.getText()));
                Log.i("check", String.valueOf(input2.getText()));
                Integer i1 = Integer.parseInt(String.valueOf(input3.getText()));
                Log.i("check", i1.toString());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}