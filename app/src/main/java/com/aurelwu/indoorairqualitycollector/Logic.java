package com.aurelwu.indoorairqualitycollector;

public class Logic {

    public AranetManager aranetManager;
    public BluetoothManager bluetoothManager;
    public SpatialManager spatialManager;

    public SubmissionData submissionData;
    public SubmissionDataManual submissionDataManual;


    public Logic(MainActivity mainActivity) {
        // Initialize your data here
        bluetoothManager = new BluetoothManager(mainActivity);
        spatialManager = new SpatialManager(mainActivity);
        aranetManager = new AranetManager(bluetoothManager,mainActivity);
    }

    public void StartNewRecording(long nwrID,String nwrType, String nwrName, double nwrLat, double nwrLon, long startTime)
    {
        submissionData = new SubmissionData(UserIDManager.GetEncryptedID(spatialManager.mainActivity, aranetManager.aranetMAC.toString()),nwrType, nwrID,nwrName,nwrLat,nwrLon,startTime);
        aranetManager.StartNewRecording(); //we dont' provide full submissionData to aranetManager and rather collect its Data once FinishRecording is called from here
    }

    public void StartNewManualRecording(long startTime)
    {
        submissionDataManual = new SubmissionDataManual(UserIDManager.GetEncryptedID(spatialManager.mainActivity, aranetManager.aranetMAC.toString()),startTime);
        aranetManager.StartNewRecording();
        //=> manual recording only has startTime initially, it will submit GPS coordinate for every minute as well as CO2 Data and it will have a mandatory Name Field and a optional Address field

    }

    public void FinishRecording(boolean windowDoorState,boolean ventilationSystem, String occupancy, String notes, String manualName, String manualAddress , boolean ManualMode)
    {
        if(!ManualMode)
        {
            submissionData.sensorData = aranetManager.sensorData;
            submissionData.openWindowsDoors = windowDoorState;
            submissionData.ventilationSystem = ventilationSystem;
            submissionData.OccupancyLevel = occupancy;
            submissionData.AdditionalNotes = notes;
        }

        if(ManualMode)
        {
            submissionDataManual.sensorData = aranetManager.sensorData;
            submissionDataManual.openWindowsDoors = windowDoorState;
            submissionDataManual.ventilationSystem = ventilationSystem;
            submissionDataManual.OccupancyLevel = occupancy;
            submissionDataManual.AdditionalNotes = notes;
            submissionDataManual.LocationName = manualName;
            submissionDataManual.LocationAddress = manualAddress;
        }
        aranetManager.FinishRecording();
    }

    public String GenerateJSONToTransmit(int RangeSliderMin, int RangeSliderMax)
    {
        ApiGatewayCaller.manualMode=false;
        String jsonToSubmit = submissionData.toJson(RangeSliderMin,RangeSliderMax);
        return jsonToSubmit;
    }

    public String GenerateManualModeJSONToTransmit(int RangeSliderMin, int RangeSliderMax)
    {
        ApiGatewayCaller.manualMode=true;
        String jsonToSubmit = submissionDataManual.ToJson(RangeSliderMin,RangeSliderMax);
        return jsonToSubmit;
    }
}