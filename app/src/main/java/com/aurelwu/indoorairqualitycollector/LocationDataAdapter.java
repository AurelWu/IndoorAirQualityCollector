package com.aurelwu.indoorairqualitycollector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class LocationDataAdapter extends ArrayAdapter<LocationData> {

    public LocationDataAdapter(@NonNull Context context, @NonNull List<LocationData> locations) {
        super(context, 0, locations);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.custom_spinner_item, parent, false);
        }
        TextView textView = convertView.findViewById(R.id.text1);
        LocationData locationData = getItem(position);
        textView.setText(locationData.GetSpinnerstring());
        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.custom_spinner_item, parent, false);
        }
        TextView textView = convertView.findViewById(R.id.text1);
        LocationData locationData = getItem(position);
        textView.setText(locationData.GetSpinnerstring());
        return convertView;
    }
}
