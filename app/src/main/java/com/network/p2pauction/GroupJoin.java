package com.network.p2pauction;

import android.content.Context;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.LoginFilter;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class GroupJoin extends AppCompatActivity {
    String AUCTION_NAME;
    String AUCTION_CATALOGUE;
    int AUCTION_DURATION;
    TextView auctionName, noOfBidders;
    ListView itemsList;
    MaterialButton btnSend;
    MaterialTextView txtName, txtPrice, txtHighest, txtLeaderboardPosition;
    TextInputLayout textInputLayout;
    TextInputEditText txtBidAmount;
    NsdManager.DiscoveryListener discoveryListener;
    NsdManager nsdManager;
    NsdServiceInfo mService;
    ArrayList<String> itemArrList;
    String ip, myip;
    RelativeLayout relativeLayout;
    MaterialTextView txtResult;
    private int currentItem = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_join);
        auctionName = (TextView) findViewById(R.id.AuctionName);
        noOfBidders = (TextView) findViewById(R.id.NoOfBidders);
        itemsList = (ListView) findViewById(R.id.ItemsList);
        btnSend = (MaterialButton) findViewById(R.id.sendToSocketBtn);
        relativeLayout = (RelativeLayout) findViewById(R.id.ItemDisplay);
        txtName = (MaterialTextView) findViewById(R.id.ItemName);
        txtPrice = (MaterialTextView) findViewById(R.id.StartingPrice);
        txtHighest = (MaterialTextView) findViewById(R.id.HighestBid);
        txtBidAmount = (TextInputEditText) findViewById(R.id.BidAmount);
        textInputLayout = (TextInputLayout) findViewById(R.id.inputLayout);
        txtLeaderboardPosition = (MaterialTextView) findViewById(R.id.LeaderboardPosition);
        txtResult = (MaterialTextView) findViewById(R.id.result);
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
                if(btnSend.getText().toString().toLowerCase().contains("join")) {
                    BackgroundTask b = new BackgroundTask();
                    b.execute();
                    Log.i("module2", "background task executed");
                    btnSend.setVisibility(View.GONE);
                }
                else if(btnSend.getText().toString().toLowerCase().contains("bid")) {

                    String bidAmount = txtBidAmount.getText().toString();
                    Log.i("module4", "bid amound: " + bidAmount);
                    if(Integer.parseInt(bidAmount) > Integer.parseInt(txtHighest.getText().toString().replace("Highest Bid: ", ""))
                        && Integer.parseInt(bidAmount) > Integer.parseInt(txtPrice.getText().toString().replace("Starting Price: ", ""))) {
                        Log.i("module4","calling broadcast");
                        new BroadcastTask().execute("update " + currentItem + " " + bidAmount + "#" + myip);
                        Log.i("module4", "broadcasted");
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Enter higher amount", Toast.LENGTH_SHORT).show();
                    }
                }
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
    class BroadcastTask extends AsyncTask<String, Void, String> {
        Socket s;
        DataOutputStream dataOutputStream;
        @Override
        protected String doInBackground(String... strings) {
            try {
                s = new Socket(ip, 5826);
                dataOutputStream = new DataOutputStream(s.getOutputStream());
                dataOutputStream.writeUTF(strings[0]);
                Log.i("module4", "message: " + strings[0] + " status: sent");
                dataOutputStream.close();
                s.close();

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
                                itemArrList = new ArrayList<String>(Arrays.asList(itemArr));
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
                            else if(message.contains("start")) {
                                Thread auctionThread = new Thread(new AuctionServer());
                                auctionThread.start();
                                itemsList.setVisibility(View.GONE);
                                relativeLayout.setVisibility(View.VISIBLE);
                                btnSend.setText("Bid");
                                btnSend.setVisibility(View.VISIBLE);
                                updateItemInfo();
                            }
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    class AuctionServer implements Runnable {
        ServerSocket ss;
        Socket mySocket;
        DataInputStream dataInputStream;
        BufferedReader bufferedReader;
        Handler handler = new Handler();
        @Override
        public void run() {
            try {
                ss = new ServerSocket(5826);
                while(true) {
                    mySocket = ss.accept();
                    dataInputStream = new DataInputStream(mySocket.getInputStream());
                    String message = dataInputStream.readUTF();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(message.contains("result")) {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        currentItem++;
                                        txtResult.setText("");
                                        Log.i("module6", "gone");
                                        updateItemInfo();
                                    }
                                }, 5000);

                                if(txtLeaderboardPosition.getText().toString().equals("Your Leaderboard Position: 1")) {
                                    txtResult.setText("You won this bid");
                                    Log.i("module6", "winner");
                                } else {
                                    Log.i("module6", "not a winner");
                                }
                            }
                            else if(message.contains("update")) {
                                Log.i("module4", "message: " + message + "status: received");
                                String[] splitMessage = message.split(" ");
                                if(currentItem == Integer.parseInt(splitMessage[1])) {
                                    txtHighest.setText("Highest Bid: " + splitMessage[2]);
                                }
                            }
                            else if(message.contains("position") && message.contains(myip)) {
                                String[] splitMessage = message.split(" ");
                                txtLeaderboardPosition.setText("Your Leaderboard Position: " + splitMessage[1]);
                            }
                            else if(message.contains("leaderboard")) {
                                String[] splitMessage = message.split(" ");
                                txtLeaderboardPosition.setText("Your Leaderboard Position: " + splitMessage[1]);
                            }
                            else if(message.contains("finish")) {
                                btnSend.setVisibility(View.GONE);
                                txtBidAmount.setVisibility(View.GONE);
                                textInputLayout.setVisibility(View.GONE);
                                if(txtLeaderboardPosition.getText().toString().equals("Your Leaderboard Position: 1"))
                                    txtResult.setText("You won this bid");
                            }
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void updateItemInfo() {
        String[] itemInfo = itemArrList.get(currentItem).split(" ");
        txtName.setText("Name: " + itemInfo[0]);
        txtPrice.setText("Starting Price: " + itemInfo[1]);
        txtHighest.setText("Highest Bid: " + "0");
        txtLeaderboardPosition.setText("Leaderboard Position: ");
    }
    @Override
    protected void onDestroy() {
        nsdManager.stopServiceDiscovery(discoveryListener);
        super.onDestroy();
    }
}