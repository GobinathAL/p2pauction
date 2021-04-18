package com.network.p2pauction;

import android.content.Context;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textview.MaterialTextView;

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
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class GroupFormation extends AppCompatActivity {
    TextView auctionName, noOfBidders;
    ListView itemsList, log;
    MaterialButton startButton;
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    NsdManager.RegistrationListener registrationListener;
    String serviceName;
    String message;
    String ip;
    String selectiveFloodingToggle = "position";
    ArrayList<String> clientIpAddress;
    ArrayList<Double> bidderFreq = new ArrayList<Double>();
    NsdManager nsdManager;
    MaterialTextView txtName, txtPrice, txtHighest, txtLeaderboard, txtActiveBidders;
    SwitchMaterial toggle;
    RelativeLayout relativeLayout;
    ArrayList<String> itemArrList, logArrList, playerBidHistory, bidderLeaderboard, activeBidders = new ArrayList<String>(), prev = new ArrayList<String>();
    private int currentItem = 0;
    boolean auctionThreadFlag = true;
    private NetworkTimerSF timerSF = new NetworkTimerSF();
    private NetworkTimerWOSF timerWOSF = new NetworkTimerWOSF();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_formation);
        clientIpAddress = new ArrayList<String>();
        auctionName = (TextView) findViewById(R.id.AuctionName_OwnerSide);
        auctionName.setText(AuctionCatalogue.AUCTION_NAME);
        noOfBidders = (TextView) findViewById(R.id.NoOfBidders_OwnerSide);
        itemsList = (ListView) findViewById(R.id.ItemsList_OwnerSide);
        startButton = (MaterialButton) findViewById(R.id.StartButton);
        relativeLayout = (RelativeLayout) findViewById(R.id.ItemDisplay_OwnerSide);
        txtName = (MaterialTextView) findViewById(R.id.ItemName_OwnerSide);
        txtPrice = (MaterialTextView) findViewById(R.id.StartingPrice_OwnerSide);
        txtHighest = (MaterialTextView) findViewById(R.id.HighestBid_OwnerSide);
        txtLeaderboard = (MaterialTextView) findViewById(R.id.Leaderboard);
        txtActiveBidders = (MaterialTextView) findViewById(R.id.ActiveBidders);
        toggle = (SwitchMaterial) findViewById(R.id.Switch);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    selectiveFloodingToggle = "leaderboard";
                }
                else {
                    selectiveFloodingToggle = "position";
                }
            }
        });
        String[] itemList = AuctionCatalogue.AUCTION_CATALOGUE.split(",");
        itemArrList = new ArrayList<String>(Arrays.asList(itemList));
        playerBidHistory = new ArrayList<String>();
        bidderLeaderboard = new ArrayList<String>();
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

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        Thread myThread = new Thread(new MyServer());
        myThread.start();
        initializeRegistrationListener();
        try {
            registerService(5825);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(startButton.getText().equals("Start auction")) {
                    Log.i("module4", "start button pressed");
                    Log.i("module4", "thread stopped");
                    for(int i = 0; i < clientIpAddress.size(); i++)
                        bidderFreq.add(0.0);
                    Thread auctionThread = new Thread(new AuctionServer());
                    auctionThread.start();
                    Log.i("module4", "calling start");
                    new BackgroundTask().execute("start");
                    itemsList.setVisibility(View.GONE);
                    startButton.setText("Next");
                    Log.i("module4", "Button text changed");
                    updateItemInfo();
                }
                else if(startButton.getText().equals("Next")){
                    bidderLeaderboard.clear();
                    playerBidHistory.clear();
                    activeBidders.clear();
                    announceResult();
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    currentItem++;
                    if(currentItem == itemArrList.size() - 1) {
                        startButton.setText("Finish");
                    }
                    updateItemInfo();
                }
                else if(startButton.getText().equals("Finish")) {
                    Toast.makeText(getApplicationContext(), "Auction ended", Toast.LENGTH_SHORT).show();
                    auctionThreadFlag = false;
                    new BroadcastTask().execute("finish");
                }
            }
        });
    }

    private void initializeRegistrationListener() {
        registrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {

            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {

            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                serviceName = serviceInfo.getServiceName();
                Log.i("module2","nsdservice registered");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {

            }
        };
    }

    private void registerService(int port) throws UnknownHostException {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        Log.i("module2", ip);
        serviceInfo.setServiceName("p2pauction".concat(ip));
        Log.i("module2", serviceInfo.getServiceName());
        serviceInfo.setServiceType("_p2pauction._tcp");
        serviceInfo.setPort(port);
        serviceInfo.setHost(InetAddress.getByName(ip));
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
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
                    message = dataInputStream.readUTF();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(!clientIpAddress.contains(message))
                                clientIpAddress.add(message);
                            noOfBidders.setText("Bidders: " + clientIpAddress.size());
                            Log.i("module2", message);
                            new BackgroundTask().execute("config:" + message);
                            new BackgroundTask().execute("noOfBidders:" + clientIpAddress.size());
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    class BackgroundTask extends AsyncTask<String, Void, String> {
        Socket s;
        DataOutputStream dataOutputStream;
        String message;
        @Override
        protected String doInBackground(String... strings) {
            String command = strings[0];
            if(command.contains("config")) {
                try {
                    s = new Socket(command.substring(7), 5825);
                    Log.i("module2", "socket established " + command.substring(7));
                    dataOutputStream = new DataOutputStream(s.getOutputStream());
                    dataOutputStream.writeUTF("config:" + AuctionCatalogue.AUCTION_NAME + "@" + AuctionCatalogue.AUCTION_CATALOGUE + "@" + AuctionCatalogue.AUCTION_DURATION);
                    dataOutputStream.close();
                    Log.i("module2", command + "written to socket");
                    s.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(command.contains("noOfBidders") || command.contains("start")) {
                if(command.contains("start")) {
                    timerSF.startTimer();
                    timerWOSF.startTimer();
                }
                for(String sendip : clientIpAddress) {
                    try {
                        Socket s = new Socket(sendip, 5825);
                        Log.i("module2", "socket established " + command + "to " + sendip);
                        dataOutputStream = new DataOutputStream(s.getOutputStream());
                        dataOutputStream.writeUTF(command);
                        dataOutputStream.close();
                        Log.i("module2", command + " written");
                        s.close();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }
    class AuctionServer implements Runnable {

        ServerSocket ss;
        DataInputStream dataInputStream;
        Socket mySocket;;
        BufferedReader bufferedReader;
        Handler handler = new Handler();
        String message;
        @Override
        public void run() {
            try {
                ss = new ServerSocket(5826);
                while (auctionThreadFlag) {
                    mySocket = ss.accept();
                    dataInputStream = new DataInputStream(mySocket.getInputStream());
                    message = dataInputStream.readUTF();
                    Log.i("module5", "received " + message);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            new BroadcastTask().execute(message);
                            new BroadcastTask().execute(selectiveFloodingToggle);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    class BroadcastTask extends AsyncTask<String, Void, String> {
        Socket s;
        DataOutputStream dataOutputStream;
        String message;
        @Override
        protected String doInBackground(String... strings) {
            String command = strings[0];
            if(strings.length == 1 && (strings[0].contains("result") || strings[0].contains("finish")) ) {

                if(strings[0].contains("finish")) {
                    Log.i("module6", "Delay without SF " + timerWOSF.getDelay());
                    Log.i("module6", "Delay with SF " + timerSF.getDelay());
                }
                for(String sendip : clientIpAddress) {
                    try {
                        Socket s = new Socket(sendip, 5826);
                        dataOutputStream = new DataOutputStream(s.getOutputStream());
                        dataOutputStream.writeUTF(strings[0]);
                        dataOutputStream.close();
                        s.close();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(selectiveFloodingToggle.contains("position")) {
                    timerWOSF.waitForAck();
                    timerWOSF.updateDelayOnAckReceived(command);
                }
                if(selectiveFloodingToggle.contains("leaderboard")) {
                    timerSF.waitForAck();
                    timerSF.updateDelayOnAckReceived(command);
                }
            }
            else if(strings[0].contains("update") && auctionThreadFlag) {
                String split[] = strings[0].split("#");
                playerBidHistory.add(split[1]);
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        updateLeaderboard();
                    }
                });
                String[] splitString = split[0].split(" ");
                if(currentItem == Integer.parseInt(splitString[1])) {
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            txtHighest.setText("Highest Bid: " + splitString[2]);
                        }
                    });
                    for(String sendip : clientIpAddress) {
                        try {
                            Socket s = new Socket(sendip, 5826);
                            dataOutputStream = new DataOutputStream(s.getOutputStream());
                            dataOutputStream.writeUTF(split[0]);
                            dataOutputStream.close();
                            s.close();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(selectiveFloodingToggle.contains("position")) {
                        timerWOSF.waitForAck();
                        timerWOSF.updateDelayOnAckReceived(command);
                    }
                    if(selectiveFloodingToggle.contains("leaderboard")) {
                        timerSF.waitForAck();
                        timerSF.updateDelayOnAckReceived(command);
                    }
                }

            }
            else if(strings[0].contains("position") && auctionThreadFlag) {
                for(String sendip : clientIpAddress) {
                    int pos = bidderLeaderboard.size() + 1;
                    if(bidderLeaderboard.contains(sendip))
                        pos = bidderLeaderboard.indexOf(sendip) + 1;
                    try {
                        Socket s = new Socket(sendip, 5826);
                        dataOutputStream = new DataOutputStream(s.getOutputStream());
                        dataOutputStream.writeUTF("position " + pos + " " + sendip);
                        Log.i("flooding", "Broadcasting \"" + "position " + pos + " " + sendip + "\" to all peers");
                        dataOutputStream.close();
                        s.close();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                timerWOSF.waitForAck();
                timerWOSF.updateDelayOnAckReceived(command);
            }
            else if(strings[0].contains("leaderboard") && auctionThreadFlag) {
                if(prev == null || prev.size() == 0) {
                    for(String sendip : clientIpAddress) {
                        int pos = bidderLeaderboard.size() + 1;
                        if(bidderLeaderboard.contains(sendip))
                            pos = bidderLeaderboard.indexOf(sendip) + 1;
                        try {
                            Socket s = new Socket(sendip, 5826);
                            dataOutputStream = new DataOutputStream(s.getOutputStream());
                            dataOutputStream.writeUTF("leaderboard " + pos);
                            Log.i("flooding", "Broadcasting \"" + "leaderboard " + pos + "\" to all peers");
                            timerSF.waitForAck();
                            timerSF.updateDelayOnAckReceived(command + "initial");
                            dataOutputStream.close();
                            s.close();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    for(String sendip : clientIpAddress) {
                        if(prev.indexOf(sendip) != bidderLeaderboard.indexOf(sendip)) {
                            try {
                                int pos = bidderLeaderboard.size() + 1;
                                if(bidderLeaderboard.contains(sendip))
                                    pos = bidderLeaderboard.indexOf(sendip) + 1;
                                if(prev.contains(sendip) && prev.indexOf(sendip) == (pos - 1)) continue;
                                Socket s = new Socket(sendip, 5826);
                                dataOutputStream = new DataOutputStream(s.getOutputStream());
                                dataOutputStream.writeUTF("leaderboard " + (bidderLeaderboard.indexOf(sendip) + 1));
                                Log.i("flooding", "sending " + "\"leaderboard " + pos + "\" to " + sendip);
                                timerSF.waitForAck();
                                timerSF.updateDelayOnAckReceived(command);
                                dataOutputStream.close();
                                s.close();
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            return null;
        }
    }
    private void announceResult() {
        new BroadcastTask().execute("result");
    }
    private void updateItemInfo() {
        relativeLayout.setVisibility(View.VISIBLE);
        String[] itemInfo = itemArrList.get(currentItem).split(" ");
        txtName.setText("Name: " + itemInfo[0]);
        txtPrice.setText("Starting Price: " + itemInfo[1]);
        txtHighest.setText("Highest Bid: " + "0");
        txtLeaderboard.setText("Leaderboard: ");
    }
    private void updateLeaderboard() {
        prev.clear();
        prev = (ArrayList<String>) bidderLeaderboard.clone();
        bidderLeaderboard.clear();
        Log.i("module5", "leaderboard cleared");
        for(int i = playerBidHistory.size() - 1; i >= 0; i--) {
            if(bidderLeaderboard.contains(playerBidHistory.get(i))) continue;
            bidderLeaderboard.add(playerBidHistory.get(i));
        }
        txtLeaderboard.setText("Leaderboard: ");
        String str = txtLeaderboard.getText().toString();
        for(int i = 0; i < bidderLeaderboard.size(); i++) {
            str += "\n" + bidderLeaderboard.get(i);
        }
        str += "\n";
        for(int i = 0; i < clientIpAddress.size(); i++) {
            if(bidderLeaderboard.contains(clientIpAddress.get(i))) continue;
            str += clientIpAddress.get(i) + " ";
        }
        txtLeaderboard.setText(str);
        bidderFreq.clear();
        for(int i = 0; i < clientIpAddress.size(); i++) {
            int count = Collections.frequency(playerBidHistory, clientIpAddress.get(i));
            Log.i("module5", "count " + count + "size " + playerBidHistory.size() + ((double)count / (double)playerBidHistory.size() * 100.0));
            bidderFreq.add(((double)count / (double)playerBidHistory.size()) * 100.0);
        }
        Log.i("module5", "bid freq " + bidderFreq.toString());
        activeBidders.clear();
        for(int i = 0; i < bidderFreq.size(); i++) {
            if(bidderFreq.get(i) > 25.0) {
                activeBidders.add(clientIpAddress.get(i));
            }
        }
        Log.i("module5", "active bidders" + activeBidders.toString());
        txtActiveBidders.setText(activeBidders.toString());
    }
    @Override
    protected void onDestroy() {
        nsdManager.unregisterService(registrationListener);
        super.onDestroy();
    }
}