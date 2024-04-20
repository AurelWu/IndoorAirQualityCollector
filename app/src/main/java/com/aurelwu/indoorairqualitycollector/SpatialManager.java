package com.aurelwu.indoorairqualitycollector;

import static android.content.Context.LOCATION_SERVICE;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

public class SpatialManager implements LocationListener {
    public MainActivity mainActivity;
    public LocationManager locationManager;
    public double myLongitude;
    public double myLatitude;

    public double searchRadius;

    public OverpassModule overpassModule;

    public SpatialManager(MainActivity mainActivity) {

        this.mainActivity = mainActivity;
        locationManager = (LocationManager) mainActivity.getSystemService(LOCATION_SERVICE);
        overpassModule = new OverpassModule(this);
        RequestLocationPermissions(345, false);
    }

    public void RequestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
    }

    @Override
    public void onLocationChanged(@NonNull Location location)
    {
        myLatitude = location.getLatitude();
        myLongitude = location.getLongitude();
    }

    public boolean IsGPSEnabled() {
        if (locationManager == null) return false;
        LocationManager locationManager = (LocationManager) mainActivity.getSystemService(LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean CheckLocationPermissions()
    {
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public void RequestLocationPermissions(int requestCode, boolean showDialogToManuallyChangePermissions )
    {
        ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);

        if(showDialogToManuallyChangePermissions && !CheckLocationPermissions())
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder.setMessage("Location permission is denied. Do you want to enable it?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Open location settings to enable GPS
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.fromParts("package", mainActivity.getPackageName(), null));
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

    public void showEnableGpsDialog()
    {
        if(IsGPSEnabled()) return; //GPS already enabled, no need to show Dialog
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
