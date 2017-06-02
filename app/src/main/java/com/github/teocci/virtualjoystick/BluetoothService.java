package com.github.teocci.virtualjoystick;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.widget.Toast;

import com.github.teocci.virtualjoystick.ui.DeviceListActivity;
import com.github.teocci.virtualjoystick.ui.MainActivity;
import com.github.teocci.virtualjoystick.utils.Debug;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService
{
    private Activity activity;
    private BluetoothAdapter adapter;

    // Serial connection
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;

    private static final int STATE_NONE = 0;
    private static final int STATE_LISTEN = 1;
    private static final int STATE_CONNECTING = 2;
    private static final int STATE_CONNECTED = 3;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_CONNECT_DEVICE = 2;

    public BluetoothService(Activity activity)
    {
        this.activity = activity;

        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean getDeviceState()
    {
        return adapter != null;
    }

    public void enableBluetooth()
    {
        if ( !adapter.isEnabled() ) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    }

    public void scanDevice()
    {
        Intent intent = new Intent(activity, DeviceListActivity.class);
        activity.startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
    }

    public void getDeviceInfo(Intent data)
    {
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = adapter.getRemoteDevice(address);
        connect(device);
    }

    private synchronized void setState(int state)
    {
        this.state = state;
    }

    private synchronized void start()
    {
        if ( connectThread != null ) {
            connectThread.cancel();
            connectThread = null;
        }
        if ( connectedThread != null ) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    private synchronized void connect(BluetoothDevice device)
    {
        if ( state == STATE_CONNECTING ) {
            if ( connectThread != null ) {
                connectThread.cancel();
                connectThread = null;
            }
        }
        if ( connectedThread != null ) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    private synchronized void connected(BluetoothSocket socket)
    {
        if ( connectThread != null ) {
            connectThread.cancel();
            connectThread = null;
        }
        if ( connectedThread != null ) {
            connectedThread.cancel();
            connectedThread = null;
        }
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        setState(STATE_CONNECTED);
    }

    public synchronized void stop()
    {
        if ( connectThread != null ) {
            connectThread.cancel();
            connectThread = null;
        }
        if ( connectedThread != null ) {
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_NONE);
    }

    public void write(byte[] out)
    {
        Debug.log("write");
        ConnectedThread r;
        synchronized ( this ) {
            if ( state != STATE_CONNECTED )
                return;
            r = connectedThread;
            r.write(out);
        }
    }

    public void connectionFailed()
    {
        setState(STATE_LISTEN);
    }

    public void connectionLost()
    {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, "연결이 끊겼습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        ((MainActivity)activity).stopConnection();
        setState(STATE_LISTEN);
    }

    private class ConnectThread extends Thread
    {
        private BluetoothSocket bluetoothSocket;

        public ConnectThread(BluetoothDevice device)
        {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch ( Exception e ) {
                Debug.err(e);
            }
        }

        public void run()
        {
            adapter.cancelDiscovery();
            try {
                bluetoothSocket.connect();
            } catch ( Exception e ) {
                Debug.err(e);
                connectionFailed();
                cancel();
                BluetoothService.this.start();
                return;
            }

            synchronized ( BluetoothService.this ) {
                connectThread = null;
            }
            connected(bluetoothSocket);
        }

        public void cancel()
        {
            try {
                bluetoothSocket.close();
            } catch ( Exception e ) {
                Debug.err(e);
            }
        }
    }

    private class ConnectedThread extends Thread
    {
        private BluetoothSocket bluetoothSocket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            this.bluetoothSocket = socket;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch ( Exception e ) {
                Debug.err(e);
            }

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Connected", Toast.LENGTH_SHORT).show();
                }
            });
            ((MainActivity)activity).successConnection();
        }

        public void run()
        {
            byte[] buf = new byte[1024];
            while ( true ) {
                Debug.log("socket read");
                try {
                    inputStream.read(buf);
                } catch ( Exception e ) {
                    Debug.err(e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buf)
        {
            try {
                Debug.log("socket send");
                outputStream.write(buf);
            } catch ( Exception e ) {
                Debug.err(e);
            }
        }

        public void cancel()
        {
            try {
                bluetoothSocket.close();
            } catch ( Exception e ) {
                Debug.err(e);
            }
        }
    }
}