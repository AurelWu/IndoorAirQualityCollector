package com.aurelwu.indoorairqualitycollector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
import java.util.Set;
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

    public static UUID InkbirdServiceUUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public static UUID InkbirdCO2NotifyCharacteristic = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb");
    public static boolean InkbirdAlreadyHookedUp = false;

    private static final UUID AirvalentServiceUUID = UUID.fromString("B81C94A4-6B2B-4D41-9357-0C8229EA02DF");
    public static UUID AirvalentUpdateIntervalCharacteristic = UUID.fromString("b1c48eea-4f5c-44f7-9797-73e0ce294881");
    public static UUID AirvalentHistoryCharacteristic = UUID.fromString("426d4fa2-50ea-4a8d-b88c-c58b3e78f857");
    public static UUID AirvalentDataChunkCount = UUID.fromString("a6cf90e4-7ec0-46b2-a90a-5c2580f85a43");
    public static UUID AirvalentHistoryPointerCharacteristic = UUID.fromString("cdbde84d-2dc6-46e4-8d6b-f3ababf560aa");
    public static boolean AirvalentFirstHistoryChunkCall = true;
    public static byte[] AirvalentFirstHistoryChunkData;


    BluetoothGattCharacteristic liveDataCharacteristic;
    BluetoothGattCharacteristic totalDataPointsCharacteristic;
    BluetoothGattCharacteristic aranetWriteCharacteristic;
    BluetoothGattCharacteristic historyV2Characteristic;

    BluetoothGattCharacteristic airValentUpdateInterval;
    BluetoothGattCharacteristic airValentHistory;
    BluetoothGattCharacteristic airValentHistoryPointer; //write
    BluetoothGattCharacteristic airValentChunkCounter;


    public BluetoothDevice aranetDevice;
    public String aranetMAC;
    public String aranetDeviceName;

    public boolean prerecording;
    public int prerecordingLength = 15;
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
    public int airvalentChunkCount = 0;

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

    public void StartNewRecording(boolean deleteOldData, boolean prerecording) {
        airvalentChunkCount = 0;
        isRecording = true;
        this.prerecording = prerecording;
        sensorData = new ArrayList<>();
        if (deleteOldData) {
            timeOfRecordingStart = System.currentTimeMillis(); //Unix Epoch
        } else {
            timeOfRecordingStart = MainActivity.recoveryData.startTime;
            Update();
        }

        //MainActivity.recoveryData.startTime=timeOfRecordingStart;

    }

    public void FinishRecording() {
        isRecording = false;
    }

    public void Update() {
        CleanupGatts();
        Log.d("AranetManager", "Update Called");
        foundDevice = false;
        ScanFilter scanFilter = null;
        if (mainActivity.selectedCO2Device.equals("Aranet")) {
            scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(ARANET_SERVICE_UUID)) // Filter by service UUID
                    .build();
        } else if (mainActivity.selectedCO2Device.equals("Airvalent")) {
            scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(AirvalentServiceUUID)) // Filter by service UUID
                    .build();
        } else if (mainActivity.selectedCO2Device.equals("Inkbird IAM-T1")) {
            scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(InkbirdServiceUUID)) // Filter by service UUID
                    .build();
        } else {
            return;
        }


        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Set scan mode
                .build();
        if (mainActivity.selectedCO2Device.equals("Inkbird IAM-T1")) {
            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                String deviceName =device.getName().toLowerCase();
                if(deviceName.contains("iam-t1"))
                {
                    UpdateInterval=60; // if it is higher than unlike other sensors we don't get multiple same readings but fewer, so no need to enforce it, if people are patient they can have higher interval
                    aranetDevice = device;
                    aranetMAC = device.getAddress();
                    break;
                }
            }

            connectToDevice();
            return;
        }

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
        if(!mainActivity.selectedCO2Device.equals("Inkbird IAM-T1"))
        {
            new Handler(Looper.getMainLooper()).postDelayed(() -> bluetoothLeScanner.stopScan(scanCallback), 15000);
        }
        else
        {
            new Handler(Looper.getMainLooper()).postDelayed(() -> bluetoothLeScanner.stopScan(scanCallback), 30000);
        }

    }

    private final ScanCallback scanCallback = new ScanCallback() {
        public void onScanResult(int callbackType, ScanResult result) {

            //Log.d("AranetManager", "Scancallback called");
            BluetoothDevice device = result.getDevice();
            //String debugN = device.getName();
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
            if (status == BluetoothGatt.GATT_SUCCESS && mainActivity.selectedCO2Device.equals("Aranet")) {
                BluetoothGattService service = gatt.getService(ARANET_SERVICE_UUID);
                Log.d("onServicesDiscovered", "service UUID: " + service.getUuid());
                if (service != null) {
                    totalDataPoints = 0; //we reset this value to make sure we don't have outdated size
                    lastUpdateFailed = false;
                    failureID = 0;
                    List<BluetoothGattCharacteristic> x = service.getCharacteristics();
                    liveDataCharacteristic = service.getCharacteristic(ARANET_LIVE_CHARACTERISTIC_UUID);
                    totalDataPointsCharacteristic = service.getCharacteristic(ARANET_TOTAL_READINGS_CHARACTERISTIC_UUID);
                    aranetWriteCharacteristic = service.getCharacteristic(ARANET_WRITE_CHARACTERISTIC_UUID);
                    historyV2Characteristic = service.getCharacteristic(ARANET_HISTORY_V2_CHARACTERISTIC_UUID);
                    Log.d("onServicesDiscovered", "characteristic: " + liveDataCharacteristic.getUuid());
                    if (liveDataCharacteristic != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // Android 12 and above
                            if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                gatt.disconnect();
                                gatt.close();
                                return;
                            }
                        } else {
                            // Android 11 and below
                            // No specific permission check required for Bluetooth connect
                        }
                        gatt.readCharacteristic(liveDataCharacteristic);
                    }
                }
            }
            else if (status == BluetoothGatt.GATT_SUCCESS && mainActivity.selectedCO2Device.equals("Airvalent"))
            {
                BluetoothGattService service = gatt.getService(AirvalentServiceUUID);
                Log.d("onServicesDiscovered", "service UUID: " + service.getUuid());
                if (service != null) {
                    totalDataPoints = 0; //we reset this value to make sure we don't have outdated size
                    lastUpdateFailed = false;
                    failureID = 0;
                    List<BluetoothGattCharacteristic> x = service.getCharacteristics();
                    airValentUpdateInterval = service.getCharacteristic(AirvalentUpdateIntervalCharacteristic);
                    airValentHistory = service.getCharacteristic(AirvalentHistoryCharacteristic);
                    airValentHistoryPointer = service.getCharacteristic(AirvalentHistoryPointerCharacteristic);
                    airValentChunkCounter = service.getCharacteristic(AirvalentDataChunkCount);

                    //totalDataPointsCharacteristic = service.getCharacteristic(ARANET_TOTAL_READINGS_CHARACTERISTIC_UUID);
                    //writeCharacteristic = service.getCharacteristic(ARANET_WRITE_CHARACTERISTIC_UUID);
                    //historyV2Characteristic = service.getCharacteristic(ARANET_HISTORY_V2_CHARACTERISTIC_UUID);
                    //Log.d("onServicesDiscovered", "characteristic: " + liveDataCharacteristic.getUuid());
                    if (airValentUpdateInterval != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // Android 12 and above
                            if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                gatt.disconnect();
                                gatt.close();
                                return;
                            }
                        } else {
                            // Android 11 and below
                            // No specific permission check required for Bluetooth connect
                        }
                        AirvalentFirstHistoryChunkCall = false;
                        AirvalentFirstHistoryChunkData = null;
                        gatt.readCharacteristic(airValentUpdateInterval);
                        // => continue here calling other stuff if needed (
                    }
                }

            }
            else if (status == BluetoothGatt.GATT_SUCCESS && mainActivity.selectedCO2Device.equals("Inkbird IAM-T1"))
            {
                BluetoothGattService service = gatt.getService(InkbirdServiceUUID);
                //if (InkbirdAlreadyHookedUp) return;
                BluetoothGattCharacteristic inkbirdCO2NotifyCharacteristic = service.getCharacteristic(InkbirdCO2NotifyCharacteristic);
                inkbirdCO2NotifyCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                gatt.setCharacteristicNotification(inkbirdCO2NotifyCharacteristic, false);
                inkbirdCO2NotifyCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                // Set up the notification
                gatt.setCharacteristicNotification(inkbirdCO2NotifyCharacteristic, true);
                // Enable notifications
                BluetoothGattDescriptor descriptor = inkbirdCO2NotifyCharacteristic.getDescriptors().get(0);
                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
                //InkbirdAlreadyHookedUp = true;
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

            if(uuid.equals(AirvalentUpdateIntervalCharacteristic.toString()) && status == BluetoothGatt.GATT_SUCCESS)
            {
                byte[] data = characteristic.getValue();
                int interval = data[1] << 8 | (data[0] & 0xFF);
                UpdateInterval = interval;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    // Android 12 and above
                    if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        gatt.disconnect();
                        gatt.close();
                        return;
                    }
                }
                gatt.readCharacteristic(airValentChunkCounter);

            }

            else if(uuid.equals(AirvalentDataChunkCount.toString()) && status == BluetoothGatt.GATT_SUCCESS)
            {
                byte[] data = characteristic.getValue();
                int chunkCount = data[1] << 8 | (data[0] & 0xFF);
                if(chunkCount>0)
                {
                    int response = -999;
                    byte[] msg = AirvalentSetHistoryPointerMsgData();
                    airValentHistoryPointer.setValue(msg);
                    gatt.writeCharacteristic(airValentHistoryPointer);
                    //TODO in OnWriteCharacteristic handle the airvalentHistoryPointer call and then read AirvalentHistoryCharacteristic...
                }
            }
            else if(uuid.equals(AirvalentHistoryCharacteristic.toString()) && status == BluetoothGatt.GATT_SUCCESS)
            {
                long timeDifferenceLocation = System.currentTimeMillis() - timeOfRecordingStart;
                elapsedMinutes = (short) ((timeDifferenceLocation + 59999) / 60000); // Adding 59999 to ensure rounding up
                //TODO if called first time => call a 2nd time and we need to store the data of first call... but clean up before first call
                //TODO if called 2nd Time then just continue with the stuff
                if(AirvalentFirstHistoryChunkCall==true) {
                    AirvalentFirstHistoryChunkCall=false;
                    byte[] data = characteristic.getValue();
                    data = removeFirst8Bytes(data);
                    if (data == null) return;
                    AirvalentFirstHistoryChunkData = data;
                    gatt.readCharacteristic(airValentHistory);
                }
                else
                {
                    byte[] data = characteristic.getValue();
                    data = removeFirst8Bytes(data);
                    if(data == null) return;
                    byte[] combined = concatenateByteArrays(AirvalentFirstHistoryChunkData,data);

                    List<Integer> co2values = new ArrayList<>();

                    for (int i = 0; i < combined.length; i += 8)
                    {
                        byte co2byte2shift = (byte) ((combined[i + 1] << 2) & 0xFF); // Remove first 2 bits
                        co2byte2shift = (byte) ((co2byte2shift & 0xFF) >> 2); // Shift it back

                        byte[] co2bytes = new byte[2];
                        co2bytes[1] = combined[i + 0];
                        co2bytes[0] = co2byte2shift;

                        int co2Value = ByteBuffer.wrap(co2bytes).getShort() & 0xFFFF;
                        co2values.add(co2Value);
                    }
                    int amountOfValuesToTake = elapsedMinutes + 1;
                    if(prerecording)
                    {
                        amountOfValuesToTake = amountOfValuesToTake + prerecordingLength;
                    }
                    if (elapsedMinutes > 120) elapsedMinutes = 120;
                    if (amountOfValuesToTake > co2values.size())
                    {
                        amountOfValuesToTake= co2values.size();
                    }
                    Collections.reverse(co2values);
                    List<Integer> valuesTaken = new ArrayList<>();
                    for(int i = 0; i<amountOfValuesToTake;i++)
                    {
                        valuesTaken.add(co2values.get(i));
                    }
                    Collections.reverse((valuesTaken));
                    if(sensorData!=null)
                    {
                        sensorData.clear();
                    }
                    else
                    {
                        sensorData = new ArrayList<SensorData>();
                    }

                    for(int i = 0; i <valuesTaken.size();i++)
                    {
                        sensorData.add(new SensorData(valuesTaken.get(i), 0));
                    }
                    if (sensorData.size() > 0)
                    {
                        currentReading = sensorData.get(sensorData.size()-1);
                    }
                    MainActivity.recoveryData.timeOfLastUpdate=System.currentTimeMillis();
                    MainActivity.recoveryData.WriteToPreferences(mainActivity);
                    gatt.disconnect();
                    gatt.close();
                }
            }


            else if (uuid.equals("f0cd3001-95da-4f4b-9ac8-aa55d312af0c") && status == BluetoothGatt.GATT_SUCCESS) //aranet live Characteristic
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
                if (isRecording)
                {
                    gatt.readCharacteristic(totalDataPointsCharacteristic);
                }
            }
            else if (uuid.equals("f0cd2001-95da-4f4b-9ac8-aa55d312af0c") && status == BluetoothGatt.GATT_SUCCESS) //total Readings characterisic
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
                    if(!prerecording)
                    {
                        startidx = (short) (totalDataPoints - (0 + elapsedMinutes)); //change value to start with a bit of pre-recording history
                    }
                    else
                    {
                        startidx = (short) (totalDataPoints - (15 + elapsedMinutes)); //change value to start with a bit of pre-recording history
                    }

                    if (startidx < 0)
                    {
                        startidx = 0;
                    }

                    aranetWriteCharacteristic.setValue(DataArrayBuilder.packDataRequestCO2History(startidx));
                    gatt.writeCharacteristic(aranetWriteCharacteristic);
                }
                else
                {
                    lastUpdateFailed = true;
                    failureID = 2;
                }


            }


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

                MainActivity.recoveryData.timeOfLastUpdate=System.currentTimeMillis();
                MainActivity.recoveryData.WriteToPreferences(mainActivity);
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
            }
            else if((uuid.equals(AirvalentHistoryPointerCharacteristic.toString())) && status == BluetoothGatt.GATT_SUCCESS)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12 and above
                    if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        gatt.disconnect();
                        gatt.close();
                        return;
                    }
                }
                byte[] answer = characteristic.getValue(); //should just be a success flag? maybe add check later
                AirvalentFirstHistoryChunkCall=true;
                gatt.readCharacteristic(airValentHistory);
            }
            else
            {
                // Write failed
                System.out.println("Characteristic write failed: " + characteristic.getUuid() + ", status: " + status);
                lastUpdateFailed = true;
                failureID = 5;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(InkbirdCO2NotifyCharacteristic)) {
                onInkbirdCO2CharacteristicValueChanged(characteristic);
            }
        }

        private void onInkbirdCO2CharacteristicValueChanged(BluetoothGattCharacteristic characteristic)
        {
            if (characteristic == null) return;

            byte[] data = characteristic.getValue();
            if (data == null) return;
            if (data.length < 11) return;

            byte fb = data[9];
            byte sb = data[10];
            byte[] c = new byte[]{sb, fb}; // Ensure the order is correct for Little Endian

            // Convert byte array to unsigned short (Java doesn't have unsigned types, so use int)
            int CO2LiveValue = ((c[1] & 0xFF) << 8) | (c[0] & 0xFF);

            // Update current CO2 reading
            currentReading = new SensorData(CO2LiveValue,0);

            // If recording, add to recorded data
            if (isRecording) {
                if(sensorData== null)
                {
                    sensorData = new ArrayList<>();
                }
                sensorData.add(new SensorData(CO2LiveValue, 0));
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

    private void CleanupGatts()
    {
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

    private static byte[] AirvalentSetHistoryPointerMsgData() {
        return new byte[] {(byte) 0xbf, (byte) 0x04};
    }

    public static byte[] removeFirst8Bytes(byte[] data) {
        if (data == null || data.length <= 8) {
            return null;
        }

        // Create a new array with a size 8 bytes less than the original
        byte[] newArray = new byte[data.length - 8];

        // Copy the elements from the 9th byte onwards
        System.arraycopy(data, 8, newArray, 0, newArray.length);

        return newArray;
    }

    public static byte[] concatenateByteArrays(byte[] array1, byte[] array2) {
        // Check if either array is null
        if (array1 == null) array1 = new byte[0];
        if (array2 == null) array2 = new byte[0];

        // Create a new array with the combined length of both arrays
        byte[] concatenatedArray = new byte[array1.length + array2.length];

        // Copy the elements of the first array into the new array
        System.arraycopy(array1, 0, concatenatedArray, 0, array1.length);

        // Copy the elements of the second array into the new array
        System.arraycopy(array2, 0, concatenatedArray, array1.length, array2.length);

        return concatenatedArray;
    }



}
