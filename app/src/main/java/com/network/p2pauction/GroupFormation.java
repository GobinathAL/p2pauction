package com.network.p2pauction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.acl.Group;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class GroupFormation extends AppCompatActivity {
    TextView auctionName, noOfBidders;
    ListView itemsList;
    MaterialButton startButton;
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    NsdManager.RegistrationListener registrationListener;
    String serviceName;
    String message;
    String ip;
    ArrayList<String> clientIpAddress;
    NsdManager nsdManager;
    MaterialTextView txtName, txtPrice, txtHighest;
    RelativeLayout relativeLayout;
    ArrayList<String> itemArrList;
    private int currentItem = 0;
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
        String[] itemList = AuctionCatalogue.AUCTION_CATALOGUE.split(",");
        itemArrList = new ArrayList<String>(Arrays.asList(itemList));
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
                    Thread auctionThread = new Thread(new AuctionServer());
                    auctionThread.start();
                    Log.i("module4", "calling start");
                    new BackgroundTask().execute("start");
                    startButton.setText("Next");
                    Log.i("module4", "Button text changed");
                }
                else if(startButton.getText().equals("Next")){
                    announceResult();
                    currentItem++;
                    if(currentItem == itemArrList.size() - 1) {
                        startButton.setText("Finish");
                    }
                }
                updateItemInfo();
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
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Waiting for client", Toast.LENGTH_SHORT).show();
                    }
                });
                while(true) {
                    mySocket = ss.accept();
                    dataInputStream = new DataInputStream(mySocket.getInputStream());
                    message = dataInputStream.readUTF();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Message Received: " + message, Toast.LENGTH_SHORT).show();
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
                while (true) {
                    mySocket = ss.accept();
                    dataInputStream = new DataInputStream(mySocket.getInputStream());
                    message = dataInputStream.readUTF();
                    Log.i("module4", "received " + message);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            new BroadcastTask().execute(message);
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
            if(strings.length == 1 && strings[0].contains("result")) {
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
            }
            else if(strings[0].contains("update")) {
                String[] splitString = strings[0].split(" ");
                Log.i("module4", "splitted the received msg");
                if(currentItem == Integer.parseInt(splitString[1])) {
                    Log.i("module4", "updating txtHighest with " + splitString[2]);
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
                            dataOutputStream.writeUTF(strings[0]);
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
    }
    @Override
    protected void onDestroy() {
        nsdManager.unregisterService(registrationListener);
        super.onDestroy();
    }
}