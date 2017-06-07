package com.github.teocci.virtualjoystick.core;

import android.content.Context;
import android.os.AsyncTask;

import com.github.teocci.virtualjoystick.interfaces.BluetoothControlListener;
import com.github.teocci.virtualjoystick.utils.Debug;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class BluetoothClientThread extends AsyncTask<Void, Void, Void>
{
    private Context context;
    private Socket clientSocket;
    private BufferedReader input;

    public BluetoothClientThread(Context context, Socket socket)
    {
        this.context = context;
        this.clientSocket = socket;

        try {
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch ( Exception e ) {
            Debug.err(e);
        }
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        try {
            while ( !Thread.currentThread().isInterrupted() ) {
                String str = input.readLine();
                Debug.log("S: Received: '" + str + "'");
                if ( str == null ) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if ( context instanceof BluetoothControlListener ) {
                    ((BluetoothControlListener)context).receive(str);
                }
            }
        } catch ( Exception e ) {
            Debug.err(e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void param)
    {
        try {
            if ( clientSocket != null && !clientSocket.isClosed() ) {
                clientSocket.close();
            }
        } catch ( Exception e ) {
            Debug.err(e);
        }
    }
}