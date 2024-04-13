package com.example.indoorairqualitycollector;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiGatewayCaller {

    public static String successState;
    public static MainActivity mainActivity;

    // Modify sendJsonToApiGateway to accept callback
    public static void sendJsonToApiGateway(String json, MainActivity m)
    {
        mainActivity = m;
        successState = "";
        new NetworkTask().execute(json);
    }

    private static class NetworkTask extends AsyncTask<String, Void, Void> {



        public NetworkTask() {;
;
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                String json = params[0];

                // Create JSON request body
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(JSON, json);

                OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

                clientBuilder.readTimeout(20, TimeUnit.SECONDS);
                clientBuilder.connectTimeout(20, TimeUnit.SECONDS);
                clientBuilder.writeTimeout(20, TimeUnit.SECONDS);

                OkHttpClient client = clientBuilder.build();

                // Create POST request
                Request request = new Request.Builder()
                        .url("https://wzugdkxj15.execute-api.eu-central-1.amazonaws.com/Standard/CO2")
                        .post(requestBody)
                        .build();

                // Execute request
                Response response = client.newCall(request).execute();

                // Get response code
                int responseCode = response.code();

                // Handle response
                if (response.isSuccessful()) {
                    successState = "success";
                }
                else
                {
                    successState = "failure";
                }

                response.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(successState=="success")
            {
                mainActivity.OnTransmissionSuccess();
            }
            else if(successState=="failure")
            {
                mainActivity.OnTransmissionFail("Transmission failed, try again!");
            }

            else //no response?
            {
                mainActivity.OnTransmissionFail("No Response from server, try again!");
            }
        }
    }
}
