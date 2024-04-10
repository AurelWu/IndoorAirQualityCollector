package com.example.indoorairqualitycollector;

import java.util.List;

public class SubmissionData
{
    public String sensorID;
    public long nwrID;
    public String nwrName;
    public double nwrLatitude;
    public double nwrLongitude;
    public long startTime;
    public List<SensorData> sensorData;
    public boolean openWindowsDoors;
    public boolean ventilationSystem;
    public String OccupancyLevel;
    public String AdditionalNotes;

    public SubmissionData(String sensorID, long nwrID, String nwrName, double nwrLat, double nwrLon, long startTime)
    {
        this.sensorID = sensorID;
        this.nwrID = nwrID;
        this.nwrName= nwrName;
        this.startTime = startTime;
        this.nwrLatitude = nwrLat;
        this.nwrLongitude = nwrLon;
    }
}
