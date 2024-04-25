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
import android.location.LocationManager;

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
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
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
        if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    public void StopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void updateLocation(Location location) {
        myLatitude = location.getLatitude();
        myLongitude = location.getLongitude();
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