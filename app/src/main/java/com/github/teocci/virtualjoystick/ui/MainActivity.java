package com.github.teocci.virtualjoystick.ui;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.teocci.virtualjoystick.BluetoothService;
import com.github.teocci.virtualjoystick.interfaces.OnMoveListener;
import com.github.teocci.virtualjoystick.utils.Debug;
import com.github.teocci.virtualjoystick.R;
import com.github.teocci.virtualjoystick.view.JoystickView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity
{
    public static final String TAG = MainActivity.class.getSimpleName();

    private final int CENTER_VALUE = 200; // this represent the value of the center
    private final int MAX_RANGE_VALUE = 100; // this represent the maximum value from the center
    private static final int LOOP_INTERVAL = 200; // in milliseconds

    private final byte STX = 0x02;
    private final byte ETX = 0x03;

    private BluetoothService service = null;
    private final Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
        }
    };

    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_CONNECT_DEVICE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (service == null) {
            service = new BluetoothService(this, handler);
        }

        Button connectBtn = (Button) findViewById(R.id.connectBtn);
        connectBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                service.scanDevice();
            }
        });

        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new OnMoveListener()
        {
            @Override
            public void onMove(int angle, int strength)
            {
                // Do whatever you want
                Long rawX = Math.round(Math.cos(Math.toRadians(angle)) * strength * MAX_RANGE_VALUE / 100);
                Long rawY = Math.round(Math.sin(Math.toRadians(angle)) * strength * MAX_RANGE_VALUE / 100);
                final int x = CENTER_VALUE + rawX.intValue();
                final int y = CENTER_VALUE + rawY.intValue();

                updateTextViews(angle, strength, x, y);
            }
        }, LOOP_INTERVAL);

        if (!service.getDeviceState()) {
            killAll();
            return;
        }
        service.enableBluetooth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        Debug.err("REQ:" + requestCode + ",RET:" + resultCode + ",DATA:" + intent);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode != RESULT_OK) {
                    killAll();
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == RESULT_OK) {
                    service.getDeviceInfo(intent);
                }
                break;
        }
    }

    public void sendPosition(int x, int y)
    {
        try {
            String tmpX = x + "";
            String tmpY = y + "";
            byte[] buf;

            outputStream.write(STX);
            outputStream.write(tmpX.getBytes());
            outputStream.write(tmpY.getBytes());
            outputStream.write(ETX);

            buf = outputStream.toByteArray();
            printByteArray(buf);
            service.write(buf);

            outputStream.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateTextViews(final int angle, final int strength, final int x, final int y)
    {
        final TextView angleTextView = (TextView) findViewById(R.id.angle_value);
        final TextView strengthTextView = (TextView) findViewById(R.id.strength_value);
        final TextView xTextView = (TextView) findViewById(R.id.x_value);
        final TextView yTextView = (TextView) findViewById(R.id.y_value);
        sendPosition(x, y);

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    angleTextView.setText(angle + "°");
                    strengthTextView.setText(strength + "%");
                    xTextView.setText(x + "");
                    yTextView.setText(y + "");
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printByteArray(final byte[] data)
    {
        byte b;
        for (int i = 0; i < data.length; i++) {
            b = data[i];
            Debug.err(b);
        }
    }

    private void killAll()
    {
        finish();
        killApplication();
    }

    private void killApplication()
    {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        am.killBackgroundProcesses(getPackageName());

        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
