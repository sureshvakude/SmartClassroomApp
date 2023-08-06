package com.example.smartclassapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class FacultyHomeActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1000;
    private static final int REQUEST_PERMISSION = 1001;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaProjectionCallback mediaprojectioncallback;
    private MediaRecorder mediaRecorder;
    private int mScreenDensity;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;


    static {
        ORIENTATIONS.append(Surface.ROTATION_0,0);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,180);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }
    //view
    //    private VideoView videoView;
    private String videoUri = "";
    static final int MESSAGE_READ=1;
    private RelativeLayout rootLayout;

    private WifiP2pManager wifiP2pManager;
    RecyclerView msgListRecyclerView;
    private BroadcastReceiver broadcastReceiver;
    private WifiP2pManager.Channel channel;
    DeviceListAdapter deviceListAdaptermsg;
    private IntentFilter intentFilter;
    EditText msgtype;
    TextView txtcount;
    Button btngrpcrt,btnsend;
    ToggleButton toggleButton;
    ServerClass serverClass;
    ArrayList<SendReceive> sendReceiveList = new ArrayList<>();

    //---------------------------------------------------
    screenServerClass screenServerClass;
    ArrayList<screenSendReceive> screenSendReceiveList = new ArrayList<>();

    private SurfaceView surfaceView;
    ImageReader imageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_home);

        initialwork();
        startwork();

    }

    private void startwork() {

        btngrpcrt.setOnClickListener(view -> createGroup());

        btnsend.setOnClickListener(v -> {
            String message = msgtype.getText().toString();
            sendMessageToAllDevices(message);

        });

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        //  videoView = (VideoView) findViewById(R.id.videoView);
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        rootLayout = (RelativeLayout) findViewById(R.id.rootlayout);

        toggleButton.setOnClickListener(v -> {
            if(ContextCompat.checkSelfPermission(FacultyHomeActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    +ContextCompat.checkSelfPermission(FacultyHomeActivity.this, android.Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED)
            {
                if(ActivityCompat.shouldShowRequestPermissionRationale(FacultyHomeActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        || ActivityCompat.shouldShowRequestPermissionRationale(FacultyHomeActivity.this, android.Manifest.permission.RECORD_AUDIO))
                {
                    startService();
                    toggleButton.setChecked(false);
                    Snackbar.make(rootLayout,"Permissions",Snackbar.LENGTH_SHORT).setAction("ENABLE", v1 -> ActivityCompat.requestPermissions(FacultyHomeActivity.this,new String[]{
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.RECORD_AUDIO
                    },REQUEST_PERMISSION)).show();
                }
                else
                {
                    ActivityCompat.requestPermissions(FacultyHomeActivity.this,new String[]{
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                    },REQUEST_PERMISSION);
                }
            }
            else
            {
                toggleScreenShare(v);
            }
        });
    }

    private void createGroup() {
        wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(FacultyHomeActivity.this, "group created succefully", Toast.LENGTH_SHORT).show();
                serverClass = new ServerClass();
                serverClass.start();
                screenServerClass = new screenServerClass();
                screenServerClass.start();
            }
            @Override
            public void onFailure(int reason) {

            }

        });
    }

    public class ServerClass extends Thread{
        ServerSocket serverSocket;

        @Override
        public void run() {
            super.run();
            try {
                serverSocket = new ServerSocket(8888);
                while (true) {
                    Socket socket = serverSocket.accept();
                    SendReceive sendReceive = new SendReceive(socket);
                    sendReceiveList.add(sendReceive);
                    sendReceive.start();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }


    private class SendReceive extends Thread{
        private final Socket socket;
        private final InputStream inputstream;
        private final OutputStream outputstream;

        public SendReceive(Socket skt){
            socket=skt;
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

            while(socket!=null){
                try {
                    int bytes;
                    bytes=inputstream.read(buffer);
                    if(bytes>0){
                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void write(final byte[] bytes) {
            new Thread(() -> {
                try {
                    outputstream.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }
    private void sendMessageToAllDevices(String message) {
        byte[] bytes = message.getBytes();
        for (SendReceive sendReceive : sendReceiveList) {
            sendReceive.write(bytes);
        }
    }

    Handler handler=new Handler(msg -> {
        switch(msg.what){
            case MESSAGE_READ:
                byte[] readBuff = (byte[]) msg.obj;
                String tempMsg = new String(readBuff,0,msg.arg1);
                Toast.makeText(FacultyHomeActivity.this, "received msg :"+tempMsg, Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    });


    private void initialwork() {

        btngrpcrt = (Button) findViewById(R.id.btngrpcrt);
        txtcount = (TextView) findViewById(R.id.txtcout);
        btnsend = (Button)findViewById(R.id.sendbtnservermsg);
        msgtype = (EditText) findViewById(R.id.msgtype);
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);


        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);




        msgListRecyclerView = (RecyclerView) findViewById(R.id.msgviewcycle);
        deviceListAdaptermsg = new DeviceListAdapter(new ArrayList<>(), details -> {
           //onclickprintmsg writehere
        });
        msgListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        msgListRecyclerView.setAdapter(deviceListAdaptermsg);

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
                  //  Toast.makeText(context, "first called", Toast.LENGTH_SHORT).show();
                } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                        wifiP2pManager.requestGroupInfo(channel, wifiP2pGroup -> {
                            if(wifiP2pGroup!=null){
                                int numofclients = wifiP2pGroup.getClientList().size();
                                txtcount.setText("Connected device count : " + numofclients);
                            }
                        });

                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                   // Toast.makeText(context, "third called", Toast.LENGTH_SHORT).show();

                } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                   // Toast.makeText(context, "fourth called", Toast.LENGTH_SHORT).show();
                }
            }
        };

    }

    public void startService() {
        String input = "hello";

        Intent serviceIntent = new Intent(FacultyHomeActivity.this, ExampleService.class);
        serviceIntent.putExtra("inputExtra", input);

        ContextCompat.startForegroundService(FacultyHomeActivity.this, serviceIntent);
    }

    public void stopService() {

        Intent serviceIntent = new Intent(FacultyHomeActivity.this, ExampleService.class);
        stopService(serviceIntent);

    }

    private void toggleScreenShare(View v) {
        if(((ToggleButton)v).isChecked()){
            startService();
            initRecorder();
            recordScreen();
        }else{
            mediaRecorder.stop();
            mediaRecorder.reset();
            stopRecordScreen();

        }
    }

    private void recordScreen() {

        if(mediaProjection == null){
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),REQUEST_CODE);
            return;
        }
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();



    }

    private VirtualDisplay createVirtualDisplay() {
        return mediaProjection.createVirtualDisplay("FacultyHomeActivity",DISPLAY_WIDTH,DISPLAY_HEIGHT,mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,mediaRecorder.getSurface(),null,null);
    }

    private void initRecorder() {
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            videoUri = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+
                    new StringBuilder("/EDMTRecord_").append(new SimpleDateFormat("dd-mm-yyyy-hh_mm_ss")
                            .format(new Date())).append(".mp4").toString();
            mediaRecorder.setOutputFile(videoUri);
            mediaRecorder.setVideoSize(DISPLAY_WIDTH,DISPLAY_HEIGHT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(512*1000);
            mediaRecorder.setVideoFrameRate(30);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation*90);
            mediaRecorder.setOrientationHint(orientation);
            mediaRecorder.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode != REQUEST_CODE){
            Toast.makeText(this, "unk error", Toast.LENGTH_SHORT).show();
            return;
        }
        if(resultCode != RESULT_OK){
            Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
            toggleButton.setChecked(false);
            return;
        }

        mediaprojectioncallback  = new MediaProjectionCallback();
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode,data);
        mediaProjection.registerCallback(mediaprojectioncallback,null);
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
        //--------------------------------------------

        imageReader = ImageReader.newInstance(DISPLAY_WIDTH, DISPLAY_HEIGHT, PixelFormat.RGBA_8888, 10);
        VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                DISPLAY_WIDTH,
                DISPLAY_HEIGHT,
                mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                null
        );

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    // Process and send the captured image over the socket
                    sendImageOverSocket(image);
                }
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }, null);


    }

    private void sendImageOverSocket(Image image){

        Bitmap bitmap = ImageUtils(image);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 10, stream);
            byte[] pimagearray = stream.toByteArray();
            sendScreenToAllDevices(pimagearray);

        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private Bitmap ImageUtils(Image image) {
        if(image==null){
            return null;
        }
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        Bitmap bitmap = Bitmap.createBitmap(width+rowPadding/pixelStride,height,Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    //-----------------------------------------------------
    public class screenServerClass extends Thread{
        ServerSocket serverSocket;

        @Override
        public void run() {
            super.run();
            try {
                serverSocket = new ServerSocket(4444);
                while (true) {
                    Socket socket1 = serverSocket.accept();
                    screenSendReceive screenSendReceive = new screenSendReceive(socket1);
                    screenSendReceiveList.add(screenSendReceive);
                    screenSendReceive.start();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
    private class screenSendReceive extends Thread{
        private final Socket socket;
        private final OutputStream outputstream;

        public screenSendReceive(Socket skt){
            socket=skt;
            try {
                outputstream = socket.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void write(final byte[] bytes) {
            new Thread(() -> {
                try {
                    outputstream.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private void sendScreenToAllDevices(byte[] message) {
        for (screenSendReceive sendReceive : screenSendReceiveList) {
            sendReceive.write(message);
        }
    }


    //-------------------------------------------------------------------------

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if(toggleButton.isChecked()){
                toggleButton.setChecked(false);
                mediaRecorder.stop();
                mediaRecorder.reset();
            }
            mediaProjection = null;
            stopRecordScreen();
            super.onStop();
        }
    }

    private void stopRecordScreen() {
        if(virtualDisplay==null){
            return;
        }
        stopService();
        virtualDisplay.release();
        destroyMediaProjection();
        imageReader.close();
    }

    private void destroyMediaProjection() {
        if(mediaProjection!=null){
            mediaProjection.unregisterCallback(mediaprojectioncallback);
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_PERMISSION:
            {
                if((grantResults.length >0) && (grantResults[0]+grantResults[1] == PackageManager.PERMISSION_GRANTED))
                {
                    toggleScreenShare(toggleButton);
                }
                else{
                    startService();
                    toggleButton.setChecked(false);
                    Snackbar.make(rootLayout,"Permissions",Snackbar.LENGTH_SHORT).setAction("ENABLE", v -> ActivityCompat.requestPermissions(FacultyHomeActivity.this,new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                    },REQUEST_PERMISSION)).show();
                }
                return;
            }
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(FacultyHomeActivity.this, "Group Deleted Succefully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(FacultyHomeActivity.this, "Failed to destroy group", Toast.LENGTH_SHORT).show();
            }
        });
    }
}