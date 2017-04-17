package com.github.teocci.virtualjoystick.ui;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.github.teocci.virtualjoystick.R;

import java.util.Set;

public class DeviceListActivity extends Activity
{
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter adapter;
    private ArrayAdapter<String> pairedDevicesArrayAdapter;
    private ArrayAdapter<String> newDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        Button scanBtn = (Button)findViewById(R.id.button_scan);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        pairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        newDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

        ListView pairedListView = (ListView)findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.cancelDiscovery();

                String info = pairedDevicesArrayAdapter.getItem(position);
                if ( !info.equals(getString(R.string.none_paired)) ) {
                    String address = info.substring(info.length() - 17);

                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

        ListView newDevicesListView = (ListView)findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(newDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.cancelDiscovery();

                String info = newDevicesArrayAdapter.getItem(position);
                if ( !info.equals(getString(R.string.none_found)) ) {
                    String address = info.substring(info.length() - 17);

                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);

        adapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if ( pairedDevices.size() > 0 ) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for ( BluetoothDevice device : pairedDevices ) {
                pairedDevicesArrayAdapter.add(device.getName()+"\n"+device.getAddress());
            }
        }
        else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if ( adapter != null ) {
            adapter.cancelDiscovery();
        }
        unregisterReceiver(receiver);
    }

    private void doDiscovery()
    {
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        if ( adapter.isDiscovering() ) {
            adapter.cancelDiscovery();
        }
        adapter.startDiscovery();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if ( BluetoothDevice.ACTION_FOUND.equals(action) ) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if ( device.getBondState() != BluetoothDevice.BOND_BONDED ) {
                    newDevicesArrayAdapter.add(device.getName() + "\n"+device.getAddress());
                }
            }
            else if ( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) ) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if ( newDevicesArrayAdapter.getCount() == 0 ) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    newDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };
}
