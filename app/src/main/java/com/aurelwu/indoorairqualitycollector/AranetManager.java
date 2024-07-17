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
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AranetManager {
    private MainActivity mainActivity;
    private BluetoothManager bluetoothManager;

    private BluetoothLeScanner bluetoothLeScanner;
    private static final UUID ARANET_SERVICE_UUID = UUID.fromString("0000FCE0-0000-1000-8000-00805f9b34fb");
    private static final UUID ARANET_LIVE_CHARACTERISTIC_UUID = UUID.fromString("f0cd3001-95da-4f4b-9ac8-aa55d312af0c");

    private static final UUID ARANET_TOTAL_READINGS_CHARACTERISTIC_UUID = UUID.fromString("f0cd2001-95da-4f4b-9ac8-aa55d312af0c");
    private static final UUID ARANET_WRITE_CHARACTERISTIC_UUID = UUID.fromString("f0cd1402-95da-4f4b-9ac8-aa55d312af0c");
    private static final UUID ARANET_HISTORY_V2_CHARACTERISTIC_UUID = UUID.fromString("f0cd2005-95da-4f4b-9ac8-aa55d312af0c");

    BluetoothGattCharacteristic liveDataCharacteristic;
    BluetoothGattCharacteristic totalDataPointsCharacteristic;
    BluetoothGattCharacteristic writeCharacteristic;
    BluetoothGattCharacteristic historyV2Characteristic;

    public BluetoothDevice aranetDevice;
    public String aranetMAC;
    public String aranetDeviceName;

    public int rssi = 0;
    public int txPower = 0;

    public int UpdateInterval = 9999;
    private boolean foundDevice = false;
    public boolean isRecording = false;
    public long timeOfRecordingStart;
    short elapsedMinutes;
    short startidx;

    public boolean GattModeIsA2DP = false;

    public boolean lastUpdateFailed = false;
    public int failureID = 0;
    public String GattStatus = "-";

    public int totalDataPoints = 0;

    public List<SensorData> sensorData;
    public SensorData currentReading;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    public ArrayList<BluetoothGatt> usedGatts; //we keep track of every opened gatt to ensure we don't miss closing ones

    public AranetManager(BluetoothManager bluetoothManager, MainActivity mainActivity) {
        this.bluetoothManager = bluetoothManager;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mainActivity = mainActivity;
        this.usedGatts = new ArrayList<>();
    }

    public void StartNewRecording() {
        isRecording = true;
        sensorData = new ArrayList<>();

        timeOfRecordingStart = System.currentTimeMillis(); //Unix Epoch
    }

    public void FinishRecording() {
        isRecording = false;
    }

    public void Update() {
        CleanupGatts();
        Log.d("AranetManager", "Update Called");
        foundDevice = false;
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(ARANET_SERVICE_UUID)) // Filter by service UUID
                .build();

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Set scan mode
                .build();

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 and above
            if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        } else {
            // Android 11 and below
            if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        bluetoothLeScanner.startScan(Collections.singletonList(scanFilter), scanSettings, scanCallback);
        new Handler(Looper.getMainLooper()).postDelayed(() -> bluetoothLeScanner.stopScan(scanCallback), 15000);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        public void onScanResult(int callbackType, ScanResult result) {

            //Log.d("AranetManager", "Scancallback called");
            BluetoothDevice device = result.getDevice();
            rssi = result.getRssi();
            txPower = result.getTxPower();
            String targetDeviceID = UserIDManager.loadTargetDeviceID(mainActivity);
            if(targetDeviceID.length()>1)
            {
                if(!device.getAddress().equals(targetDeviceID))
                {
                    Log.d("DeviceTargetID","Device ID does not match");
                    return;
                }
            }

            if (device == null && aranetDevice != null) //we didnt find it this time but we already had it before and use it again
            {
                device = aranetDevice;
            }
            Log.d("AranetManager", "Scancallback called | DeviceID: " + device.getAddress());
            if (device != null && foundDevice != true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12 and above
                    if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                } else {
                    // Android 11 and below
                    if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                bluetoothLeScanner.stopScan(this);
                foundDevice = true;
                aranetDevice = device;
                aranetMAC = device.getAddress();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12 and above
                    if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                } else {
                    // Android 11 and below
                    // No specific permission check required for Bluetooth connect
                }
                aranetDeviceName = device.getName();
                connectToDevice();
            }
        }
    };

    public void connectToDevice() {
        Log.d("AranetManager", "connectToDevice called");
        if (aranetDevice == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 and above
            if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        } else {
            // Android 11 and below
            // No specific permission check required for Bluetooth connect
        }

        bluetoothGatt = aranetDevice.connectGatt(mainActivity, false, gattCallback);
        usedGatts.add(bluetoothGatt);
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
                    totalDataPoints = 0; //we reset this value to make sure we don't have outdated size
                    lastUpdateFailed = false;
                    failureID = 0;
                    List<BluetoothGattCharacteristic> x = service.getCharacteristics();
                    liveDataCharacteristic = service.getCharacteristic(ARANET_LIVE_CHARACTERISTIC_UUID);
                    totalDataPointsCharacteristic = service.getCharacteristic(ARANET_TOTAL_READINGS_CHARACTERISTIC_UUID);
                    writeCharacteristic = service.getCharacteristic(ARANET_WRITE_CHARACTERISTIC_UUID);
                    historyV2Characteristic = service.getCharacteristic(ARANET_HISTORY_V2_CHARACTERISTIC_UUID);
                    Log.d("onServicesDiscovered", "characteristic: " + liveDataCharacteristic.getUuid());
                    if (liveDataCharacteristic != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // Android 12 and above
                            if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                        } else {
                            // Android 11 and below
                            // No specific permission check required for Bluetooth connect
                        }
                        gatt.readCharacteristic(liveDataCharacteristic);
                        //gatt.readCharacteristic(totalDataPointsCharacteristic);

                        //elapsedTime since start of Recording


                        //gatt.writeCharacteristic(writeCharacteristic,MYBYTE,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    }
                }


            }


        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d("onCharacteristicRead", "onCharacteristicRead called");
            Log.d("onCharacteristicRead", "status: " + status);
            //Log.d("onCharacteristicRead", "onCharacteristicRead called");
            //Log.d("nCharacteristicRead/Gatt_status", "status: " + status);

            String uuid = characteristic.getUuid().toString();

            GattStatus = String.valueOf(status);
            if (status == BluetoothGatt.A2DP) {
                GattModeIsA2DP = true;
            } else {
                GattModeIsA2DP = false;
            }


            if (uuid.equals("f0cd3001-95da-4f4b-9ac8-aa55d312af0c") && status == BluetoothGatt.GATT_SUCCESS) //live Characteristic
            {
                //Log.d("onCharacteristicRead", "now reading data");
                byte[] data = characteristic.getValue();

                if (data != null && data.length >= 10) {
                    // Convert byte array to desired values
                    int valueCO2 = (data[1] << 8) | (data[0] & 0xFF);
                    int valueTemp = (data[3] << 8) | (data[2] & 0xFF);
                    int valuePressure = (data[5] << 8) | (data[4] & 0xFF);
                    int valueHumidity = data[6] & 0xFF;
                    int valueUpdateInterval = (data[10] << 8) | (data[9] & 0xFF);
                    //DONE TODO: IF UPDATEINTERVAL IS NOT 60 then demand user to change it to 60s! (or maybe we can also change it programmatically after asking user for permission?";
                    // Display values in TextView
                    UpdateInterval = valueUpdateInterval;
                    currentReading = new SensorData(valueCO2, 0);
                    //if (isRecording) {
                    //    long timeStamp = System.currentTimeMillis();
                    //    sensorData.add(new SensorData(valueCO2, timeStamp));
                    //}
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12 and above
                    if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        gatt.disconnect();
                        gatt.close();
                        return;
                    }
                }
                if (isRecording) {
                    gatt.readCharacteristic(totalDataPointsCharacteristic);
                }
            } else if (uuid.equals("f0cd2001-95da-4f4b-9ac8-aa55d312af0c") && status == BluetoothGatt.GATT_SUCCESS) //total Readings characterisic
            {
                byte[] data = characteristic.getValue();
                if (data != null && data.length == 2) {
                    totalDataPoints = (data[1] << 8) | (data[0] & 0xFF);
                }
                else
                {
                    lastUpdateFailed = false;
                    failureID = 1;
                }

                Log.d("WriteCharacteristic", "TotalDataPoints: " + String.valueOf(totalDataPoints));
                if(totalDataPoints!=0)
                {
                    long timeDifferenceLocation = System.currentTimeMillis() - timeOfRecordingStart;
                    elapsedMinutes = (short) ((timeDifferenceLocation + 59999) / 60000); // Adding 59999 to ensure rounding up
                    startidx = (short) (totalDataPoints - (0 + elapsedMinutes)); //change value to start with a bit of pre-recording history
                    if (startidx < 0)
                    {
                        startidx = 0;
                    }

                    writeCharacteristic.setValue(DataArrayBuilder.packDataRequestCO2History(startidx));
                    gatt.writeCharacteristic(writeCharacteristic);
                }
                else
                {
                    lastUpdateFailed = true;
                    failureID = 2;
                }


            }
            //else if(uuid.equals("f0cd1402-95da-4f4b-9ac8-aa55d312af0c") && status == BluetoothGatt.GATT_SUCCESS)
            //{
            //    byte[] answer = characteristic.getValue(); //should just be a success flag - 0 for success? add check later
            //    gatt.readCharacteristic(historyV2Characteristic);
            //}

            else if (uuid.equals("f0cd2005-95da-4f4b-9ac8-aa55d312af0c") && status == BluetoothGatt.GATT_SUCCESS) //history V2 Characteristic
            {
                byte[] historyDataRaw = characteristic.getValue();
                ByteBuffer buffer = ByteBuffer.wrap(historyDataRaw);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                Log.d("HistoryV2Characteristic", "historyDataRawByteSize: " + String.valueOf(historyDataRaw.length));
                if (historyDataRaw.length < 10) {
                    gatt.disconnect();
                    gatt.close();
                    return;
                }
                byte paramID = historyDataRaw[0];
                short interval = buffer.getShort(1);
                short totalReadings = buffer.getShort(3);
                short ago = buffer.getShort(5);
                short startIndex = buffer.getShort(7);
                byte count = historyDataRaw[9];

                Log.d("HistoryV2Characteristic", "startIndexAsInAranetMessage: " + String.valueOf(startIndex));
                Log.d("HistoryV2Characteristic", "startIdxSetBeforeByUs: " + startidx);
                Log.d("HistoryV2Characteristic", "Data Count: " + startidx);

                if(count== 0)
                {
                    Log.d("HistoryV2Characteristic", "Data Count: " + startidx + "| skipping updating Data array");
                    failureID = 3;
                    gatt.disconnect();
                    gatt.close();
                    return;
                }

                int[] co2dataArray = new int[count];
                for (int i = 0; i < count; i++) {
                    co2dataArray[i] = (buffer.getShort(10 + (i * 2)));
                }

                //not sure if that can cause issues if UI updates (other thread?) between clearing and newly adding
                sensorData.clear();
                for (int i = 0; i < count; i++) {
                    sensorData.add(new SensorData(co2dataArray[i], i));
                }


                //put at end here
                gatt.disconnect();
                gatt.close();
            } else {
                gatt.disconnect();
                gatt.close();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String uuid = characteristic.getUuid().toString();
            if (uuid.equals("f0cd1402-95da-4f4b-9ac8-aa55d312af0c") && status == BluetoothGatt.GATT_SUCCESS) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12 and above
                    if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        gatt.disconnect();
                        gatt.close();
                        return;
                    }
                }

                byte[] answer = characteristic.getValue(); //should just be a success flag - 0 for success? add check later
                gatt.readCharacteristic(historyV2Characteristic);
                System.out.println("Characteristic written successfully: " + characteristic.getUuid());
            } else {
                // Write failed
                System.out.println("Characteristic write failed: " + characteristic.getUuid() + ", status: " + status);
                lastUpdateFailed = true;
                failureID = 5;
            }
        }
    };

    public int[] GetCO2Data() {
        int[] co2Values = new int[sensorData.size()];

        for (int i = 0; i < sensorData.size(); i++) {
            co2Values[i] = sensorData.get(i).CO2ppm;
        }

        return co2Values;
    }

    private void CleanupGatts() {
        ArrayList<BluetoothGatt> cleanedUpGatts = new ArrayList<>();
        for (int i = 0; i < usedGatts.size(); i++) {
            BluetoothGatt gatt = usedGatts.get(i);
            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                continue;
            }
            gatt.disconnect();
            gatt.close();
            cleanedUpGatts.add(gatt);
        }
        usedGatts.removeAll(cleanedUpGatts);

    }



}
