package com.example.indoorairqualitycollector;


import android.os.AsyncTask;
import android.util.Log;
import java.io.IOException;
import okhttp3.*;

public class ApiGatewayCaller {

    public static void sendJsonToApiGateway(String json) {
        new NetworkTask().execute(json);
    }

    private static class NetworkTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try {
                String json = params[0];

                // Create JSON request body
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(JSON, json);

                // Create OkHttpClient instance
                OkHttpClient client = new OkHttpClient();

                // Create POST request
                Request request = new Request.Builder()
                        .url("https://wzugdkxj15.execute-api.eu-central-1.amazonaws.com/Standard/CO2")
                        .post(requestBody)
                        .build();

                // Execute request
                Response response = client.newCall(request).execute();

                // Handle response
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    // Handle successful response
                    Log.d("sendJsonToApiGateway", "Response: " + responseBody);
                    System.out.println("Response: " + responseBody);
                } else {
                    // Handle unsuccessful response
                    Log.d("sendJsonToApiGateway", "Error: " + response.code() + " " + response.message());
                    System.out.println("Error: " + response.code() + " " + response.message());
                }

                // Close response
                response.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // This method is executed on the main UI thread after doInBackground() completes
            // You can update UI or perform any post-processing here
        }
    }
}