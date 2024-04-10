package com.example.indoorairqualitycollector;

import android.util.Log;

public class Haversine {
    public static final double R = 6372.8; // In kilometers

    public static double getDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {

        Log.d("Haversine", "lat1:"+lat1 +" | lat2:" +lat2 + " | lon1: "+lon1 + " | lon2: "+ lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c * 1000;
    }
}