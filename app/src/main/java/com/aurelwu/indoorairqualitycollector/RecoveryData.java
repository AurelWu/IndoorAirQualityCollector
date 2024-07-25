package com.aurelwu.indoorairqualitycollector;

import android.content.Context;
import android.content.SharedPreferences;

//should probably write the class serialized to preferences, but this works too...
public class RecoveryData
{
    long startTime;
    long timeOfLastUpdate;
    long locationID;
    String locationType; //nwr
    String locationName;
    float locationLat;
    float locationLon;
    final static String prefName = "IndoorDataCollector";

    public RecoveryData()
    {

    }

    public RecoveryData(long startTime,long timeOfLastUpdate,long locationID,String locationType,String locationName,float lat, float lon)
    {
        this.startTime = startTime;
        this.timeOfLastUpdate = timeOfLastUpdate;
        this.locationID=locationID;
        this.locationType=locationType;
        this.locationName=locationName;
        this.locationLat=lat;
        this.locationLon=lon;
    }

    public void WriteToPreferences(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("recoveryStartTime", startTime);
        editor.putLong("recoveryTimeOfLastUpdate", timeOfLastUpdate);
        editor.putLong("recoveryLocationID", locationID);
        editor.putString("recoveryLocationType", locationType);
        editor.putString("recoveryLocationName", locationName);
        editor.putFloat("recoveryLat",locationLat);
        editor.putFloat("recoveryLon",locationLon);
        editor.apply();
    }

    public static RecoveryData ReadFromPreferences(Context context)
    {
        RecoveryData r = new RecoveryData();
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        r.startTime = sharedPreferences.getLong("recoveryStartTime",0);
        r.timeOfLastUpdate = sharedPreferences.getLong("recoveryTimeOfLastUpdate",0);
        r.locationID= sharedPreferences.getLong("recoveryLocationID",0);
        r.locationName = sharedPreferences.getString("recoveryLocationName","");
        r.locationType = sharedPreferences.getString("recoveryLocationType","");
        r.locationLat = sharedPreferences.getFloat("recoveryLat",0);
        r.locationLon = sharedPreferences.getFloat("recoveryLon",0);
        return r;
    }

    public static void DeleteRecoveryData(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("recoveryStartTime", 0);
        editor.putLong("recoveryTimeOfLastUpdate", 0);
        editor.putLong("recoveryLocationID", 0);
        editor.putString("recoveryLocationType", "");
        editor.putString("recoveryLocationName", "");
        editor.putFloat("recoveryLat",0);
        editor.putFloat("recoveryLon",0);
        editor.apply();
    }
}
