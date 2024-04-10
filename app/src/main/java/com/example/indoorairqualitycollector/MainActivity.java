package com.example.indoorairqualitycollector;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    ImageButton buttonGPSStatus;
    ImageButton buttonLocationPermissionStatus;
    ImageButton buttonBluetooth;

    ImageButton buttonBluetoothScanPermission;
    ImageButton buttonBluetoothConnectPermission;
    Button buttonStartRecording;
    Button buttonUpdateNearByLocations;
    Spinner locationSpinner;

    TextView textViewPermissionAndServiceStatus;
    TextView textViewSensorStatus;
    TextView textViewLocationStatus;
    private long lastTimestamp = 0;
    private long lastLocationUpdateTimer = 0;
    private long timeToNextUpdate = 0;

    boolean GPSEnabled =false;
    boolean bluetoothEnabled = false;
    boolean locationPermission = false;
    boolean bluetoothScanPermission = false;
    boolean bluetoothConnectPermission = false;
    boolean updateIntervalSetTo1Minute = false;
    boolean deviceFound = false;

    int searchRadius = 100;
    String occupancyLevel = "undefined";

    RadioButton radioButton50mRange;
    RadioButton radioButton100mRange;
    RadioButton radioButton250mRange;

    RadioButton radioButtonOccupancyLow;
    RadioButton radioButtonOccupancyMedium;
    RadioButton radioButtonOccupancyHigh;

    Button buttonFinishAndSubmitRecording;
    LinearLayout layoutSearchRangeSelection;
    LinearLayout layoutLocationSelection;
    LinearLayout layoutStopRecording;
    LinearLayout layoutCheckboxes;
    TextInputLayout textInputLayoutCustomNotes;
    TextInputEditText textInputEditTextCustomNotes;
    LinearLayout layoutOccupancy;

    LinearLayout chartContainer;
    LineChartView lineChartView;

    CheckBox checkBoxWindowsDoors;
    CheckBox checkBoxVentilationSystem;

    public boolean invalidateLocations = false;

    Logic logic;

    //DONE (no pairing required) TODO: Monday => Display Sensor ID and if no sensor paired then open pairing Dialog, mention that currently only Aranet is supported
    //DONE TODO: Monday => Display current CO2-Value and Update Interval
    //DONE (user needs to do it in official APP) TODO: Monday => Show error if Update Interval is not set to 1 Minute (and maybe even to change update value programmatically in this app, if not tell to use official app!)
    //DONE TODO: Tuesday => Populate Location-Spinner and allow Selection & also allow to set different Ranges (50m, 100m, 250m)
    //DONE BUT IN MAIN SCREEN WITH HIDING OTHER ELEMENTS TODO: Wednesday => Create Recording Screen (=> display time to next update, display chart,
    //TODO WEDNESDAY:     add trim-range slider,
    // DONE TODO: add selection for occupancy and maybe
    //DONE TODO:     2-3 checkboxes for open windows etc.,
    //DONE TODO:     add Freeform text field for custom Notes of Submitter
    //SEMI DONE (BUTTONS ARE THERE BUT SUBMITTING NOT DONE) TODO:     add Button to cancel recording and to submit recorded Data, both should have a confirmation
    //TODO: Build JSON to SUBMIT
    //TODO: WEDNESDAY: Submit to S3 Cloud temporarily
    //TODO: Thursday: Create Database Tables and submit to that instead
    //TODO: Do some real Measurements
    //TODO: Friday+Weekend: Start working on the Map and create first prototype of Map
    //TODO: LATER: integrate Map into the App(?)
    //TODO: Scan Callback is called during scan again and again... not horrible, but not clean, fix if easily possible
    //DONE TODO: Start recording button disable if no location selected;s
    //TODO: => MAKE SURE IT RUNS IN BACKGROUND!!!!
    //TODO: Checkboxes  with 3 States (undefined, yes , no)

    private final Handler UIUpdater = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.RadioButtonSearchRange250m), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        logic = new Logic(this);

        buttonGPSStatus = findViewById(R.id.imageButtonGPS);
        buttonBluetooth = findViewById(R.id.buttonBluetoothStatus);
        buttonLocationPermissionStatus = findViewById(R.id.buttonLocationPermissionStatus);
        buttonBluetoothScanPermission = findViewById(R.id.buttonBluetoothScanPermission);
        buttonBluetoothConnectPermission = findViewById(R.id.imageButtonBluetoothConnectPermission);
        textViewPermissionAndServiceStatus = findViewById(R.id.textViewStatus);
        textViewSensorStatus = findViewById(R.id.textViewSensorStatus);
        textViewLocationStatus = findViewById(R.id.textViewLocationStatus);
        buttonStartRecording = findViewById(R.id.buttonStartRecording);
        radioButton50mRange = findViewById(R.id.radioButtonSearchRange50m);
        radioButton100mRange = findViewById(R.id.radioButtonSearchRange100m);
        radioButton250mRange = findViewById(R.id.radioButtonSearchRange250m);

        radioButton100mRange.setChecked(true);
        radioButton50mRange.setOnCheckedChangeListener(radioGroupListenerSearchRadius);
        radioButton100mRange.setOnCheckedChangeListener(radioGroupListenerSearchRadius);
        radioButton250mRange.setOnCheckedChangeListener(radioGroupListenerSearchRadius);

        radioButtonOccupancyLow = findViewById(R.id.radioButtonLowOccupancy);
        radioButtonOccupancyMedium = findViewById(R.id.radioButtonMediumOccupancy);
        radioButtonOccupancyHigh = findViewById(R.id.radioButtonHighOccupancy);

        radioButtonOccupancyLow.setOnCheckedChangeListener(radioGroupListenerOccupancyLevels);
        radioButtonOccupancyMedium.setOnCheckedChangeListener(radioGroupListenerOccupancyLevels);
        radioButtonOccupancyHigh.setOnCheckedChangeListener(radioGroupListenerOccupancyLevels);



        layoutLocationSelection = findViewById(R.id.LinearLayoutLocationSelector);
        layoutSearchRangeSelection = findViewById(R.id.LinearLayoutSearchRange);
        layoutStopRecording = findViewById(R.id.linearLayoutEndRecording);
        chartContainer = findViewById(R.id.LinearLayoutLineChartContainer);
        lineChartView = new LineChartView(this);
        chartContainer.addView(lineChartView);

        layoutCheckboxes = findViewById(R.id.LinearLayoutCheckboxes);
        checkBoxWindowsDoors = findViewById(R.id.checkBoxWindowsDoors);
        checkBoxVentilationSystem = findViewById(R.id.checkBoxVentilationSystem);


        textInputLayoutCustomNotes = findViewById((R.id.TextInputLayout));
        textInputLayoutCustomNotes.setVisibility(View.GONE);

        textInputEditTextCustomNotes = findViewById(R.id.TextInputEditTextNotes);

        layoutOccupancy = findViewById(R.id.LinearLayoutOccupancy);


        buttonUpdateNearByLocations = findViewById(R.id.buttonUpdateNearbyLocations);
        locationSpinner = findViewById(R.id.spinnerSelectLocation);


        UIUpdater.postDelayed(Update, 1000);

        logic.spatialManager.searchRadius = searchRadius;
    }

    public void OnStartRecordingButton(View view)
    {
        LocationData selectedLocation = (LocationData)locationSpinner.getSelectedItem();
        if(selectedLocation==null)
        {
            //maybe display message that no location selected?)
            return;
        }
        //TODO:
        layoutStopRecording.setVisibility(View.VISIBLE);
        layoutSearchRangeSelection.setVisibility(View.GONE);
        layoutLocationSelection.setVisibility(View.GONE);
        buttonStartRecording.setVisibility(View.GONE);
        buttonUpdateNearByLocations.setVisibility(View.GONE);
        chartContainer.setVisibility(View.VISIBLE);
        lineChartView.setVisibility(View.VISIBLE);
        lineChartView.invalidate();
        layoutCheckboxes.setVisibility(View.VISIBLE);
        textInputLayoutCustomNotes.setVisibility(View.VISIBLE);
        layoutOccupancy.setVisibility(View.VISIBLE);


        logic.StartNewRecording(selectedLocation.ID,selectedLocation.Name,selectedLocation.latitude,selectedLocation.longitude,System.currentTimeMillis());
    }

    public void OnFinishAndSubmitRecording(View view)
    {

        logic.FinishRecording(checkBoxWindowsDoors.isChecked(),checkBoxVentilationSystem.isChecked(),occupancyLevel,textInputEditTextCustomNotes.getText().toString());
        logic.SubmitRecordedData();
        OnStopRecordingChangeUI();
    }

    public void OnCancelRecording(View view)
    {

        logic.FinishRecording(checkBoxWindowsDoors.isChecked(),checkBoxVentilationSystem.isChecked(),occupancyLevel,textInputEditTextCustomNotes.getText().toString());
        OnStopRecordingChangeUI();

    }


    public void OnStopRecordingChangeUI()
    {
        buttonStartRecording.setVisibility(View.VISIBLE);
        layoutStopRecording.setVisibility(View.GONE);
        layoutSearchRangeSelection.setVisibility(View.VISIBLE);
        layoutLocationSelection.setVisibility(View.VISIBLE);
        buttonUpdateNearByLocations.setVisibility(View.VISIBLE);
        chartContainer.setVisibility(View.GONE);
        lineChartView.setVisibility(View.GONE);
        layoutCheckboxes.setVisibility(View.GONE);
        textInputLayoutCustomNotes.setVisibility(View.GONE);
        layoutOccupancy.setVisibility(View.GONE);
    }


    public void OnButtonClickUpdateLocationSpinner(View view)
    {
        logic.spatialManager.overpassModule.FetchNearbyBuildings();
        //UpdateLocationSelectionSpinner();
    }

    public void UpdateLocationSelectionSpinner()
    {
        if(!invalidateLocations) return;
        ArrayAdapter<LocationData> adapter = new LocationDataAdapter(this, logic.spatialManager.overpassModule.locationData);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);
        invalidateLocations=false;
    }

    public void OnButtonClickGPS(View view)
    {
        logic.spatialManager.showEnableGpsDialog();
    }

    public void OnButtonClickBluetooth(View view)
    {
        logic.bluetoothManager.showEnableBluetoothDialog();
    }

    public void OnButtonClickLocationPermission(View view) 
    {
        logic.spatialManager.RequestLocationPermissions(345,true);
    }

    public void OnButtonBTScanPermission(View view)
    {
        logic.bluetoothManager.RequestBluetoothScanPermission(123,true);
    }

    public void OnButtonBTConnectPermission(View view)
    {
        logic.bluetoothManager.RequestBluetoothConnectPermission(890,true);
    }

    private void UpdateUIElements()
    {
        UpdateLocationSelectionSpinner();
        GPSEnabled = logic.spatialManager.IsGPSEnabled();
        bluetoothEnabled = logic.bluetoothManager.IsBluetoothEnabled();
        locationPermission = logic.spatialManager.CheckLocationPermissions();
        bluetoothScanPermission = logic.bluetoothManager.CheckBluetoothScanPermissions();
        bluetoothConnectPermission = logic.bluetoothManager.CheckBluetoothConnectPermissions();
        updateIntervalSetTo1Minute = false;
        if(logic.aranetManager.UpdateInterval <=60)
        {
            updateIntervalSetTo1Minute = true;
        }
        deviceFound = false;
        if(logic.aranetManager.aranetDevice!=null)
        {
            deviceFound = true;
        }

        if (GPSEnabled) {
            buttonGPSStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonTintEnabled));
        }
        else
        {
            buttonGPSStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonTintDisabled));
        }

        if(bluetoothEnabled)
        {
            buttonBluetooth.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonTintEnabled));
        }
        else
        {
            buttonBluetooth.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonTintDisabled));
        }

        if (locationPermission)
        {
            buttonLocationPermissionStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonTintEnabled));
        }
        else
        {
            buttonLocationPermissionStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonTintDisabled));
        }

        if(bluetoothConnectPermission)
        {
            buttonBluetoothConnectPermission.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonTintEnabled));
        }
        else
        {
            buttonBluetoothConnectPermission.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonTintDisabled));
        }

        if(bluetoothScanPermission)
        {
            buttonBluetoothScanPermission.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonTintEnabled));
        }
        else
        {
            buttonBluetoothScanPermission.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonTintDisabled));
        }

        if(GPSEnabled && bluetoothEnabled && locationPermission && bluetoothConnectPermission && bluetoothConnectPermission)
        {
            textViewPermissionAndServiceStatus.setText("GPS and Bluetooth enabled and all required permissions granted");
            //we display that info on the UI
        }
        else
        {
            textViewPermissionAndServiceStatus.setText("not all necessary Services / Permissions enabled, press the red buttons to adjust");
        }

        if(!bluetoothEnabled || ! bluetoothConnectPermission || ! bluetoothScanPermission)
        {
            textViewSensorStatus.setText("Bluetooth not enabled or permissions missing, can not fetch Sensor Data");
        }
        else
        {
            if(logic.aranetManager.aranetDevice == null)
            {
                textViewSensorStatus.setText("Aranet Device not found, next attempt in " + timeToNextUpdate + " seconds" );
            }
            else if(logic.aranetManager.currentReading != null && logic.aranetManager.UpdateInterval >60)
            {
                textViewSensorStatus.setText("Device found but Update Interval not set to 1 Minute, change to 1 Minute using official App. next attempt in " + timeToNextUpdate + " seconds");
            }
            else if(logic.aranetManager.currentReading != null)
            {
                textViewSensorStatus.setText("Device ID: " + logic.aranetManager.aranetMAC + "\r\nlast value: " + logic.aranetManager.currentReading.CO2ppm + "ppm. Update in " + timeToNextUpdate + " seconds");
            }
            else if(logic.aranetManager.currentReading == null)
            {
                textViewSensorStatus.setText("Waiting for first sensor update. This might take up to a Minute");
            }
        }

        if(GPSEnabled && locationPermission && bluetoothEnabled && bluetoothConnectPermission && bluetoothScanPermission && updateIntervalSetTo1Minute && deviceFound)
        {
            if(logic.spatialManager.overpassModule.locationData.size() == 0)
            {
                buttonStartRecording.setEnabled(false);
            }
            else
            {
                buttonStartRecording.setEnabled(true);
            }
        }
        else
        {
            buttonStartRecording.setEnabled(false);
        }

        if(GPSEnabled && locationPermission)
        {
            if(logic.spatialManager.myLatitude != 0 || logic.spatialManager.myLongitude != 0)
            {
                textViewLocationStatus.setText("Current Location:\r\nLat: " + String.format("%.6f", logic.spatialManager.myLatitude) + "\r\nLon: " + String.format("%.6f", logic.spatialManager.myLongitude));

                buttonUpdateNearByLocations.setEnabled(true);
            }
            else
            {
                textViewLocationStatus.setText("GPS enabled and Location permissions granted. Getting first Location Info. This might take a minute");
                buttonUpdateNearByLocations.setEnabled(false);
            }
        }
        else
        {
            textViewLocationStatus.setText("");
            if(!GPSEnabled) textViewLocationStatus.append("GPS not enabled");
            if(!locationPermission) textViewLocationStatus.append("Location Permission missing");
            buttonUpdateNearByLocations.setEnabled(false);
        }

        if(logic.aranetManager.isRecording)
        {
            lineChartView.setData(logic.aranetManager.GetCO2Data());

            if(logic.aranetManager.GetCO2Data().length<5)
            {
                buttonFinishAndSubmitRecording.setEnabled(false);
            }
            else
            {
                buttonFinishAndSubmitRecording.setEnabled(true);
            }
        }
    }



    private final Runnable Update = new Runnable() {
        @Override
        public void run() {
            UpdateUIElements();

            long timeDifference = System.currentTimeMillis()-lastTimestamp;

            long timeDifferenceLocation = System.currentTimeMillis()-lastLocationUpdateTimer;
            if(timeDifferenceLocation > 10000)
            {
                logic.spatialManager.RequestLocationUpdate();
                lastLocationUpdateTimer = System.currentTimeMillis();
            }

            if(timeDifference >= 60001)
            {
                logic.aranetManager.Update(); //This
                lastTimestamp = System.currentTimeMillis();
            }
            timeToNextUpdate=(60000-timeDifference)/1000;
            timeToNextUpdate=Math.max(timeToNextUpdate,0);

            UIUpdater.postDelayed(this, 1000);
        }
    };

    CompoundButton.OnCheckedChangeListener radioGroupListenerSearchRadius = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                switch ((String)buttonView.getText()) {
                    case "50m":
                        searchRadius = 50;
                        break;
                    case "100m":
                        searchRadius = 100;
                        break;
                    case "250m":
                        searchRadius = 250;
                        break;
                    default:
                        // Handle unexpected case
                        break;
                }
                Log.d("radioGroupListenerSearchRadius", "searchradius: " + searchRadius);
                logic.spatialManager.searchRadius = searchRadius;
            }
        }
    };

    CompoundButton.OnCheckedChangeListener radioGroupListenerOccupancyLevels = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                switch ((String)buttonView.getText()) {
                    case "low":
                        occupancyLevel = "low";
                        break;
                    case "medium":
                        occupancyLevel = "medium";
                        break;
                    case "high":
                        occupancyLevel = "high";
                        break;
                    default:
                        // Handle unexpected case
                        break;
                }
                Log.d("radioGroupListenerOccupancyLevels", "occupancyLevel: " + occupancyLevel);
                logic.spatialManager.searchRadius = searchRadius;
            }
        }
    };
}