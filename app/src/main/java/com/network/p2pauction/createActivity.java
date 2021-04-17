package com.network.p2pauction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class createActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener {
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    static BroadcastReceiver receiver;
    IntentFilter intentFilter;
    Button btnDiscover, groupFormation;
    private boolean retryChannel = false;
    public ListView peerListView;
    List<WifiP2pDevice> peerList = new ArrayList<WifiP2pDevice>();
    ArrayList<String> deviceNameArray;
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_P2pauction);
        setContentView(R.layout.activity_main);
        P2PHandler();
        P2PInfoReceiver();
        service();
    }
    protected void P2PHandler() {
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new BReceiver(manager, channel, this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        deviceNameArray = new ArrayList<String>();
        btnDiscover = (MaterialButton) findViewById(R.id.btnDiscover);
        groupFormation = (MaterialButton) findViewById(R.id.Group);
        peerListView = (ListView) findViewById(R.id.peerList);
        textView = (TextView) findViewById(R.id.textField);
    }
    protected void P2PInfoReceiver() {
        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        textView.setText("Discovered Peers");
                        Toast.makeText(getApplicationContext(), "Peers Discovered",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(getApplicationContext(), "Cannot be initiated. Give required permissions", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    private void service() {
        groupFormation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), GroupFormation.class);
                startActivity(intent);
            }
        });
    }
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            Log.i("CONN", "Peers available, updating device list");
            peerList.clear();
            peerList.addAll(peers.getDeviceList());
            deviceNameArray.clear();
            for (WifiP2pDevice e : peerList) {
                if(e.deviceName.toLowerCase().contains(getResources().getString(R.string.invalid))) continue;
                Log.i("CONN",e.toString());
                deviceNameArray.add(e.deviceName);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text = (TextView) view.findViewById(android.R.id.text1);
                    text.setTextColor(Color.parseColor("#000000"));
                    return view;
                }
            };
            peerListView.setAdapter(adapter);
        }
    };
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress groupOwnerAddress = info.groupOwnerAddress;
            Log.i("module2", "Groupinfo " + info.groupFormed + " " + info.isGroupOwner);
        }
    };
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }


    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            // resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost permanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }
}