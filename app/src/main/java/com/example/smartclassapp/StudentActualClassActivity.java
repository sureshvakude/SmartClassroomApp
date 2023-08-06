package com.example.smartclassapp;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;


public class StudentActualClassActivity extends AppCompatActivity {
    private WifiP2pManager.Channel channel;
    private WifiP2pManager wifiP2pManager;
    private IntentFilter intentFilter;

    private BroadcastReceiver broadcastReceiver;
    Button sendbtn;
    InetAddress address;
    ClientClass clientClass;
    SendReceive sendReceive;
    static final int MESSAGE_READ = 1;
    EditText writemsg;
    //-----------------------------------------------
    screenClientClass screenclientClass;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    Canvas canvas;

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_actual_class);
        byte[] addressBytes = getIntent().getByteArrayExtra("address");
        if (addressBytes != null) {
            try {
                address = InetAddress.getByAddress(addressBytes);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        initialwork();
        startwork();
    }

    private void startwork() {
        sendbtn.setOnClickListener(v -> {
            Executor executer = Executors.newSingleThreadExecutor();
            executer.execute(() -> {
                String msg = writemsg.getText().toString();
                sendReceive.write(msg.getBytes());
            });
        });
    }

    private void initialwork() {

        sendbtn = (Button) findViewById(R.id.sendbtnservermsg);
        writemsg = (EditText) findViewById(R.id.msgtype);
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);
        // surfaceView = findViewById(R.id.screenshared);
        // surfaceHolder = surfaceView.getHolder();
        //canvas = surfaceHolder.lockCanvas();

        imageView = findViewById(R.id.screenshare);
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
                    //Toast.makeText(context, "called1 address : "+address, Toast.LENGTH_SHORT).show();
                    clientClass = new ClientClass(address);
                    clientClass.start();

                    screenclientClass = new screenClientClass(address);
                    screenclientClass.start();

                } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                    //Toast.makeText(context, "called2", Toast.LENGTH_SHORT).show();
                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

                    //Toast.makeText(context, "called 3", Toast.LENGTH_SHORT).show();
                } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                    //Toast.makeText(context, "called4", Toast.LENGTH_SHORT).show();
                }
            }
        };

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

    //-----message sending code socket class
    public class ClientClass extends Thread {
        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress) {
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
            Toast.makeText(getApplicationContext(), hostAdd, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd, 8888), 50000);
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class SendReceive extends Thread {
        private final Socket socket;
        private final InputStream inputstream;
        private final OutputStream outputstream;

        public SendReceive(Socket skt) {
            socket = skt;
            try {
                inputstream = socket.getInputStream();
                outputstream = socket.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];

            while (socket != null) {
                try {
                    int bytes;
                    bytes = inputstream.read(buffer);
                    if (bytes > 0) {
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputstream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    Handler handler = new Handler(msg -> {
        switch (msg.what) {
            case MESSAGE_READ:
                byte[] readBuff = (byte[]) msg.obj;
                String tempMsg = new String(readBuff, 0, msg.arg1);
                Toast.makeText(StudentActualClassActivity.this, "server send  :" + tempMsg, Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    });

    //---------------------------------------------


    public class screenClientClass extends Thread {
        Socket socket;
        String hostAdd;

        public screenClientClass(InetAddress hostAddress) {
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd, 4444), 500);
                InputStream inputStream = socket.getInputStream();

                byte[] buffer = new byte[1024];
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                int flag = 1;
                while (socket != null) {
                    try {
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            byteArrayOutputStream.write(buffer, 0, bytesRead);
                            if (bytesRead < buffer.length) {
                                if(flag == 1) {
                                    processImageData(byteArrayOutputStream.toByteArray());
                                    flag++;
                                }else {
                                    flag++;
                                    if(flag==4){
                                        flag=1;
                                    }
                                }
                                byteArrayOutputStream.reset();
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
            } 
        } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void processImageData(byte[] toByteArray) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(toByteArray);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                runOnUiThread(() ->
                        imageView.setImageBitmap(bitmap)
                );
            }
        }
        }

}



