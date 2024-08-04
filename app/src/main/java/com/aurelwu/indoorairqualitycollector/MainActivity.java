package com.aurelwu.indoorairqualitycollector;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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

    //DONE? TODO: write to recoveryData when recording
    //DONE TODO: delete recoveryData when transmitted succesfully/canceled
    //TODO: IF recoveryData is not zeroed out, then display "resume recording" button if last update wasnt more than 30 minutes ago
    public static RecoveryData recoveryData;

    public String selectedCO2Device = "Aranet";
    private boolean manualTransmissionMode = false;
    private boolean enableManualRecordingButton = false;
    private boolean showExactLocation = false;
    Switch switchAdvancedOptions;
    Switch switchPrerecording;

    ImageButton buttonGPSStatus;
    ImageButton buttonLocationPermissionStatus;
    ImageButton buttonBluetooth;

    ImageButton buttonBluetoothScanPermission;
    ImageButton buttonBluetoothConnectPermission;
    AppCompatButton buttonStartRecording;

    AppCompatButton buttonResumeRecording;
    AppCompatButton buttonStartManualRecording;
    AppCompatButton buttonFinishRecording;

    AppCompatButton buttonCancelRecording;
    AppCompatButton buttonUpdateNearByLocations;

    boolean isUpdatingLocations = false;
    Spinner locationSpinner;

    TextView textViewTrimSliderInfo;
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
    TextInputLayout textInputLayoutManualName;
    TextInputLayout textInputLayoutManualAddress;
    TextInputLayout textInputLayoutDeviceID;
    TextInputEditText textInputEditTextDeviceID;
    TextInputEditText textInputEditTextCustomNotes;
    TextInputEditText textInputEditTextManualName;
    TextInputEditText textInputEditTextManualAddress;

    LinearLayout CO2DeviceSelectorContainer;

    LinearLayout layoutOccupancy;

    LinearLayout chartContainer;
    LineChartView lineChartView;

    LinearLayout chartRangeSliderContainer;
    IntegerRangeSlider chartRangeSlider;

    CheckBox checkBoxWindowsDoors;
    CheckBox checkBoxVentilationSystem;

    AppCompatButton buttonOpenMapInBrowser;
    AppCompatButton buttonImpressumDataProtection; //links to webpage with this statemenet
    //ConstraintLayout constraintLayoutMap;

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

        CO2DeviceSelectorContainer = findViewById(R.id.LinearLayoutCO2DeviceSelector);
        Spinner CO2DeviceSpinner = findViewById(R.id.spinnerSelectCO2Device);

        switchAdvancedOptions = findViewById(R.id.switchAdvanced);
        switchPrerecording = findViewById(R.id.switchPrerecording);

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
        buttonResumeRecording = findViewById(R.id.buttonResumeRecording);
        buttonStartRecording = findViewById(R.id.buttonStartRecording);
        buttonStartManualRecording = findViewById(R.id.buttonStartRecordingManual);
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
        //constraintLayoutMap = findViewById(R.id.ConstraintLayoutShowMap);


        layoutLocationSelection = findViewById(R.id.LinearLayoutLocationSelector);
        layoutSearchRangeSelection = findViewById(R.id.LinearLayoutSearchRange);
        layoutStopRecording = findViewById(R.id.linearLayoutEndRecording);
        chartContainer = findViewById(R.id.LinearLayoutLineChartContainer);
        lineChartView = new LineChartView(this);
        lineChartView.ProvideMainActivity(this);
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

        textInputLayoutManualName = findViewById(R.id.TextInputManualName);
        textInputLayoutManualName.setVisibility(View.GONE);
        textInputEditTextManualName = findViewById(R.id.TextInputEditTextManualName);

        textInputLayoutManualAddress = findViewById(R.id.TextInputManualAddress);
        textInputLayoutManualAddress.setVisibility(View.GONE);
        textInputEditTextManualAddress = findViewById(R.id.TextInputEditTextManualAddress);

        textInputEditTextDeviceID = findViewById(R.id.TextInputEditTextSetDevice);
        textInputLayoutDeviceID = findViewById(R.id.TextInputLayoutSetDeviceID);

        layoutOccupancy = findViewById(R.id.LinearLayoutOccupancy);
        buttonCancelRecording = findViewById(R.id.buttonAbort);
        locationSpinner = findViewById(R.id.spinnerSelectLocation);

        recoveryData = RecoveryData.ReadFromPreferences(this);

        buttonResumeRecording.setVisibility(View.GONE);
        buttonStartManualRecording.setEnabled(false);
        buttonStartManualRecording.setBackgroundColor(buttonColorDisabled);
        buttonStartManualRecording.setTextColor(Color.LTGRAY);
        buttonStartManualRecording.setTextColor(Color.LTGRAY);
        buttonUpdateNearByLocations.setBackgroundColor(buttonColorEnabled);
        buttonFinishRecording.setBackgroundColor(buttonColorEnabled);
        buttonOpenMapInBrowser.setBackgroundColor(buttonColorEnabled);
        buttonImpressumDataProtection.setBackgroundColor(buttonColorEnabled);
        buttonCancelRecording.setBackgroundColor(buttonColorEnabled);
        buttonFinishRecording.setBackgroundColor(buttonColorEnabled);

        UIUpdater.postDelayed(Update, 100);

        logic.spatialManager.searchRadius = searchRadius;

        switchPrerecording.setVisibility(View.GONE);
        textInputEditTextDeviceID.setVisibility(View.GONE);
        textInputEditTextDeviceID.setText(UserIDManager.loadTargetDeviceID(this));

        switchAdvancedOptions.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    switchPrerecording.setVisibility(View.VISIBLE);
                    textInputEditTextDeviceID.setVisibility(View.VISIBLE);
                    textInputLayoutDeviceID.setVisibility(View.VISIBLE);
                } else {
                    switchPrerecording.setVisibility(View.GONE);
                    textInputEditTextDeviceID.setVisibility(View.GONE);
                    textInputLayoutDeviceID.setVisibility(View.GONE);
                }
            }
        });

        textInputEditTextDeviceID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                UserIDManager.saveTargetDeviceID(logic.spatialManager.mainActivity,textInputEditTextDeviceID.getText().toString());
                logic.aranetManager.aranetDevice=null;
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }

        });

        String[] items = {"Aranet", "Airvalent", "Inkbird IAM-T1"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        CO2DeviceSpinner.setAdapter(adapter);

        CO2DeviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Call a method whenever an item is selected
                onCO2DeviceSelected(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Optionally handle the case where no item is selected
            }
        });

        String sel = UserIDManager.LoadSelectedDeviceType(this);
        if (sel != null) {
            selectedCO2Device=sel;
            int position = adapter.getPosition(sel);
            if (position >= 0) {
                CO2DeviceSpinner.setSelection(position);
            }
        }



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

    private void onCO2DeviceSelected(String selectedDevice)
    {
        selectedCO2Device=selectedDevice;
        UserIDManager.SaveSelectedDeviceType(this,selectedDevice);
    }

    public void OnStartRecordingButton(View view)
    {


        manualTransmissionMode=false;
        transmissionState = "none";
        selectedLocation = (LocationData)locationSpinner.getSelectedItem();
        if(selectedLocation==null)
        {
            //maybe display message that no location selected?)
            return;
        }
        CO2DeviceSelectorContainer.setVisibility(View.GONE);
        textInputEditTextCustomNotes.setText("");
        textViewCC0.setVisibility(View.GONE);
        buttonResumeRecording.setVisibility(View.GONE);
        buttonFinishRecording.setEnabled(false);
        buttonFinishRecording.setBackgroundColor(buttonColorDisabled);
        layoutStopRecording.setVisibility(View.VISIBLE);
        layoutSearchRangeSelection.setVisibility(View.GONE);
        layoutLocationSelection.setVisibility(View.GONE);
        buttonStartRecording.setVisibility(View.GONE);
        buttonStartManualRecording.setVisibility(View.GONE);
        buttonUpdateNearByLocations.setVisibility(View.GONE);
        chartContainer.setVisibility(View.VISIBLE);
        chartRangeSliderContainer.setVisibility(View.VISIBLE);
        lineChartView.setVisibility(View.VISIBLE);
        lineChartView.invalidate();
        layoutCheckboxes.setVisibility(View.VISIBLE);
        textInputLayoutCustomNotes.setVisibility(View.VISIBLE);
        layoutOccupancy.setVisibility(View.GONE); // For now we won't track this, its subjective anyways and less UI Elements = better
        buttonOpenMapInBrowser.setVisibility(View.GONE);
        buttonImpressumDataProtection.setVisibility(View.GONE);
        chartRangeSlider.ReInit();

        logic.StartNewRecording(selectedLocation.ID,selectedLocation.type, selectedLocation.Name,selectedLocation.latitude,selectedLocation.longitude,System.currentTimeMillis(),true,switchPrerecording.isChecked());
        recoveryData.locationType= selectedLocation.type;
        recoveryData.locationID=selectedLocation.ID ;
        recoveryData.locationName= selectedLocation.Name;
        recoveryData.locationLon=(float)selectedLocation.longitude;
        recoveryData.locationLat=(float)selectedLocation.latitude;
        recoveryData.startTime=System.currentTimeMillis();
        recoveryData.timeOfLastUpdate=System.currentTimeMillis();
        recoveryData.WriteToPreferences(this);
        checkBoxVentilationSystem.setChecked(false);
        checkBoxWindowsDoors.setChecked(false);
        occupancyLevel = "undefined";
        textInputLayoutDeviceID.setVisibility(View.GONE);
        switchAdvancedOptions.setVisibility(View.GONE);
        switchPrerecording.setVisibility(View.GONE);
        textInputLayoutDeviceID.setVisibility(View.GONE);
        textInputEditTextDeviceID.setVisibility(View.GONE);

    }

    public void OnFinishAndSubmitRecording(View view)
    {
        logic.FinishRecording(checkBoxWindowsDoors.isChecked(),checkBoxVentilationSystem.isChecked(),occupancyLevel,textInputEditTextCustomNotes.getText().toString(), textInputEditTextManualName.getText().toString(),textInputEditTextManualAddress.getText().toString(), manualTransmissionMode);
        //chartRangeSlider.getMinValue();
        transmissionState = "none";
        buttonFinishRecording.setEnabled(false);
        buttonFinishRecording.setBackgroundColor(buttonColorDisabled);
        buttonFinishRecording.setTextColor(Color.LTGRAY);
        buttonFinishRecording.setText(("Submitting"));

        if(!manualTransmissionMode)
        {
            String json = logic.GenerateJSONToTransmit(chartRangeSlider.getMinValue(),chartRangeSlider.getMaxValue());
            ApiGatewayCaller.sendJsonToApiGateway(json,this);
        }
        else if(manualTransmissionMode)
        {
            String json = logic.GenerateManualModeJSONToTransmit(chartRangeSlider.getMinValue(),chartRangeSlider.getMaxValue());
            ApiGatewayCaller.sendJsonToApiGateway(json,this);
        }
    }

    public void OnTransmissionSuccess()
    {

        RecoveryData.DeleteRecoveryData(this);
        recoveryData = RecoveryData.ReadFromPreferences(this);
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

    //public void OnCancelRecording(View view)
    //{
    //    if(!firstCancelStepTriggered)
    //    {
    //        firstCancelStepTriggered = true;
    //        buttonCancelRecording.setText("Confirm Cancel");
    //    }
    //    else
    //    {
    //        logic.aranetManager.FinishRecording();
    //        OnStopRecordingChangeUI();
    //
    //        buttonCancelRecording.setText("Cancel");
    //        firstCancelStepTriggered = false;
    //    }
    //
    //}

    public void OnCancelRecording(View view) {
        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

        // Set the dialog title and message
        builder.setTitle("Confirm Cancel");
        builder.setMessage("Are you sure you want to cancel the recording?");

        // Set the positive button (confirmation)
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                logic.aranetManager.FinishRecording();
                RecoveryData.DeleteRecoveryData(logic.spatialManager.mainActivity);
                recoveryData = RecoveryData.ReadFromPreferences(logic.spatialManager.mainActivity);
                OnStopRecordingChangeUI();

            }
        });

        // Set the negative button (cancellation)
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        // Show the dialog
        builder.create().show();
    }


    public void OnStopRecordingChangeUI()
    {
        RecoveryData.DeleteRecoveryData(this);
        CO2DeviceSelectorContainer.setVisibility(View.VISIBLE);
        textViewCC0.setVisibility(View.GONE);
        buttonStartRecording.setVisibility(View.VISIBLE);
        buttonStartManualRecording.setVisibility(View.VISIBLE);
        layoutStopRecording.setVisibility(View.GONE);
        layoutSearchRangeSelection.setVisibility(View.VISIBLE);
        layoutLocationSelection.setVisibility(View.VISIBLE);
        buttonUpdateNearByLocations.setVisibility(View.VISIBLE);
        buttonOpenMapInBrowser.setVisibility(View.VISIBLE);
        buttonImpressumDataProtection.setVisibility(View.VISIBLE);
        //constraintLayoutMap.setVisibility(View.VISIBLE);
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
        enableManualRecordingButton = false;
        buttonStartManualRecording.setEnabled(false);
        buttonStartManualRecording.setBackgroundColor(buttonColorDisabled);
        buttonStartManualRecording.setTextColor(Color.LTGRAY);
        buttonStartManualRecording.setText("Start Manual Recording (Available after Location Update)");
        textInputLayoutManualAddress.setVisibility(View.GONE);
        textInputLayoutManualName.setVisibility(View.GONE);
        switchAdvancedOptions.setVisibility(View.VISIBLE);
        if(switchAdvancedOptions.isChecked())
        {
            switchPrerecording.setVisibility(View.VISIBLE);
            textInputEditTextDeviceID.setVisibility(View.VISIBLE);
            textInputLayoutDeviceID.setVisibility(View.VISIBLE);
        }
        else
        {
            switchPrerecording.setVisibility(View.GONE);
            textInputEditTextDeviceID.setVisibility(View.GONE);
            textInputLayoutDeviceID.setVisibility(View.GONE);
        }
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
        enableManualRecordingButton = true;
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
            ArrayAdapter<String> emptyAdapter = new ArrayAdapter<String>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,defaultMessageList);
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
            String targetID = UserIDManager.loadTargetDeviceID(this);
            if(logic.aranetManager.aranetDevice == null && UserIDManager.loadTargetDeviceID(this).length()<2)
            {
                textViewSensorStatus.setText("Device not found yet" );
                if(selectedCO2Device.equals("Inkbird IAM-T1"))
                {
                    textViewSensorStatus.setText("Device not found yet, needs to be bonded and Device name must include 'IAM-T1'" );
                }
            }
            else if(logic.aranetManager.aranetDevice == null && UserIDManager.loadTargetDeviceID(this).length()>=2)
            {
                textViewSensorStatus.setText("Device with ID: " +targetID + " not found yet");
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

        if(recoveryData.locationID!=0 && buttonStartRecording.getVisibility() == View.VISIBLE) //check vs normal recording button ensures that it doesnt show in recording mode once we submit the data
        {
            buttonResumeRecording.setVisibility(View.VISIBLE);
            if(GPSEnabled && locationPermission && bluetoothEnabled && bluetoothConnectPermission &&bluetoothScanPermission && updateIntervalSetTo1Minute && deviceFound)
            {
                buttonResumeRecording.setEnabled(true);
                buttonResumeRecording.setBackgroundColor(buttonColorEnabled);
                buttonResumeRecording.setTextColor(Color.BLACK);
            }
            else
            {
                buttonResumeRecording.setEnabled(false);
                buttonResumeRecording.setBackgroundColor(buttonColorDisabled);
                buttonResumeRecording.setTextColor(Color.LTGRAY);
            }
        }
        else
        {
            buttonResumeRecording.setVisibility(View.GONE);
        }
        if(logic.aranetManager.isRecording)
        {
            buttonResumeRecording.setVisibility(View.GONE);
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

        if(GPSEnabled && locationPermission && bluetoothEnabled && bluetoothConnectPermission && bluetoothScanPermission && updateIntervalSetTo1Minute && deviceFound && enableManualRecordingButton)
        {
            buttonStartManualRecording.setEnabled(true);
            buttonStartManualRecording.setTextColor(Color.BLACK);
            buttonStartManualRecording.setText("Start Manual Recording");
            buttonStartManualRecording.setBackgroundColor(buttonColorEnabled);
        }
        else
        {
            buttonStartManualRecording.setEnabled(false);
            buttonStartManualRecording.setTextColor(Color.LTGRAY);
            buttonStartManualRecording.setText("Start Manual Recording (not all requirements met) ");
            buttonStartManualRecording.setBackgroundColor(buttonColorDisabled);
        }



        if(GPSEnabled && locationPermission)
        {
            if(logic.spatialManager.myLatitude != 0 || logic.spatialManager.myLongitude != 0)
            {
                if(logic.aranetManager.isRecording && selectedLocation!=null)
                {
                    //textViewLocationStatus.setText("Recording data of Location: " + selectedLocation.Name+"\r\n"+ String.format("%.6f", selectedLocation.latitude) + " | " + String.format("%.6f", selectedLocation.longitude ));
                    textViewLocationStatus.setText("Recording data of Location: " + selectedLocation.Name);
                    if(logic.aranetManager.lastUpdateFailed==true)
                    {
                        textViewLocationStatus.append(" | last update not successful (Status: ) " + logic.aranetManager.failureID);
                    }
                }
                else if(logic.aranetManager.isRecording && recoveryData.locationName !=null && recoveryData.locationName.length()>0)
                {
                    textViewLocationStatus.setText("Recording data of Location: " + recoveryData.locationName);
                }
                else
                {
                    if(showExactLocation)
                    {
                        textViewLocationStatus.setText(String.format("Lat:" +"%.6f", logic.spatialManager.myLatitude) + " | Lon: " + String.format("%.6f", logic.spatialManager.myLongitude) + " (tap to hide)");
                        if(logic.aranetManager.lastUpdateFailed==true)
                        {
                            textViewLocationStatus.append(" | last update not successful (Status: ) " + logic.aranetManager.failureID);
                        }
                    }
                    else
                    {
                        textViewLocationStatus.setText(String.format("Lat:" +"%.1f" + "***", logic.spatialManager.myLatitude) + " | Lon: " + String.format("%.1f" + "***", logic.spatialManager.myLongitude) + " (tap to show)");
                        if(logic.aranetManager.lastUpdateFailed==true)
                        {
                            textViewLocationStatus.append(" | last update not successful (Status: ) " + logic.aranetManager.failureID);
                        }
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
            buttonUpdateNearByLocations.setTextColor(Color.LTGRAY);
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
            if(!manualTransmissionMode)
            {
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
            else //is manual transmission Mode
            {
                Boolean anyConditionsMissing = false;

                if(co2Data.length<5 || (chartRangeSlider.getMaxValue()-chartRangeSlider.getMinValue() <4))
                {
                        buttonFinishRecording.setText("Submit Data (needs 5 Minutes of Data");
                        buttonFinishRecording.setEnabled(false);
                        buttonFinishRecording.setBackgroundColor(buttonColorDisabled);
                        buttonFinishRecording.setTextColor(Color.LTGRAY);
                        anyConditionsMissing=true;
                }
                else if(textInputEditTextManualName.getText().toString().length() <1)
                {
                    buttonFinishRecording.setText("Submit Data (needs Location Name)");
                    buttonFinishRecording.setEnabled(false);
                    buttonFinishRecording.setBackgroundColor(buttonColorDisabled);
                    buttonFinishRecording.setTextColor(Color.LTGRAY);
                }
                else if(textInputEditTextManualAddress.getText().toString().length() <1)
                {
                    buttonFinishRecording.setText("Submit Data (needs Location Address)");
                    buttonFinishRecording.setEnabled(false);
                    buttonFinishRecording.setBackgroundColor(buttonColorDisabled);
                    buttonFinishRecording.setTextColor(Color.LTGRAY);
                }
                else
                {
                    buttonFinishRecording.setEnabled(true);
                    buttonFinishRecording.setBackgroundColor(buttonColorEnabled);
                    buttonFinishRecording.setTextColor(Color.BLACK);
                    buttonFinishRecording.setText("Submit Data");
                }
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
                if(manualTransmissionMode)
                {
                    logic.submissionDataManual.LongitudeData.add(logic.spatialManager.myLongitude);
                    logic.submissionDataManual.LatitudeData.add((logic.spatialManager.myLatitude));
                }
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


    public void OnConfirmedManualRecording(View view)
    {
        transmissionState = "none";
        manualTransmissionMode=true;
        textViewCC0.setVisibility(View.GONE);
        buttonFinishRecording.setEnabled(false);
        buttonFinishRecording.setBackgroundColor(buttonColorDisabled);
        CO2DeviceSelectorContainer.setVisibility(View.GONE);
        layoutStopRecording.setVisibility(View.VISIBLE);
        layoutSearchRangeSelection.setVisibility(View.GONE);
        layoutLocationSelection.setVisibility(View.GONE);
        buttonStartRecording.setVisibility(View.GONE);
        buttonStartManualRecording.setVisibility(View.GONE);
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
        //constraintLayoutMap.setVisibility(View.GONE);
        chartRangeSlider.ReInit();

        logic.StartNewManualRecording(System.currentTimeMillis(),switchPrerecording.isChecked());
        checkBoxVentilationSystem.setChecked(false);
        checkBoxWindowsDoors.setChecked(false);
        occupancyLevel = "undefined";
        textInputLayoutManualName.setVisibility(View.VISIBLE);
        textInputLayoutManualAddress.setVisibility(View.VISIBLE);
        textInputEditTextManualAddress.setText("");
        textInputEditTextManualName.setText("");
        //display a popup telling people to only use this in emergencies
    }

    public void OnStartManualRecording(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Only use this recording mode if the location is not in the List above or if receiving Locations does not work currently. Recordings in this mode are not put into the map instantly but manually looked at and then added to the Map if the location can be validated. Using this mode the exact GPS Coordinates taken during the recording duration will be submitted at the end!")
                .setPositiveButton("Understood", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Perform the confirm action
                        OnConfirmedManualRecording(view);
                        dialog.dismiss(); // Close the dialog
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dialog.dismiss(); // Close the dialog
                    }
                });
        builder.create().show();
    }

    public void OnOpenMapInBrowser(View view)
    {
        String mapURL = "https://indoorco2map.com/";
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

    public void OnResumeRecordingButton(View view)
    {
        manualTransmissionMode=false;
        transmissionState = "none";


        logic.StartNewRecording(recoveryData.locationID,recoveryData.locationType, recoveryData.locationName, recoveryData.locationLat, recoveryData.locationLon, recoveryData.startTime,false,switchPrerecording.isChecked());

        //This UI stuff should really be put in a method itself so it doesnt duplicate/triplicate
        textInputEditTextCustomNotes.setText("");
        textViewCC0.setVisibility(View.GONE);
        buttonResumeRecording.setVisibility(View.GONE);
        buttonFinishRecording.setEnabled(false);
        buttonFinishRecording.setBackgroundColor(buttonColorDisabled);
        layoutStopRecording.setVisibility(View.VISIBLE);
        layoutSearchRangeSelection.setVisibility(View.GONE);
        layoutLocationSelection.setVisibility(View.GONE);
        buttonStartRecording.setVisibility(View.GONE);
        buttonStartManualRecording.setVisibility(View.GONE);
        buttonUpdateNearByLocations.setVisibility(View.GONE);
        chartContainer.setVisibility(View.VISIBLE);
        chartRangeSliderContainer.setVisibility(View.VISIBLE);
        lineChartView.setVisibility(View.VISIBLE);
        lineChartView.invalidate();
        layoutCheckboxes.setVisibility(View.VISIBLE);
        textInputLayoutCustomNotes.setVisibility(View.VISIBLE);
        layoutOccupancy.setVisibility(View.GONE); // For now we won't track this, its subjective anyways and less UI Elements = better
        buttonOpenMapInBrowser.setVisibility(View.GONE);
        buttonImpressumDataProtection.setVisibility(View.GONE);
        chartRangeSlider.ReInit();
        checkBoxVentilationSystem.setChecked(false);
        checkBoxWindowsDoors.setChecked(false);
        occupancyLevel = "undefined";
        textInputLayoutDeviceID.setVisibility(View.GONE);
        switchAdvancedOptions.setVisibility(View.GONE);
        switchPrerecording.setVisibility(View.GONE);
        textInputLayoutDeviceID.setVisibility(View.GONE);
        textInputEditTextDeviceID.setVisibility(View.GONE);
        CO2DeviceSelectorContainer.setVisibility(View.GONE);


    }
}