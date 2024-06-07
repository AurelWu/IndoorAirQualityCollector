package com.aurelwu.indoorairqualitycollector;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OverpassModule
{
    private SpatialManager spatialManager;

    public ArrayList<LocationData> locationData;

    public OverpassModule(SpatialManager spatialManager)
    {
        this.spatialManager = spatialManager;
        this.locationData = new ArrayList<>();
    }
    private String buildOverpassQuery(double latitude,double longitude,double radius) {
        locationData = new ArrayList<>();
        // Construct the Overpass query with the specified radius and location
        //TODO: add remaining categories of amenities and maybe other
        return "[out:json];" +
                "(" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[shop];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=bar];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=place_of_worship];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=pub];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=restaurant];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=cafe];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=fast_food];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=food_court];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=ice_cream];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=college];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=dancing_school];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=driving_school];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=kindergarten];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=language_school];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=library];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=research_institute];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=music_school];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=school];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=university];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=hospital];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=clinic];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[amenity=doctors];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[healthcare];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[tourism=museum];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[tourism=gallery];" +
                "  nwr(around:" + radius + "," + latitude + "," + longitude + ")[tourism=hotel];" +
                ");" +
                "out center qt;";
        //TODO: Add hotel?
    }

    private void parseOverpassResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            JSONArray elements = json.getJSONArray("elements");
//
            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                String type = element.getString("type");
                long id = element.getLong("id");
                JSONObject center = element.optJSONObject("center");
                double lon = 0;
                double lat = 0;
                if(center==null)
                {
                    lon = element.getDouble("lon");
                    lat = element.getDouble("lat");
                }

                if(center!=null)
                {
                    lon = center.getDouble("lon");
                    lat = center.getDouble("lat");
                }

                JSONObject tags = element.getJSONObject("tags");
                String name = tags.optString("name", ""); // Use optString to handle missing or null values
                LocationData bd = new LocationData(type,id,name,lat,lon, spatialManager.myLatitude, spatialManager.myLongitude);
                locationData.add(bd);
            }
        } catch (JSONException e) {

        }
        Collections.sort(locationData, new Comparator<LocationData>() {
            @Override
            public int compare(LocationData point1, LocationData point2) {
                // Compare the distances
                return Double.compare(point1.distanceToGivenLocation, point2.distanceToGivenLocation);
            }
        });

        spatialManager.mainActivity.invalidateLocations = true; //bit dirty but whatever
    }

    public void FetchNearbyBuildings() {
        Log.d("fetchNearbyBuildings", "fetch NearbyBuildings called");
        locationData.clear();
        String overpassQuery = buildOverpassQuery(spatialManager.myLatitude, spatialManager.myLongitude, spatialManager.searchRadius);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "data=" + overpassQuery);
        // Make an HTTP request to Overpass API
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .post(requestBody)  // Set request method to POST and attach the request body
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // Handle failure
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    parseOverpassResponse(jsonData);
                    // Update UI on the main thread if necessary
                } else {
                    // Handle unsuccessful response
                }
            }
        });
    }

}
