package com.aurelwu.indoorairqualitycollector;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private boolean showExactLocation = false;
    //private PowerManager.WakeLock wakeLock;
    ImageButton buttonGPSStatus;
    ImageButton buttonLocationPermissionStatus;
    ImageButton buttonBluetooth;

    ImageButton buttonBluetoothScanPermission;
    ImageButton buttonBluetoothConnectPermission;
    AppCompatButton buttonStartRecording;
    AppCompatButton buttonFinishRecording;

    AppCompatButton buttonCancelRecording;
    AppCompatButton buttonUpdateNearByLocations;

    boolean isUpdatingLocations = false;
    Spinner locationSpinner;

    TextView textViewCC0;
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

    boolean auxiliaryLocationMode = false;

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

    AppCompatButton buttonOpenMapInBrowser;
    AppCompatButton buttonImpressumDataProtection; //links to webpage with this statemenet
    ConstraintLayout constraintLayoutMap;

    public boolean invalidateLocations = false;

    public String transmissionState = "";

    Logic logic;

    int buttonColorEnabled = Color.parseColor("#FAA5DA");
    int buttonColorDisabled = Color.parseColor("#222222");

    LocationData selectedLocation = null;

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

        textViewCC0 = findViewById(R.id.textViewCC0);
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

        buttonUpdateNearByLocations.setBackgroundColor(buttonColorEnabled);
        buttonFinishRecording.setBackgroundColor(buttonColorEnabled);
        buttonOpenMapInBrowser.setBackgroundColor(buttonColorEnabled);
        buttonImpressumDataProtection.setBackgroundColor(buttonColorEnabled);
        buttonCancelRecording.setBackgroundColor(buttonColorEnabled);
        buttonFinishRecording.setBackgroundColor(buttonColorEnabled);

        UIUpdater.postDelayed(Update, 2000);

        logic.spatialManager.searchRadius = searchRadius;

        ColorDrawable backgroundDrawable = new ColorDrawable(0xFF444444); // Dark grey color
        //locationSpinner.setBackgroundColor(0xFF444444);
        // Set the background drawable to the Spinner's popup using reflection
        //try {
        //    Field popup = Spinner.class.getDeclaredField("mPopup");
        //    popup.setAccessible(true);
//
        //    Object popupWindow = popup.get(locationSpinner);
//
        //    if (popupWindow instanceof ListPopupWindow) {
        //        ListPopupWindow listPopupWindow = (ListPopupWindow) popupWindow;
        //        listPopupWindow.setBackgroundDrawable(backgroundDrawable);
        //    }
        //} catch (Exception e) {
        //    e.printStackTrace();
        //}
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
        //PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        //wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IndoorCO2App::SensorDataRecordingWakelock");
        //wakeLock.acquire();

        //TODO:
        textViewCC0.setVisibility(View.GONE);
        buttonFinishRecording.setEnabled(false);
        buttonFinishRecording.setBackgroundColor(buttonColorDisabled);
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
        buttonFinishRecording.setBackgroundColor(buttonColorDisabled);
        buttonFinishRecording.setTextColor(Color.LTGRAY);
        buttonFinishRecording.setText(("Submitting"));
        ApiGatewayCaller.sendJsonToApiGateway(json,this);

        //// Release the wakelock
        //if (wakeLock != null && wakeLock.isHeld()) {
        //    wakeLock.release();
        //}
    }

    public void OnTransmissionSuccess()
    {
        buttonFinishRecording.setTextColor(Color.LTGRAY);
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
                buttonFinishRecording.setBackgroundColor(buttonColorEnabled);
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
            //// Release the wakelock
            //if (wakeLock != null && wakeLock.isHeld()) {
            //    wakeLock.release();
            //}
            buttonCancelRecording.setText("Cancel");
            firstCancelStepTriggered = false;
        }

    }


    public void OnStopRecordingChangeUI()
    {
        textViewCC0.setVisibility(View.GONE);
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
        buttonUpdateNearByLocations.setTextColor(Color.LTGRAY);
        buttonUpdateNearByLocations.setText("updating Locations...");
        buttonUpdateNearByLocations.setEnabled(false);
        buttonUpdateNearByLocations.setBackgroundColor(buttonColorDisabled);
        isUpdatingLocations = true;
        logic.spatialManager.overpassModule.FetchNearbyBuildings(this);

    }

    public void OnButtonClickUpdateLocationSpinnerFinished()
    {
        buttonUpdateNearByLocations.setText("Update Nearby Locations");
        buttonUpdateNearByLocations.setTextColor(Color.BLACK);
        //buttonUpdateNearByLocations.setEnabled(true);
        //buttonUpdateNearByLocations.setBackgroundColor(Color.MAGENTA);
        isUpdatingLocations = false;
    }

    public void UpdateLocationSelectionSpinner()
    {
        ArrayList<String> defaultMessageList = new ArrayList<>();
        defaultMessageList.add("No Locations found, search again");
        if(!invalidateLocations) return;
        ArrayAdapter<LocationData> adapter = new LocationDataAdapter(this, logic.spatialManager.overpassModule.locationData);
        adapter.setDropDownViewResource(R.layout.custom_spinner_item);

        locationSpinner.setAdapter(adapter);
        locationSpinner.setEnabled(true);
        if(logic.spatialManager.overpassModule.locationData.isEmpty())
        {
            locationSpinner.setEnabled(false);
            ArrayAdapter<String> emptyAdapter = new ArrayAdapter<String>(this,R.layout.custom_spinner_item,defaultMessageList);
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
                buttonStartRecording.setTextColor(Color.LTGRAY);
                buttonStartRecording.setText("Start Recording (needs Location)");
                buttonStartRecording.setEnabled(false);
                buttonStartRecording.setBackgroundColor(buttonColorDisabled);
            }
            else
            {
                buttonStartRecording.setEnabled(true);
                buttonStartRecording.setBackgroundColor(buttonColorEnabled);
                buttonStartRecording.setTextColor(Color.BLACK);
                buttonStartRecording.setText("Start Recording");
            }
        }
        else
        {
            buttonStartRecording.setTextColor(Color.LTGRAY);
            buttonStartRecording.setText("Start Recording (not all requirements met)");
            buttonStartRecording.setEnabled(false);
            buttonStartRecording.setBackgroundColor(buttonColorDisabled);
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
                    if(showExactLocation)
                    {
                        textViewLocationStatus.setText(String.format("Lat:" +"%.6f", logic.spatialManager.myLatitude) + " | Lon: " + String.format("%.6f", logic.spatialManager.myLongitude) + " (tap to hide)");
                    }
                    else
                    {
                        textViewLocationStatus.setText(String.format("Lat:" +"%.1f" + "***", logic.spatialManager.myLatitude) + " | Lon: " + String.format("%.1f" + "***", logic.spatialManager.myLongitude) + " (tap to show)");
                    }

                }

                if(isUpdatingLocations==false)
                {
                    buttonUpdateNearByLocations.setEnabled(true);
                    buttonUpdateNearByLocations.setBackgroundColor(buttonColorEnabled);
                }

            }
            else
            {
                textViewLocationStatus.setText("GPS enabled and Location permissions granted. Getting first Location Info. This might take a minute");
                buttonUpdateNearByLocations.setEnabled(false);
                buttonUpdateNearByLocations.setBackgroundColor(buttonColorDisabled);
            }
        }
        else
        {
            textViewLocationStatus.setText("");
            if(!GPSEnabled) textViewLocationStatus.append("GPS not enabled ");
            if(!locationPermission) textViewLocationStatus.append("Location Permission missing");
            buttonUpdateNearByLocations.setEnabled(false);
            buttonUpdateNearByLocations.setBackgroundColor(buttonColorDisabled);
        }

        if(logic.aranetManager.isRecording)
        {
            int[] co2Data = logic.aranetManager.GetCO2Data();

            //if(co2Data.length>0)
            //{
                lineChartView.setData(co2Data);
                chartRangeSlider.SetDataRange(co2Data.length);
            //}

            if(co2Data.length<5 || (chartRangeSlider.getMaxValue()-chartRangeSlider.getMinValue() <4))
            {
                buttonFinishRecording.setEnabled(false);
                buttonFinishRecording.setBackgroundColor(buttonColorDisabled);
                buttonFinishRecording.setTextColor(Color.LTGRAY);
                buttonFinishRecording.setText("Submit (needs 5 minutes of Data)");
            }
            else
            {
                buttonFinishRecording.setEnabled(true);
                buttonFinishRecording.setBackgroundColor(buttonColorEnabled);
                buttonFinishRecording.setTextColor(Color.BLACK);
                buttonFinishRecording.setText("Submit Data");
            }
        }
        if(transmissionState == "failure")
        {
            buttonFinishRecording.setTextColor(Color.BLACK);
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

    public void OnShowExactLocation(View view)
    {
        showExactLocation = !showExactLocation;
    }
}