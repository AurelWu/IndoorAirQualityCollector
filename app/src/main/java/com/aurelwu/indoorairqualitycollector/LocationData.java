package com.aurelwu.indoorairqualitycollector;

public class LocationData {
    public long ID;
    public String Name;
    public double latitude;
    public double longitude;

    public double distanceToGivenLocation;

    public LocationData(long ID, String Name, double latitude, double longitude, double myLatitude, double myLongitude)
    {
        this.ID=ID;
        this.Name =Name;
        this.latitude =latitude;
        this.longitude = longitude;
        CalculateDistanceToGivenLocation(myLatitude,myLongitude);
    }
    private void CalculateDistanceToGivenLocation(double myLatitude, double myLongitude)
    {
        distanceToGivenLocation = Haversine.getDistanceInMeters(myLatitude,myLongitude,latitude,longitude);
    }

    public String GetSpinnerstring()
    {
        return Name + " | " + (int)distanceToGivenLocation + "m";
    }
}

