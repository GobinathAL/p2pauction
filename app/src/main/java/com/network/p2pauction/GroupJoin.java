package com.network.p2pauction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

public class GroupJoin extends AppCompatActivity {
    String AUCTION_NAME;
    String AUCTION_CATALOGUE;
    int AUCTION_DURATION;
    TextView auctionName, noOfBidders;
    ListView itemsList;
    MaterialButton btnSend;
    NsdManager.DiscoveryListener discoveryListener;
    NsdManager nsdManager;
    NsdServiceInfo mService;
    String ip, myip;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_join);
        auctionName = (TextView) findViewById(R.id.AuctionName);
        noOfBidders = (TextView) findViewById(R.id.NoOfBidders);
        itemsList = (ListView) findViewById(R.id.ItemsList);
        btnSend = (MaterialButton) findViewById(R.id.sendToSocketBtn);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        myip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        Thread myThread = new Thread(new MyServer());
        myThread.start();
        Log.i("module2","check1");
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.i("module2", "nsd start discovery failed");
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.i("module2", "nsd stop discovery failed");
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.i("module2", "nsd discovery started");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i("module2", "nsd discovery stopped");
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.i("module2", "nsd service found" + serviceInfo.getServiceName() + " " + serviceInfo.getServiceType());
                if(!serviceInfo.getServiceType().equals("_p2pauction._tcp.")) {
                    Log.i("module2", "nsd unknown service type");
                }
                else if(serviceInfo.getServiceName().contains("p2pauction")) {
                    Log.i("module2", "nsd service name: " + serviceInfo.getServiceName());
                    ip = serviceInfo.getServiceName().substring(10);
                    Log.i("module2", "ip found - " + ip);
                    nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            Log.i("module2", "nsd service resolve failed");
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            Log.i("module2", "nsd service resolved");
                            if(serviceInfo.getServiceName().contains("p2pauction192")) {
                                Log.i("module2", "Expected service");
                                mService = serviceInfo;
                                int port = mService.getPort();
                                Log.i("module2", "" + port);
                            }
                        }
                    });
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.i("module2", "nsd service lost");
            }
        };
        Log.i("module2","check2");

        Log.i("module2","check3");
        nsdManager.discoverServices("_p2pauction._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        Log.i("module2","check4");

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BackgroundTask b = new BackgroundTask();
                b.execute();
                Log.i("module2", "background task executed");
                btnSend.setVisibility(View.GONE);
            }
        });
    }
    class BackgroundTask extends AsyncTask<String, Void, String> {
        Socket s;
        DataOutputStream dataOutputStream;
        String message = myip;
        @Override
        protected String doInBackground(String... strings) {
            Log.i("module2", "inside doinbg func. ip is " + ip);
            try {
                s = new Socket(ip, 5825);
                Log.i("module2", "socket created");
                dataOutputStream = new DataOutputStream(s.getOutputStream());
                Log.i("module2", "got hold of output stream");
                dataOutputStream.writeUTF(message);
                Log.i("module2", "written utf message");
                dataOutputStream.close();
                Log.i("module2", "dos closed");
                s.close();
                Log.i("module2", "socket closed");
                Log.i("module2", "message sent to " + ip);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    class MyServer implements Runnable {
        ServerSocket ss;
        Socket mySocket;
        DataInputStream dataInputStream;
        BufferedReader bufferedReader;
        Handler handler = new Handler();

        @Override
        public void run() {
            try {
                ss = new ServerSocket(5825);
                while(true) {
                    mySocket = ss.accept();
                    dataInputStream = new DataInputStream(mySocket.getInputStream());
                    String message = dataInputStream.readUTF();
                    Log.i("module2", "data read " + message);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(message.contains("config")) {
                                String extract = message.substring(7);
                                String[] auctionArr = extract.split("@");
                                AUCTION_NAME = auctionArr[0];
                                auctionName.setText(AUCTION_NAME);
                                String[] itemArr = auctionArr[1].split(",");
                                ArrayList<String> itemArrList = new ArrayList<String>(Arrays.asList(itemArr));
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, itemArrList) {
                                    @NonNull
                                    @Override
                                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                                        View view = super.getView(position, convertView, parent);
                                        TextView text = (TextView) view.findViewById(android.R.id.text1);
                                        text.setTextColor(Color.parseColor("#000000"));
                                        return view;
                                    }
                                };
                                itemsList.setAdapter(adapter);
                            }
                            else if(message.contains("noOfBidders")) {
                                String extract = message.substring(12);
                                noOfBidders.setText("Bidders: " + extract);
                            }
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void onDestroy() {
        nsdManager.stopServiceDiscovery(discoveryListener);
        super.onDestroy();
    }
}