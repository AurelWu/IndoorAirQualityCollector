package com.aurelwu.indoorairqualitycollector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AranetManager {
    private MainActivity mainActivity;
    private BluetoothManager bluetoothManager;

    private BluetoothLeScanner bluetoothLeScanner;
    private static final UUID ARANET_SERVICE_UUID = UUID.fromString("0000FCE0-0000-1000-8000-00805f9b34fb");
    private static final UUID ARANET_CHARACTERISTIC_UUID = UUID.fromString("f0cd3001-95da-4f4b-9ac8-aa55d312af0c");
    public BluetoothDevice aranetDevice;
    public String aranetMAC;
    public String aranetDeviceName;

    public int UpdateInterval = 9999;
    private boolean foundDevice = false;
    public boolean isRecording = false;

    public boolean GattModeIsA2DP = false;


    public List<SensorData> sensorData;
    public SensorData currentReading;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;


    public AranetManager(BluetoothManager bluetoothManager, MainActivity mainActivity) {
        this.bluetoothManager = bluetoothManager;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mainActivity = mainActivity;

    }

    public void StartNewRecording() {
        isRecording = true;
        sensorData = new ArrayList<>();
    }

    public void FinishRecording() {
        isRecording = false;
    }

    public void Update() {
        Log.d("AranetManager", "Update Called");
        foundDevice = false;
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(ARANET_SERVICE_UUID)) // Filter by service UUID
                .build();

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Set scan mode
                .build();

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothLeScanner.startScan(Collections.singletonList(scanFilter), scanSettings, scanCallback);
        new Handler(Looper.getMainLooper()).postDelayed(() -> bluetoothLeScanner.stopScan(scanCallback), 15000);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        public void onScanResult(int callbackType, ScanResult result) {

            //Log.d("AranetManager", "Scancallback called");
            BluetoothDevice device = result.getDevice();
            Log.d("AranetManager", "Scancallback called | DeviceID: " + device.getAddress());
            if (device != null && foundDevice != true) {
                if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothLeScanner.stopScan(this);
                foundDevice = true;
                aranetDevice = device;
                aranetMAC = device.getAddress();
                if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                {
                    return;
                }
                aranetDeviceName = device.getName();
                connectToDevice();
            }
        }
    };

    public void connectToDevice() {
        Log.d("AranetManager", "connectToDevice called");
        if (aranetDevice == null) return;
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt = aranetDevice.connectGatt(mainActivity, false, gattCallback);
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Handle disconnection
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d("onServicesDiscovered", "onServicesDiscovered called");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(ARANET_SERVICE_UUID);
                Log.d("onServicesDiscovered", "service UUID: " + service.getUuid());
                if (service != null) {
                    List<BluetoothGattCharacteristic> x = service.getCharacteristics();
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(ARANET_CHARACTERISTIC_UUID);
                    Log.d("onServicesDiscovered", "characteristic: " + characteristic.getUuid());
                    if (characteristic != null) {
                        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            Log.d("onServicesDiscovered", "missing Permission");
                            return;
                        }
                        gatt.readCharacteristic(characteristic);
                    }
                }
            }


        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d("onCharacteristicRead", "onCharacteristicRead called");
            Log.d("onCharacteristicRead", "status: " + status);
            //Log.d("onCharacteristicRead", "onCharacteristicRead called");
            //Log.d("nCharacteristicRead/Gatt_status", "status: " + status);

            if(status == BluetoothGatt.A2DP)
            {
                GattModeIsA2DP=true;
            }
            else
            {
                GattModeIsA2DP=false;
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Log.d("onCharacteristicRead", "now reading data");
                byte[] data = characteristic.getValue();

                if (data != null && data.length >= 10) {
                    // Convert byte array to desired values
                    int valueCO2 = (data[1] << 8) | (data[0] & 0xFF);
                    int valueTemp = (data[3] << 8) | (data[2] & 0xFF);
                    int valuePressure = (data[5] << 8) | (data[4] & 0xFF);
                    int valueHumidity = data[6] & 0xFF;
                    int valueUpdateInterval = (data[10] <<8) | (data[9]  & 0xFF);
                    //DONE TODO: IF UPDATEINTERVAL IS NOT 60 then demand user to change it to 60s! (or maybe we can also change it programmatically after asking user for permission?";
                    // Display values in TextView
                    UpdateInterval = valueUpdateInterval;
                    currentReading = new SensorData(valueCO2,0);
                    if (isRecording) {
                        long timeStamp = System.currentTimeMillis();
                        sensorData.add(new SensorData(valueCO2, timeStamp));
                    }
                }
            }
            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            {
                return;
            }
            gatt.disconnect();
            gatt.close();
        }
    };

    public int[] GetCO2Data()
    {
        int[] co2Values = new int[sensorData.size()];

        for (int i = 0; i < sensorData.size(); i++) {
            co2Values[i] = sensorData.get(i).CO2ppm;
        }

        return co2Values;

    }

}
