package com.aurelwu.indoorairqualitycollector;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private PowerManager.WakeLock wakeLock;
    ImageButton buttonGPSStatus;
    ImageButton buttonLocationPermissionStatus;
    ImageButton buttonBluetooth;

    ImageButton buttonBluetoothScanPermission;
    ImageButton buttonBluetoothConnectPermission;
    Button buttonStartRecording;
    Button buttonFinishRecording;

    Button buttonCancelRecording;
    Button buttonUpdateNearByLocations;

    boolean isUpdatingLocations = false;
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

    boolean firstCancelStepTriggered = false;

    int searchRadius = 100;
    String occupancyLevel = "undefined";

    RadioButton radioButton50mRange;
    RadioButton radioButton100mRange;
    RadioButton radioButton250mRange;



    RadioButton radioButtonOccupancyLow;
    RadioButton radioButtonOccupancyMedium;
    RadioButton radioButtonOccupancyHigh;

    LinearLayout layoutSearchRangeSelection;
    LinearLayout layoutLocationSelection;
    LinearLayout layoutStopRecording;
    LinearLayout layoutCheckboxes;
    TextInputLayout textInputLayoutCustomNotes;
    TextInputEditText textInputEditTextCustomNotes;
    LinearLayout layoutOccupancy;

    LinearLayout chartContainer;
    LineChartView lineChartView;

    LinearLayout chartRangeSliderContainer;
    IntegerRangeSlider chartRangeSlider;

    CheckBox checkBoxWindowsDoors;
    CheckBox checkBoxVentilationSystem;

    Button buttonOpenMapInBrowser;
    Button buttonImpressumDataProtection; //links to webpage with this statemenet
    ConstraintLayout constraintLayoutMap;

    public boolean invalidateLocations = false;

    public String transmissionState = "";

    Logic logic;

    LocationData selectedLocation = null;

    //DONE (no pairing required) : Monday => Display Sensor ID and if no sensor paired then open pairing Dialog, mention that currently only Aranet is supported
    //DONE : Monday => Display current CO2-Value and Update Interval
    //DONE (user needs to do it in official APP) TODO: Monday => Show error if Update Interval is not set to 1 Minute (and maybe even to change update value programmatically in this app, if not tell to use official app!)
    //DONE : Tuesday => Populate Location-Spinner and allow Selection & also allow to set different Ranges (50m, 100m, 250m)
    //DONE BUT IN MAIN SCREEN WITH HIDING OTHER ELEMENTS TODO: Wednesday => Create Recording Screen (=> display time to next update, display chart,
    //DONE : add selection for occupancy and maybe
    //DONE :     2-3 checkboxes for open windows etc.,
    //DONE :     add Freeform text field for custom Notes of Submitter
    //DONE (BUTTONS ARE THERE BUT SUBMITTING NOT DONE) TODO:     add Button to cancel recording and to submit recorded Data, both should have a confirmation
    //DONE : Start recording button disable if no location selected;s
    //SEMIDONE: SEEMS TO WORK ANYWAYSTODO: => MAKE SURE IT RUNS IN BACKGROUND!!!!
    //DONE : Build JSON to SUBMIT
    //OBSOLETE : WEDNESDAY: Submit to S3 Cloud temporarily
    //  TODO: SANITIZE DATA IN LAMBDA (CHeck size in general, of array, length of strings)
    //DONE : Thursday: Create Database Tables and submit to that instead
    //DONE : Do some real Measurements
    //DONE : Friday+Weekend: Start working on the Map and create first prototype of Map
    //SEMI DONE: links to browser Map now TODO: => OUT OF SCOPE, ADD IN 2nd VERSION integrate Map into the App(?)
    //DONE : =>  FIX IN 2nd VERSION: Scan Callback is called during scan again and again... not horrible, but not clean, fix if easily possible
    //TODO => OUT OF SCOPE, ADD IN 2nd VERSION (undefined, yes , no)
    //DONE : =>      add two-sided trim-range slider,
    //DONE: TODO IMPORTANT WHEN SENDING CHECK IF WE GET SUCESS FROM LAMBDA FUNCTION AND GIVE USER FEEDBACK THAT IT WAS SUCCESSFUL!
    //DONE  Fix name of NRW to NWR in DB...
    //TODO: Map display Windows/Ventilation/Occupancy Status , display custom Notes
    //DONE : Handle how displayed when multiple Entries for 1 Location
    //DONE TODO: keep Lambda warm to test if that makes it quick (alternatively, dump JSON directly to S3 with all Data as long as it isn't that much regularily (and maybe in future make it so that we only display general info when zoomed out and only on lower zoom level we then download a JSON which only covers a small area and we generate those regularily?
    //DONE (Using a constraint) if first submission seems to fail but doesnt really fail we can get duplicate entries in DB => DB need check if entry already exists (same NodeID + same startdate)
    //TODO: occupancy level might be buggy (especially if transmission first fails??
    //TODO: display additional Data (open doors etc.) as icons with legend explaining it (display on each entry and also x of y total
    //TODO: Option to make a Request for measurement
    //DONE: Add option to include live data
    //DONE : add button to link to web page (opening in browser)
    //DONE (not DONE: optional green/red scheme) TODO: Marker blue => red so color blindness is less of an issue (maybe have alternative scheme with green-red optionally)
    //DONE : Website add legend
    //TODO: Website add description Texts
    //TODO: in Karte CO2-Sensor eintragen zum verleihen (Angabe PLZ oder Stadt)
    //TODO: Impressum/Datenschutz in App & Website (semi done but doesnt link to existing page)
    //DONE Move to own Database
    //TODO: add confirm for cancel button
    //DONE : change text of submit button to "submitting" during submission
    //DONE BUT!! => Lambda still needs to be called automaticallyy!!! TODO: add DatabaseTables which grabs info about the locations from overpass (shop, supermarket, restaurant etc.)
    //DONE: Use Simple Query Server as Intermediate Step
    //TODO: use SQS also as intermediate Step for Live Data
    //DONE & FIXED TODO: apparently chart only shows as many entries as 1st measurement for a location was long even if later are longer, check the code and FIX
    //MAYBE DONE TODO: IF NO GPS RESULTS IN RANGE ; THEN DISPLAY THAT NOTHING WAS IN RANGE (SO PEOPLE KNOW IT WORKS)
    //TODO: APP might crash if BT is disabled? (CHristoph) => Inquire and FIX!
    //DONE  When begin recording slider values for IntegerRangeSlider must be reset to defaults!
    //TODO: Update Submit Button state as soon as the the range slider value is changed
    //DONE?: Apparently SQS response is slow if DB is not fast ? Maybe Setup of Pipeline wrong?
    //DONE?  Add a small margin left and right for all elements (as some people have protection covers which obstruct a tiny bit of the screen
    //DONE PARTLY Fix Layout a bit (display Geolocation in 1 Line ?) NEEDS TEsting
    //DONE Autosize Button Text so not on 2 lines on smaller displays) (NEEDS TESTING)
    //TODO: add other Sensors
    //TODO: Fix background of linechart on some browsers (probably need set background color explicitly? (TRIED BUT DIDNT WORK??)
    //TODO: Improve Linechart layout (especially legend) when many recordings of 1 place
    //DONE: Add Download Data button
    //Done Website add filter option (just shops, restaurants, museums etc.) [For now "Shops" , "Dining & Drinking"  "Other",] (other = Museums, Churches etc.) [Maybe add public buildings]


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

        buttonUpdateNearByLocations = findViewById(R.id.buttonUpdateNearbyLocations);
        buttonGPSStatus = findViewById(R.id.imageButtonGPS);
        buttonBluetooth = findViewById(R.id.buttonBluetoothStatus);
        buttonLocationPermissionStatus = findViewById(R.id.buttonLocationPermissionStatus);
        buttonBluetoothScanPermission = findViewById(R.id.buttonBluetoothScanPermission);
        buttonBluetoothConnectPermission = findViewById(R.id.imageButtonBluetoothConnectPermission);
        textViewPermissionAndServiceStatus = findViewById(R.id.textViewStatus);
        textViewSensorStatus = findViewById(R.id.textViewSensorStatus);
        textViewLocationStatus = findViewById(R.id.textViewLocationStatus);
        buttonStartRecording = findViewById(R.id.buttonStartRecording);
        buttonFinishRecording = findViewById(R.id.buttonFinishRecording);
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

        buttonOpenMapInBrowser = findViewById(R.id.buttonOpenMapInBrowser);
        buttonImpressumDataProtection = findViewById(R.id.buttonImpressum);
        constraintLayoutMap = findViewById(R.id.ConstraintLayoutShowMap);


        layoutLocationSelection = findViewById(R.id.LinearLayoutLocationSelector);
        layoutSearchRangeSelection = findViewById(R.id.LinearLayoutSearchRange);
        layoutStopRecording = findViewById(R.id.linearLayoutEndRecording);
        chartContainer = findViewById(R.id.LinearLayoutLineChartContainer);
        lineChartView = new LineChartView(this);
        chartContainer.addView(lineChartView);

        chartRangeSliderContainer = findViewById(R.id.LinearLayoutChartRangeSliderContainer);
        chartRangeSlider = new IntegerRangeSlider(this);
        chartRangeSliderContainer.addView(chartRangeSlider);
        chartRangeSlider.setMin(0);
        chartRangeSlider.setMax(10);

        layoutCheckboxes = findViewById(R.id.LinearLayoutCheckboxes);
        checkBoxWindowsDoors = findViewById(R.id.checkBoxWindowsDoors);
        checkBoxVentilationSystem = findViewById(R.id.checkBoxVentilationSystem);


        textInputLayoutCustomNotes = findViewById((R.id.TextInputLayout));
        textInputLayoutCustomNotes.setVisibility(View.GONE);

        textInputEditTextCustomNotes = findViewById(R.id.TextInputEditTextNotes);

        layoutOccupancy = findViewById(R.id.LinearLayoutOccupancy);
        buttonCancelRecording = findViewById(R.id.buttonAbort);


        locationSpinner = findViewById(R.id.spinnerSelectLocation);


        UIUpdater.postDelayed(Update, 2000);

        logic.spatialManager.searchRadius = searchRadius;
    }

    public void OnStartRecordingButton(View view)
    {



        transmissionState = "none";
        selectedLocation = (LocationData)locationSpinner.getSelectedItem();
        if(selectedLocation==null)
        {
            //maybe display message that no location selected?)
            return;
        }

        // Acquire the wakelock
        PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IndoorCO2App::SensorDataRecordingWakelock");
        wakeLock.acquire();

        //TODO:
        buttonFinishRecording.setEnabled(false);
        layoutStopRecording.setVisibility(View.VISIBLE);
        layoutSearchRangeSelection.setVisibility(View.GONE);
        layoutLocationSelection.setVisibility(View.GONE);
        buttonStartRecording.setVisibility(View.GONE);
        buttonUpdateNearByLocations.setVisibility(View.GONE);
        chartContainer.setVisibility(View.VISIBLE);
        chartRangeSliderContainer.setVisibility(View.VISIBLE);
        lineChartView.setVisibility(View.VISIBLE);
        lineChartView.invalidate();
        layoutCheckboxes.setVisibility(View.VISIBLE);
        textInputLayoutCustomNotes.setVisibility(View.VISIBLE);
        //layoutOccupancy.setVisibility(View.VISIBLE);
        layoutOccupancy.setVisibility(View.GONE); // For now we won't track this, its subjective anyways and less UI Elements = better
        buttonOpenMapInBrowser.setVisibility(View.GONE);
        buttonImpressumDataProtection.setVisibility(View.GONE);
        constraintLayoutMap.setVisibility(View.GONE);
        chartRangeSlider.ReInit();

        logic.StartNewRecording(selectedLocation.ID,selectedLocation.type, selectedLocation.Name,selectedLocation.latitude,selectedLocation.longitude,System.currentTimeMillis());
        checkBoxVentilationSystem.setChecked(false);
        checkBoxWindowsDoors.setChecked(false);
        occupancyLevel = "undefined";

    }

    public void OnFinishAndSubmitRecording(View view)
    {
        logic.FinishRecording(checkBoxWindowsDoors.isChecked(),checkBoxVentilationSystem.isChecked(),occupancyLevel,textInputEditTextCustomNotes.getText().toString());
        chartRangeSlider.getMinValue();
        String json = logic.GenerateJSONToTransmit(chartRangeSlider.getMinValue(),chartRangeSlider.getMaxValue());
        transmissionState = "none";
        buttonFinishRecording.setEnabled(false);
        buttonFinishRecording.setText(("Submitting"));
        ApiGatewayCaller.sendJsonToApiGateway(json,this);

        // Release the wakelock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void OnTransmissionSuccess()
    {
        buttonFinishRecording.setText("Transmission successful!");

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                OnStopRecordingChangeUI();
            }
        }, 5000); // 3000 milliseconds = 3 seconds
    }

    public void OnTransmissionFail(String failureMode)
    {
        buttonFinishRecording.setText(failureMode);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                buttonFinishRecording.setEnabled(true);
            }
        }, 2000); // 3000 milliseconds = 3 seconds
    }

    public void OnCancelRecording(View view)
    {
        if(!firstCancelStepTriggered)
        {
            firstCancelStepTriggered = true;
            buttonCancelRecording.setText("Confirm Cancel");
        }
        else
        {
            logic.FinishRecording(checkBoxWindowsDoors.isChecked(),checkBoxVentilationSystem.isChecked(),occupancyLevel,textInputEditTextCustomNotes.getText().toString());
            OnStopRecordingChangeUI();
            // Release the wakelock
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            buttonCancelRecording.setText("Cancel");
            firstCancelStepTriggered = false;
        }

    }


    public void OnStopRecordingChangeUI()
    {
        buttonStartRecording.setVisibility(View.VISIBLE);
        layoutStopRecording.setVisibility(View.GONE);
        layoutSearchRangeSelection.setVisibility(View.VISIBLE);
        layoutLocationSelection.setVisibility(View.VISIBLE);
        buttonUpdateNearByLocations.setVisibility(View.VISIBLE);
        buttonOpenMapInBrowser.setVisibility(View.VISIBLE);
        buttonImpressumDataProtection.setVisibility(View.VISIBLE);
        constraintLayoutMap.setVisibility(View.VISIBLE);
        chartContainer.setVisibility(View.GONE);
        chartRangeSliderContainer.setVisibility(View.GONE);
        lineChartView.setVisibility(View.GONE);
        layoutCheckboxes.setVisibility(View.GONE);
        textInputLayoutCustomNotes.setVisibility(View.GONE);
        layoutOccupancy.setVisibility(View.GONE);
        textInputEditTextCustomNotes.setText("");
        radioButtonOccupancyLow.setChecked(false);
        radioButtonOccupancyMedium.setChecked(false);
        radioButtonOccupancyHigh.setChecked(false);

        checkBoxWindowsDoors.setChecked(false);
        checkBoxVentilationSystem.setChecked(false);
    }


    public void OnButtonClickUpdateLocationSpinner(View view)
    {
        buttonUpdateNearByLocations.setTextColor(Color.WHITE);
        buttonUpdateNearByLocations.setText("updating Locations...");
        buttonUpdateNearByLocations.setEnabled(false);
        isUpdatingLocations = true;
        logic.spatialManager.overpassModule.FetchNearbyBuildings(this);

    }

    public void OnButtonClickUpdateLocationSpinnerFinished()
    {
        buttonUpdateNearByLocations.setText("Update Nearby Locations");
        buttonUpdateNearByLocations.setTextColor(Color.BLACK);
        isUpdatingLocations = false;
    }

    public void UpdateLocationSelectionSpinner()
    {
        ArrayList<String> defaultMessageList = new ArrayList<>();
        defaultMessageList.add("No Locations found, search again");
        if(!invalidateLocations) return;
        ArrayAdapter<LocationData> adapter = new LocationDataAdapter(this, logic.spatialManager.overpassModule.locationData);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);
        locationSpinner.setEnabled(true);
        if(logic.spatialManager.overpassModule.locationData.isEmpty())
        {
            locationSpinner.setEnabled(false);
            ArrayAdapter<String> emptyAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,defaultMessageList);
        }

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
        logic.spatialManager.RequestLocationPermissions();
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
        GPSEnabled = logic.spatialManager.isGPSEnabled();
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
                textViewSensorStatus.setText("ID: " + logic.aranetManager.aranetMAC + " | rssi: " + logic.aranetManager.rssi + " | gattS: " + logic.aranetManager.GattStatus + "\r\nlast value: " + logic.aranetManager.currentReading.CO2ppm + "ppm. Update in " + timeToNextUpdate + " seconds");
            }

            else if(logic.aranetManager.currentReading == null && logic.aranetManager.GattModeIsA2DP==true)
            {
                textViewSensorStatus.setText("Sensor found, but the required 'Smart Home Integration' is disabled.\r\n Please enable it using the official Aranet App (use the Gears Icon)");
            }

            else if(logic.aranetManager.currentReading == null)
            {
                textViewSensorStatus.setText("Waiting for first sensor update. This might take a Minute | ID: " + logic.aranetManager.aranetMAC + " | rssi: " + logic.aranetManager.rssi + "GattS: " +logic.aranetManager.GattStatus);
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
                if(logic.aranetManager.isRecording && selectedLocation!=null)
                {
                    //textViewLocationStatus.setText("Recording data of Location: " + selectedLocation.Name+"\r\n"+ String.format("%.6f", selectedLocation.latitude) + " | " + String.format("%.6f", selectedLocation.longitude ));
                    textViewLocationStatus.setText("Recording data of Location: " + selectedLocation.Name);
                }
                else
                {
                    textViewLocationStatus.setText(String.format("Lat:" +"%.6f", logic.spatialManager.myLatitude) + " | Lon: " + String.format("%.6f", logic.spatialManager.myLongitude));
                }

                if(isUpdatingLocations==false)
                {
                    buttonUpdateNearByLocations.setEnabled(true);
                }

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
            chartRangeSlider.SetDataRange(logic.aranetManager.GetCO2Data().length);
            if(logic.aranetManager.GetCO2Data().length<5 || (chartRangeSlider.getMaxValue()-chartRangeSlider.getMinValue() <4))
            {
                buttonFinishRecording.setEnabled(false);
                buttonFinishRecording.setText("Submit (needs 5 minutes of Data)");
            }
            else
            {
                buttonFinishRecording.setEnabled(true);
                buttonFinishRecording.setText("Submit Data");
            }
        }
        if(transmissionState == "failure")
        {
            buttonFinishRecording.setText("Failed to Submit Data, try again");
        }
        if(transmissionState == "success")
        {
            textViewPermissionAndServiceStatus.append(". Last Transmission successful!");
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
                logic.spatialManager.requestLocationUpdates();
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

    public void OnOpenMapInBrowser(View view)
    {
        String mapURL = "http://indoorco2map.s3-website.eu-central-1.amazonaws.com/";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(mapURL));
        startActivity(intent);
    }

    public void OnOpenImprint(View view)
    {
        String URL = "https://www.bluestats.net/Datenschutz.html";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(URL));
        startActivity(intent);
    }
}