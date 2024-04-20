package com.aurelwu.indoorairqualitycollector;

public class SensorData
{
    public int CO2ppm;
    public long timeStamp;
    //we could also track temperature... but meh lets keep it small and simple
    //maybe we also grab user Lat / Lon at that point to ensure they stayed in the building?

    public SensorData(int ppm, long time)
    {
        this.CO2ppm = ppm;
        this.timeStamp = time;
    }
}
