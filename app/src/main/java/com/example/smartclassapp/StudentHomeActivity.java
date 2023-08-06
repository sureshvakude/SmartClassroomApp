package com.example.smartclassapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;


public class StudentHomeActivity extends AppCompatActivity {

    List<String> deviceNames;
    WifiP2pDevice[] deviceId;
    private RecyclerView deviceListRecyclerView;
    private DeviceListAdapter deviceListAdapter;
    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    private IntentFilter intentFilter;
    String owneraddress;
    private BroadcastReceiver broadcastReceiver;
    private LocationManager locationManager;
    Button btnOnOff,btngpsonoff,btndiscover,btnsendmsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        initialwork();
        exwork();

    }




    private void exwork() {
        btnOnOff.setOnClickListener(v -> {
            if(wifiManager.getWifiState()==3){
                wifiManager.setWifiEnabled(false);
                btnOnOff.setText("ON");
            }
            else {
                wifiManager.setWifiEnabled(true);
                btnOnOff.setText("OFF");
            }
        });

        btngpsonoff.setOnClickListener(v -> {
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                btngpsonoff.setText("OFF");
            }
            else{
             btngpsonoff.setText("ON");
            }
        });

        btndiscover.setOnClickListener(v -> mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(StudentHomeActivity.this, "Discovery Started", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(StudentHomeActivity.this, "Discovery Starting Failed", Toast.LENGTH_SHORT).show();
            }
        }));

    }

    private void initialwork() {
        btnOnOff = (Button) findViewById(R.id.btnwifionoff);
        btngpsonoff = (Button) findViewById(R.id.btngpsonoff);
        btndiscover = (Button) findViewById(R.id.btndiscover);


        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        wifiManager =(WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);

        deviceListRecyclerView = (RecyclerView) findViewById(R.id.device_list);
        deviceListAdapter = new DeviceListAdapter(new ArrayList<>(), this::onitemclickfun);
        deviceListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceListRecyclerView.setAdapter(deviceListAdapter);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        Toast.makeText(context, "WiFi Direct is enabled.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "WiFi Direct is not enabled.", Toast.LENGTH_SHORT).show();
                    }
                } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                    if (mManager != null) {

                        mManager.requestPeers(mChannel, peers -> {
                            deviceNames = new ArrayList<>();
                            deviceId = new WifiP2pDevice[peers.getDeviceList().size()];
                            int index=0;
                            for (WifiP2pDevice device : peers.getDeviceList()) {
                                deviceNames.add(device.deviceName);
                                deviceId[index] = device;
                                index++;
                            }
                            deviceListAdapter.deviceNames = deviceNames;
                            deviceListAdapter.notifyDataSetChanged();
                        });
                    }
                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if (networkInfo.isConnected()) {
                        mManager.requestConnectionInfo(mChannel, wifiP2pInfo -> {
                            InetAddress groupowneradd = wifiP2pInfo.groupOwnerAddress;
                            owneraddress = groupowneradd.getHostAddress();
                            byte[] addressBytes = groupowneradd.getAddress();
                            Toast.makeText(context, "Connected to group.", Toast.LENGTH_SHORT).show();
                            Intent intent1 = new Intent(getApplicationContext(),StudentActualClassActivity.class);
                            intent1.putExtra("address",addressBytes);
                            startActivity(intent1);
                        });
                    } else {
                        Toast.makeText(context, "Disconnected from group.", Toast.LENGTH_SHORT).show();
                    }
                } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                    WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                    Toast.makeText(context, "This device's name is " + device.deviceName + ".", Toast.LENGTH_SHORT).show();
                }
            }
        };

    }

    private void onitemclickfun(String details) {

        int i = deviceNames.indexOf(details);
        final WifiP2pDevice device = deviceId[i];
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"Connected to "+device.deviceName,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(getApplicationContext(),"Not Connected",Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }


}
