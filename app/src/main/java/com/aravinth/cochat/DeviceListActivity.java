package com.aravinth.cochat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private ListView listPairedDevices,listAvailableDevices;
    private ArrayAdapter<String> adapterPairedDevices,adapterAvailableDevices;
    private ProgressBar progressScanDevices;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private static final String TAG = "DeviceListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        context = this;

        init();
    }

    private void init()
    {
        progressScanDevices = findViewById(R.id.progress_scan_devices);
        listPairedDevices = findViewById(R.id.list_paired_device);
        listAvailableDevices = findViewById(R.id.list_available_device);
        adapterPairedDevices = new ArrayAdapter<>(context,R.layout.list_devices);
        adapterAvailableDevices = new ArrayAdapter<>(context,R.layout.list_devices);
        listPairedDevices.setAdapter(adapterPairedDevices);
        listAvailableDevices.setAdapter(adapterAvailableDevices);

        listAvailableDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView)view).getText().toString();
                //String address = info;
                Log.d(TAG,"On activity result");
                Intent intent = getIntent();
                intent.putExtra("deviceAddress",info);
                setResult(RESULT_OK,intent);
                finish();

            }
        });

        listPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView)view).getText().toString();
                //String address = info;
                Log.d(TAG,"On activity result");
                Intent intent = getIntent();
                intent.putExtra("deviceAddress",info);
                setResult(RESULT_OK,intent);
                finish();

            }
        });


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if(pairedDevices != null && pairedDevices.size() > 0)
        {
            for(BluetoothDevice device:pairedDevices)
            {
                adapterPairedDevices.add(device.getName()+"\n"+device.getAddress());
            }
        }
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothScanDeviceListener,intentFilter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_devices_list,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.menu_scan_devices:
                //Toast.makeText(context,"Scan devices clicked",Toast.LENGTH_SHORT).show();
                Log.d(TAG,"Clicked scan devices menu");
                //checkPermissions();
                //locationPermission();
                scanDevices();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private BroadcastReceiver bluetoothScanDeviceListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"In broadcast receiver");

            if(BluetoothDevice.ACTION_FOUND.equals(action))
            {
                Log.d(TAG,"Action found");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    adapterAvailableDevices.add(device.getName()+"\n"+device.getAddress());
                    Log.d(TAG,"In new device adding adapter");
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
            {
                Toast.makeText(context,"Broadcast started",Toast.LENGTH_SHORT).show();
                Log.d(TAG,"Broadcast started Discovering");
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                progressScanDevices.setVisibility(View.GONE);
                if(adapterAvailableDevices.getCount() == 0)
                {
                    Toast.makeText(context,"No new devices found",Toast.LENGTH_SHORT).show();
                    Log.d(TAG,"Discovery over ,but no new devices");
                }
                else
                {
                    Toast.makeText(context,"New Devices found, Click on the chat to start conversation",Toast.LENGTH_SHORT).show();
                    Log.d(TAG,"Discovery finished, new devices found");
                }
                Log.d(TAG,"Discovery finished");
            }
        }
    };

    private void scanDevices()
    {
        progressScanDevices.setVisibility(View.VISIBLE);
        Log.d(TAG,"Progress bar started");
        adapterAvailableDevices.clear();
        Toast.makeText(context,"Scan started",Toast.LENGTH_SHORT).show();
        Log.d(TAG,"Cleared available devices adapter");
        if(bluetoothAdapter.isDiscovering())
        {
            Log.d(TAG,"bluetooth adapter is discovering");
            bluetoothAdapter.cancelDiscovery();
            Log.d(TAG,"Cancelling discovery");
        }

        bluetoothAdapter.startDiscovery();
        Toast.makeText(context, "Started Discovering Devices", Toast.LENGTH_SHORT).show();
        Log.d(TAG,"Clicked Start Discovery method");
    }

}