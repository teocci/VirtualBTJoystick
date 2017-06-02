package com.github.teocci.virtualjoystick.ui;

import android.content.Context;
import android.os.AsyncTask;

import com.github.teocci.virtualjoystick.utils.Debug;

import java.net.ServerSocket;
import java.net.Socket;

public class BluetoothControlCore extends AsyncTask<Void, Void, Void>
{
    private Context context;

    private ServerSocket serverSocket;
    private int port = 7777;

    public BluetoothControlCore(Context context)
    {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        connect();
        return null;
    }

    private void connect()
    {
        try {
            serverSocket = new ServerSocket(port);

            while ( !Thread.currentThread().isInterrupted() ) {
                Debug.log("connect");
                Socket client = serverSocket.accept();
                Debug.log("S: Receiving...");

                CommunicationThread commuThread = new CommunicationThread(context, client);
                commuThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } catch ( Exception e ) {
            Debug.err(e);
        }
    }

    public int getPort()
    {
        if ( serverSocket != null )
            return serverSocket.getLocalPort();
        return -1;
    }

    public void close()
    {
        try {
            if ( serverSocket != null && !serverSocket.isClosed() )
                serverSocket.close();
        } catch ( Exception e ) {
            Debug.err(e);
        }
    }
}
