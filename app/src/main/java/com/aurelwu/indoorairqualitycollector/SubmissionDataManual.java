package com.aurelwu.indoorairqualitycollector;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


import kotlin.NotImplementedError;

public class SubmissionDataManual
{
    public String sensorID;
    public long startTime;
    public List<SensorData> sensorData;

    public List<Double> LatitudeData;
    public List<Double> LongitudeData;

    public boolean openWindowsDoors;
    public boolean ventilationSystem;
    public String OccupancyLevel;
    public String AdditionalNotes;

    public String LocationName;

    public String LocationAddress;

    public SubmissionDataManual(String sensorID, long startTime)
    {
        this.sensorID = sensorID;
        this.startTime = startTime;
        this.LatitudeData = new ArrayList<Double>();
        this.LongitudeData = new ArrayList<Double>();
        this.LocationName = "";
        this.LocationAddress = "";
    }

    public String ToJson(int RangeSliderMin, int RangeSliderMax) {

        JSONObject json = new JSONObject();
        int arraySize = (RangeSliderMax-RangeSliderMin)+1;
        String[] ppmArray = new String[arraySize];




        //String[] timestampArray = new String[arraySize];
        if(RangeSliderMax+1 > sensorData.size())
        {
            throw new ArrayIndexOutOfBoundsException("RangeSliderMax +1 > SensorData Array - this should not happen");
        }


//
        int arrayIndex = 0;
        ////TODO: use limits defined by RangeSlider
        for (int i = RangeSliderMin; i < RangeSliderMax+1; i++) {
            SensorData data = sensorData.get(i);
            ppmArray[arrayIndex] = String.valueOf(data.CO2ppm);
            arrayIndex++;
        }

//
        try {
            json.put("d", sensorID);
            json.put("b", startTime);
            json.put("l", LocationName);
            json.put("ad", LocationAddress);
            json.put("w", openWindowsDoors);
            json.put("v", ventilationSystem);
            json.put("o", OccupancyLevel);
            json.put("a", AdditionalNotes);
            json.put("c", Converter.arrayToString(ppmArray,";"));
            json.put("la",Converter.arrayToString(LatitudeData.toArray(new Double[0]), ";" ));
            json.put("lo",Converter.arrayToString(LongitudeData.toArray(new Double[0]), ";" ));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }
}
