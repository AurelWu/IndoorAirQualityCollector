package com.example.indoorairqualitycollector;

public class Logic {

    public AranetManager aranetManager;
    public BluetoothManager bluetoothManager;
    public SpatialManager spatialManager;

    public SubmissionData submissionData;


    public Logic(MainActivity mainActivity) {
        // Initialize your data here
        bluetoothManager = new BluetoothManager(mainActivity);
        spatialManager = new SpatialManager(mainActivity);
        aranetManager = new AranetManager(bluetoothManager,mainActivity);
    }

    public void StartNewRecording(long nwrID, String nwrName, double nwrLat, double nwrLon, long startTime)
    {
        submissionData = new SubmissionData(aranetManager.aranetMAC, nwrID,nwrName,nwrLat,nwrLon,startTime);
        aranetManager.StartNewRecording(); //we dont' provide full submissionData to aranetManager and rather collect its Data once FinishRecording is called from here

    }
    public void FinishRecording(boolean windowDoorState,boolean ventilationSystem, String occupancy, String notes)
    {
        submissionData.sensorData = aranetManager.sensorData;
        submissionData.openWindowsDoors = windowDoorState;
        submissionData.ventilationSystem = ventilationSystem;
        submissionData.OccupancyLevel = occupancy;
        submissionData.AdditionalNotes = notes;
        aranetManager.FinishRecording();
    }

    public String GenerateJSONToTransmit()
    {
        String jsonToSubmit = submissionData.toJson();
        return jsonToSubmit;
    }
}