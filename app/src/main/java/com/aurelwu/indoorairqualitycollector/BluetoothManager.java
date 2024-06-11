package com.aurelwu.indoorairqualitycollector;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

public class BluetoothManager
{
    public MainActivity mainActivity;
    public BluetoothManager(MainActivity mainActivity)
    {
        this.mainActivity = mainActivity;
        RequestBluetoothConnectPermission(123,false);
        RequestBluetoothScanPermission(234,false);
    }
    private BluetoothAdapter bluetoothAdapter;
    public boolean IsBluetoothEnabled() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return false;
        } else return true;
    }

    public void showEnableBluetoothDialog() {
        if (IsBluetoothEnabled()) return; // Bluetooth already enabled, no need to show dialog

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setMessage("Bluetooth is disabled. Do you want to enable it?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Open Bluetooth settings to enable Bluetooth
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // Android 12 and above
                            if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                mainActivity.startActivity(intent);
                            }
                        } else {
                            // Android 11 and below
                            // No specific permission required for Bluetooth connect
                        }

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked "No", do nothing
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public boolean CheckBluetoothConnectPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 and above
            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                //Log.d("AranetManager", "CheckBluetoothConnectPermissions() called and Permission is false");
                return false;
            } else {
                //Log.d("AranetManager", "CheckBluetoothConnectPermissions() called and Permission is true");
                return true;
            }
        } else {
            // Android 11 and below
            // No need to check for BLUETOOTH_CONNECT permission
            //Log.d("AranetManager", "CheckBluetoothConnectPermissions() called and Permission is not required for this Android version");
            return true;
        }
    }

    public boolean CheckBluetoothScanPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 and above
            if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return false;
            } else {
                return true;
            }
        } else {
            // Android 11 and below
            if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false;
            } else {
                return true;
            }
        }
    }

    public void RequestBluetoothConnectPermission(int requestCode, boolean showRequestDialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 and above
            if (CheckBluetoothConnectPermissions()) {
                return; // We already have the connect permission so don't need to request it
            }

            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, requestCode);

            if (showRequestDialog && !CheckBluetoothConnectPermissions()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                builder.setMessage("Bluetooth Connect permission has been denied previously. Do you want to enable it?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Open app settings to enable the permission
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.fromParts("package", mainActivity.getPackageName(), null));
                                mainActivity.startActivity(intent);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked "No", do nothing
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        } else {
            // Android 11 and below do not need BLUETOOTH_CONNECT permission
            // No action needed for these versions
        }
    }

    public void RequestBluetoothScanPermission(int requestCode, boolean showRequestDialog) {
        Log.d("AranetManager", "RequestBluetoothScanPermission called");

        if (CheckBluetoothScanPermissions()) {
            return; // We already have the scan permission, so don't need to request it
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 and above
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.BLUETOOTH_SCAN}, requestCode);
        } else {
            // Android 11 and below
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
        }

        if (showRequestDialog && !CheckBluetoothScanPermissions()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder.setMessage("Bluetooth Scan permission has been denied previously. Do you want to enable it?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Open app settings to enable the permission
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.fromParts("package", mainActivity.getPackageName(), null));
                            mainActivity.startActivity(intent);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked "No", do nothing
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

}
