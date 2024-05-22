package com.aurelwu.indoorairqualitycollector;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import android.location.LocationManager;
import android.util.Log;

public class SpatialManager {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 345;

    public MainActivity mainActivity;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    public double myLongitude;
    public double myLatitude;
    public double searchRadius;

    public OverpassModule overpassModule;

    public SpatialManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mainActivity);
        overpassModule = new OverpassModule(this);
        RequestLocationPermissions();
        createLocationRequest();
        createLocationCallback();
    }

    private void createLocationRequest() {
        int locationInterval = 10000;
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2500)
                .setMaxUpdateDelayMillis(5000)
                .build();

    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d("SpatialManager", "LocationCallback called");
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        updateLocation(location);
                    }
                }
            }
        };
    }

    public void requestLocationUpdates() {
        Log.d("SpatialManager", "requestLocationUpdates called - pre check");
        if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d("SpatialManager", "requestLocationUpdates called - past check");
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    public void StopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void updateLocation(Location location) {
        Log.d("SpatialManager", "updateLocation Called: " + location.getLatitude() + " | " +location.getLongitude());
        myLatitude = location.getLatitude();
        myLongitude = location.getLongitude();
        StopLocationUpdates();
    }

    public boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) mainActivity.getSystemService(mainActivity.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean CheckLocationPermissions() {
        return ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void RequestLocationPermissions() {
        if (CheckLocationPermissions()) {
            return;
        }

        ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
    }

    public void showEnableGpsDialog() {
        if (isGPSEnabled()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setMessage("GPS is disabled. Do you want to enable it?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Open location settings to enable GPS
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        mainActivity.startActivity(intent);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked "No", do nothing
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}