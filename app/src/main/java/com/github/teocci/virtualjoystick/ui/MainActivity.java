package com.github.teocci.virtualjoystick.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.github.teocci.virtualjoystick.BluetoothService;
import com.github.teocci.virtualjoystick.R;
import com.github.teocci.virtualjoystick.interfaces.BluetoothControlListener;
import com.github.teocci.virtualjoystick.interfaces.OnMoveListener;
import com.github.teocci.virtualjoystick.utils.Debug;
import com.github.teocci.virtualjoystick.view.JoystickView;
import com.github.teocci.virtualjoystick.view.StickView;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements BluetoothControlListener
{
    private BluetoothService service = null;

    // Joystick 좌표 계산에 필요
    private final int CENTER_VALUE = 200; // this represent the value of the center
    private final int MAX_RANGE_VALUE = 100; // this represent the maximum value from the center
    private static final int LOOP_INTERVAL = 200; // in milliseconds

    private final byte STX = 0x02;
    private final byte ETX = 0x03;

    private boolean isBTConnected, isSignalOn;  // 블루투스 연결 여부, 탈리신호 on/off 여부
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private CountDownTimer timer;   // Joystick 움직이지 않아도 기본값 프로토콜 전송하기 위한 timer

    private String host;
    private int port;
    private BluetoothControlCore controlCore;   // PC와 연결하기 위한 core

    private NsdManager nsdManager;
    private NsdManager.RegistrationListener registrationListener;

    public String nsdServiceName = "NsdApp";
    public static final String SERVICE_TYPE = "_http._tcp";

    // UI components.
    private Button connectBtn;
    private JoystickView joystickView;
    private StickView stickView;
    private CheckBox signalCheck;
    private TextView ipText;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_CONNECT_DEVICE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindUIComponents();
        bindListeners();

        if ( service == null ) {
            service = new BluetoothService(this);
        }
        if ( !service.getDeviceState() ) {
            killAll();
            return;
        }
        service.enableBluetooth();

        controlCore = new BluetoothControlCore(this);
        controlCore.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        host = getIPAddress();
        port = controlCore.getPort();
        if ( host != null && port > -1 )
            ipText.setText(host + ":" + port);
        initializeRegistrationListener();
    }

    @Override
    protected void onResume()
    {
        if ( isBTConnected ) {
            sendDefaultPosition();
        }
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        stopSendTimer();
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        if ( nsdManager != null && registrationListener != null ) {
            try {
                nsdManager.unregisterService(registrationListener);
                registrationListener = null;
            } catch ( Exception e ) {
                Debug.err(e);
            }
        }
        super.onDestroy();
    }

    private void bindUIComponents()
    {
        connectBtn = (Button)findViewById(R.id.connectBtn);
        joystickView = (JoystickView)findViewById(R.id.joystickView);
        stickView = (StickView)findViewById(R.id.stickView);
        signalCheck = (CheckBox)findViewById(R.id.signalCheck);
        ipText = (TextView)findViewById(R.id.ipText);
    }

    private void bindListeners()
    {
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                service.scanDevice();
            }
        });

        joystickView.setOnMoveListener(new OnMoveListener() {
            @Override
            public void onMove(int angle, int strength)
            {
                // Do whatever you want
                Long rawX = Math.round(Math.cos(Math.toRadians(angle)) * strength * MAX_RANGE_VALUE / 100);
                Long rawY = Math.round(Math.sin(Math.toRadians(angle)) * strength * MAX_RANGE_VALUE / 100);
                final int x = CENTER_VALUE + rawX.intValue();
                final int y = CENTER_VALUE + rawY.intValue();

                updateTextViews(x, y);
            }
            @Override
            public void onHeightMove(int posY, int fixedCenterY) { }
        }, LOOP_INTERVAL);

        stickView.setOnMoveListener(new OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) { }
            @Override
            public void onHeightMove(int posY, int fixedCenterY)
            {
                int z;
                if ( posY > fixedCenterY ) z = 0;
                else if ( posY < fixedCenterY ) z = 1;
                else z = 2;
                sendPosition(200, 200, z, isSignalOn);
            }
        });

        signalCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isSignalOn = isChecked;
                sendPosition(200, 200, 2, isSignalOn);
            }
        });
    }

    public void successConnection()
    {
        isBTConnected = true;
        sendDefaultPosition();
    }

    public void sendDefaultPosition()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                timer = new CountDownTimer(1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) { }
                    @Override
                    public void onFinish() {
                        Debug.err("timer finish");
                        sendPosition(200, 200, 2, isSignalOn);
                    }
                };
//                timer.start();
            }
        });
    }

    public void sendPosition(int x, int y, int z, boolean isOn)
    {
        if ( !isBTConnected ) return;

        Debug.log("x : "+x+", y : "+y+", z : "+z+", isON : "+isOn);
        stopSendTimer();
        try {
            String tmpX = x + "";
            String tmpY = y + "";
            String tmpZ = z + "";
            String tmpOn;
            if ( isOn ) tmpOn = "1";
            else tmpOn = "0";

            outputStream.write(STX);
            outputStream.write(tmpX.getBytes());
            outputStream.write(tmpY.getBytes());
            outputStream.write(tmpOn.getBytes());
            outputStream.write(tmpZ.getBytes());
            outputStream.write(ETX);

            byte[] buf;
            buf = outputStream.toByteArray();
//            printByteArray(buf);
            service.write(buf);

            outputStream.reset();
        } catch ( Exception e ) {
            Debug.err(e);
        }

        sendDefaultPosition();
    }

    public void sendPosition(String str)
    {
        if ( !isBTConnected ) return;

        stopSendTimer();
        try {
            outputStream.write(str.getBytes());

            byte[] buf;
            buf = outputStream.toByteArray();
//            printByteArray(buf);
            service.write(buf);

            outputStream.reset();
        } catch ( Exception e ) {
            Debug.err(e);
        }

        sendDefaultPosition();
    }

    public void stopSendTimer()
    {
        if ( timer != null ) {
            timer.cancel();
            timer = null;
        }
    }

    public void stopConnection()
    {
        stopSendTimer();
        isBTConnected = false;
    }

    public void updateTextViews(final int x, final int y)
    {
        final TextView locationText = (TextView)findViewById(R.id.locationText);
        sendPosition(x, y, 2, isSignalOn);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                locationText.setText("("+x+", "+y+")");
            }
        });
    }

    private String getIPAddress()
    {
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    private void initializeRegistrationListener()
    {
        nsdManager = (NsdManager)getSystemService(Context.NSD_SERVICE);

        registrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                nsdServiceName = NsdServiceInfo.getServiceName();

                Debug.log("nd : "+nsdServiceName);
                Debug.log("host : "+NsdServiceInfo.getHost()+", port : "+NsdServiceInfo.getPort());
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) { }
            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) { }
            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) { }
        };

        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(nsdServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);

        Debug.log("service : "+serviceInfo.getPort());

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        Debug.err("REQ:" + requestCode + ",RET:" + resultCode + ",DATA:" + intent);

        switch ( requestCode ) {
            case REQUEST_ENABLE_BT :
                if ( resultCode != RESULT_OK ) {
                    killAll();
                }
                break;
            case REQUEST_CONNECT_DEVICE :
                if ( resultCode == RESULT_OK ) {
                    service.getDeviceInfo(intent);
                }
                break;
        }
    }

    @Override
    public void receive(final String str)
    {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                ipText.setText(str);
//            }
//        });
        sendPosition(str);
    }

    private void killAll()
    {
        if ( service != null ) {
            service.stop();
            service = null;
        }
        if ( controlCore != null ) {
            controlCore.close();
            controlCore = null;
        }
        if ( nsdManager != null && registrationListener != null ) {
            try {
                nsdManager.unregisterService(registrationListener);
                registrationListener = null;
            } catch ( Exception e ) {
                Debug.err(e);
            }
        }
        finish();
        killApplication();
    }

    private void killApplication()
    {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        am.killBackgroundProcesses(getPackageName());

        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onBackPressed()
    {
        killAll();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        Debug.err(getClass(), "::onConfigurationChanged() " + newConfig);

        // 화면전환시 onCreate()가 다시 불리는 것을 방지.
        // 카메라 사용시 화면전환이 되는 디바이스들이 많은데 이 때 변수가 모두 초기화됨.
        // 이를 방지해야함.
        // SDK v13 이후로 configChanges="orientation|screenSize" 처럼.
        // screenSize를 설정하지 않으면 이 함수가 불리지 않고 onCreate가 불릴 수 있음을 주의.
        // 이것때문에 삽질 ㅡㅡ
    }
}