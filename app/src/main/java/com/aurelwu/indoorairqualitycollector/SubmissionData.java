package com.aurelwu.indoorairqualitycollector;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class SubmissionData
{
    public String sensorID;

    public String nwrType;
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

    public SubmissionData(String sensorID, String nwrType, long nwrID, String nwrName, double nwrLat, double nwrLon, long startTime)
    {
        this.nwrType = nwrType;
        this.sensorID = sensorID;
        this.nwrID = nwrID;
        this.nwrName= nwrName;
        this.startTime = startTime;
        this.nwrLatitude = nwrLat;
        this.nwrLongitude = nwrLon;
    }

    public String toJson(int RangeSliderMin, int RangeSliderMax) {
        //throw new NotImplementedError();
        JSONObject json = new JSONObject();
        int arraySize = (RangeSliderMax-RangeSliderMin)+1;
        String[] ppmArray = new String[arraySize];
        String[] timestampArray = new String[arraySize];

        if(RangeSliderMax+1 > sensorData.size())
        {
            throw new ArrayIndexOutOfBoundsException("RangeSliderMax +1 > SensorData Array - this should not happen");
        }

        int arrayIndex = 0;
        //TODO: use limits defined by RangeSlider
        for (int i = RangeSliderMin; i < RangeSliderMax+1; i++) {
            SensorData data = sensorData.get(i);
            ppmArray[arrayIndex] = String.valueOf(data.CO2ppm);
            timestampArray[arrayIndex] = String.valueOf(data.timeStamp);
            arrayIndex++;
        }

        try {
            json.put("d", sensorID);
            json.put("p",nwrType);
            json.put("i", nwrID);
            json.put("n", nwrName);
            json.put("b", startTime);
            json.put("x", nwrLongitude); //x = lon
            json.put("y", nwrLatitude); //y = lat
            json.put("w", openWindowsDoors);
            json.put("v", ventilationSystem);
            json.put("o", OccupancyLevel);
            json.put("a", AdditionalNotes);
            json.put("c", Converter.arrayToString(ppmArray,";"));
            json.put("t", Converter.arrayToString(timestampArray,";"));


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }
}
