package com.ketan_studio.example.wifi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.InetAddresses;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Button on,discover;
    WifiManager wifiManager;
    WifiP2pManager manager;
    WifiP2pManager.Channel mChannel;
    ListView listView;
    TextView conStatus;

    BroadcastReceiver mRecever;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initalization();
        NextWork();
    }

    private void NextWork() {
        on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                    Toast.makeText(MainActivity.this, "OFF", Toast.LENGTH_SHORT).show();
                    on.setText("ON");
                }else {
                    wifiManager.setWifiEnabled(true);
                    on.setText("OFF");
                }
            }
        });
        discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Discovered Peer", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(MainActivity.this, "Discovered Failed", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice device = deviceArray[position];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                manager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Connected to "+device.deviceName, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void initalization() {
        on = (Button)findViewById(R.id.on);
        discover = (Button)findViewById(R.id.discover);
        listView = (ListView)findViewById(R.id.listView);
        conStatus = (TextView)findViewById(R.id.conStatus);
        
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = manager.initialize(this,getMainLooper(),null);

        mRecever = new WifiDirectBroadcastReceiver(manager,mChannel,this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }
    
    WifiP2pManager.PeerListListener peerListListener= new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peersList) {
            if (!peersList.equals(peers))
            {
                peers.clear();
                peers.addAll(peersList.getDeviceList());
                deviceNameArray = new String[peersList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peersList.getDeviceList().size()];
                int index = 0;
                for(WifiP2pDevice device : peersList.getDeviceList()){
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(adapter);
            }

            if (peers.size()==0)
            {
                Toast.makeText(MainActivity.this, "No device found...", Toast.LENGTH_SHORT).show();
                return;
            }
            
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress groupOwnerAddress = info.groupOwnerAddress;
            if (info.groupFormed && info.isGroupOwner)
            {
                conStatus.setText("Host");
            }else if (info.groupFormed){
                conStatus.setText("Client");
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mRecever,mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mRecever);
    }
}
