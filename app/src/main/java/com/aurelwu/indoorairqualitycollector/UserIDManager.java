package com.aurelwu.indoorairqualitycollector;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public class UserIDManager {
    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_NAME = "IndoorDataCollectorUserID";
    private static final String TargetDeviceID = "IndoorDataCollectorDeviceID";
    private static final String SelectedDeviceTypeID = "IndoorDataCollectorDeviceType";

    public static String getUserID(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String userID = sharedPreferences.getString(PREF_USER_ID, null);

        // If no user ID is stored, generate a new one and save it
        if (userID == null) {
            userID = generateRandomID();
            saveUserID(context, userID);
        }

        return userID;
    }

    public static void saveTargetDeviceID(Context context, String deviceID)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TargetDeviceID, deviceID);
        editor.apply();
    }

    public static String loadTargetDeviceID(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String deviceID = sharedPreferences.getString(TargetDeviceID,"");
        return deviceID;
    }

    public static void SaveSelectedDeviceType(Context context, String deviceType)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SelectedDeviceTypeID, deviceType);
        editor.apply();
    }
    public static String LoadSelectedDeviceType(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String device = sharedPreferences.getString(SelectedDeviceTypeID,"Aranet"); //we default to Aranet if nothing set
        return device;
    }

    private static String generateRandomID() {
        return UUID.randomUUID().toString();
    }

    private static void saveUserID(Context context, String userID) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_USER_ID, userID);
        editor.apply();
    }

    public static String GetEncryptedID(Context context, String deviceID) {
        String userID = getUserID(context);
        String concatenatedString = userID + deviceID;
        return EncryptionManager.encryptString(concatenatedString);
    }
}