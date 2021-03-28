package com.aravinth.cochat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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

import java.security.Permission;
import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private final static int REQUEST_CHECK_SETTING = 1001;
    private final static int SELECT_DEVICE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        context = this;
        initBluetooth();
    }

    private boolean isChecked = false;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem checkable = menu.findItem(R.id.dark_theme);
        checkable.setChecked(isChecked);
        return true;
    }

    private void initBluetooth()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null)
        {
            Toast.makeText(context,"No Bluetooth found!",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_activity,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.menu_search_devices:
                //checkPermissions();
                //checkAllPermissions();
                locationPermission();
                Intent intent = new Intent(context,DeviceListActivity.class);
                startActivityForResult(intent,SELECT_DEVICE);
                Toast.makeText(context,"Clicked search devices",Toast.LENGTH_SHORT).show();
                return true;

            case R.id.menu_enable_bluetooth:
                enableBluetooth();
                Toast.makeText(context,"Clicked enable bluetooth",Toast.LENGTH_SHORT).show();
                return true;

            case R.id.dark_theme:
                isChecked = !item.isChecked();
                item.setChecked(isChecked);
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }

    }

//    private void checkAllPermissions() {
//        if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
//        {
//            Log.d(TAG,"Fine location permission granted");
//        }
//        if(ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED)
//        {
//            Log.d(TAG,"Bluetooth location permission granted");
//        }
//        if(ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED)
//        {
//            Log.d(TAG,"Bluetooth admin location permission granted");
//        }
//        if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
//        {
//            Log.d(TAG,"Coarse location permission granted");
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(requestCode == SELECT_DEVICE && resultCode == RESULT_OK)
        {

            String address = data.getStringExtra("deviceAddress");
            Log.d(TAG,address);
            Toast.makeText(context,address,Toast.LENGTH_SHORT).show();
        }

        if(requestCode == REQUEST_CHECK_SETTING)
        {
            switch (resultCode)
            {
                case Activity.RESULT_OK:
                    Toast.makeText(context,"Location turned on",Toast.LENGTH_SHORT).show();
                    break;

                case Activity.RESULT_CANCELED:
                    Toast.makeText(context,"Location has to be enabled",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void enableBluetooth()
    {
        if(!bluetoothAdapter.isEnabled())
        {
            Toast.makeText(context,"Bluetooth is getting ON",Toast.LENGTH_SHORT).show();
            bluetoothAdapter.enable();
        }
        if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
        {
            Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivityForResult(discoverIntent,SELECT_DEVICE);
        }
    }

//    private void checkPermissions()
//    {
//        if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
//        {
//            ActivityCompat.requestPermissions(HomeActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
//        }
//        else
//        {
//            Intent intent = new Intent(context,DeviceListActivity.class);
//            startActivityForResult(intent,SELECT_DEVICE);
//        }
//    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if(LOCATION_REQUEST_CODE == requestCode)
//        {
//            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
//            {
//                Toast.makeText(context,"Permission Granted Successfully",Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent(context,DeviceListActivity.class);
//                startActivityForResult(intent,SELECT_DEVICE);
//
//            }
//            else
//            {
//                new AlertDialog.Builder(context)
//                        .setCancelable(true)
//                        .setMessage("Location Permission is Required\nPlease Grant")
//                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                checkPermissions();
//                            }
//                        })
//                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                HomeActivity.this.finish();
//                            }
//                        })
//                        .show();
//            }
//        }
//        else
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }
private void locationPermission()
{
    LocationRequest locationRequest = LocationRequest.create();
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    locationRequest.setInterval(5000);
    locationRequest.setFastestInterval(2000);

    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest);
    builder.setAlwaysShow(true);

    Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
            .checkLocationSettings(builder.build());

    result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
        @Override
        public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
            try {
                LocationSettingsResponse response = task.getResult(ApiException.class);
                Toast.makeText(context,"Location is On",Toast.LENGTH_SHORT).show();
            } catch (ApiException e) {
                switch (e.getStatusCode())
                {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException)e;
                            resolvableApiException.startResolutionForResult(HomeActivity.this,REQUEST_CHECK_SETTING);
                        } catch (IntentSender.SendIntentException sendIntentException) {

                        }
                        break;

                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        }
    });

}

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(requestCode == REQUEST_CHECK_SETTING)
//        {
//            switch (resultCode)
//            {
//                case Activity.RESULT_OK:
//                    Toast.makeText(context,"Location turned on",Toast.LENGTH_SHORT).show();
//                    break;
//
//                case Activity.RESULT_CANCELED:
//                    Toast.makeText(context,"Location has to be enabled",Toast.LENGTH_SHORT).show();
//                    break;
//            }
//        }
//    }
}

