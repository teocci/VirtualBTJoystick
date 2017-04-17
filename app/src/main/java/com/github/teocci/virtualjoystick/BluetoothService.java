package com.github.teocci.virtualjoystick;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import com.github.teocci.virtualjoystick.ui.DeviceListActivity;
import com.github.teocci.virtualjoystick.utils.Debug;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService
{
    private BluetoothAdapter adapter;
    private Activity activity;

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

    public BluetoothService(Activity activity, Handler handler)
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
        if (!adapter.isEnabled()) {
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

    private synchronized int getState()
    {
        return state;
    }

    private synchronized void start()
    {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    private synchronized void connect(BluetoothDevice device)
    {
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device)
    {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        setState(STATE_CONNECTED);
    }

    public synchronized void stop()
    {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_NONE);
    }

    public void write(byte[] out)
    {
        Debug.log("write");
        ConnectedThread r;
        synchronized (this) {
            if (state != STATE_CONNECTED)
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
        setState(STATE_LISTEN);
    }

    private class ConnectThread extends Thread
    {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ConnectThread(BluetoothDevice device)
        {
            this.device = device;

            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception e) {
                Debug.err(e);
            }
            socket = tmp;
        }

        public void run()
        {
            setName("ConnectThread");

            adapter.cancelDiscovery();

            try {
                socket.connect();
            } catch (Exception e) {
                Debug.err(e);
                connectionFailed();

                try {
                    socket.close();
                } catch (Exception e1) {
                    Debug.err(e1);
                }

                BluetoothService.this.start();
                return;
            }

            synchronized (BluetoothService.this) {
                connectThread = null;
            }

            connected(socket, device);
        }

        public void cancel()
        {
            try {
                socket.close();
            } catch (Exception e) {
                Debug.err(e);
            }
        }
    }

    private class ConnectedThread extends Thread
    {
        private BluetoothSocket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (Exception e) {
                Debug.err(e);
            }
            inputStream = tmpIn;
            outputStream = tmpOut;

            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(activity, "Connected", Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void run()
        {
            byte[] buf = new byte[1024];
            int bytes;

            while (true) {
                Debug.log("4444");
                try {
                    bytes = inputStream.read(buf);
                    Debug.log("232323 :  " + bytes);
                } catch (Exception e) {
                    Debug.err(e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buf)
        {
            try {
                outputStream.write(buf);
            } catch (Exception e) {
                Debug.err(e);
            }
        }

        public void cancel()
        {
            try {
                socket.close();
            } catch (Exception e) {
                Debug.err(e);
            }
        }
    }
}
