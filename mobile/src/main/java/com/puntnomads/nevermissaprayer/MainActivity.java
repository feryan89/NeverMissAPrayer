package com.puntnomads.nevermissaprayer;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import com.puntnomads.nevermissaprayer.parser.JSONParser;
import com.puntnomads.nevermissaprayer.utils.Keys;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Button syncButton;
    private Button sendButton;
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<Long> startTimes = new ArrayList<Long>();
    GoogleApiClient googleClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        syncButton = (Button) findViewById(R.id.syncButton);
        sendButton = (Button) findViewById(R.id.sendButton);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDataFromAPI();
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getEvents();
            }
        });
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


    }

    private void getDataFromAPI(){
        new GetDataTask().execute();
    }

    private long convertStringToLong(String day, String time, String prayer){
        String s = day + " " + time;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMM yyyy HH:mm");
        Date date;
        long epoch = 1486104852532L;
        try
        {
            date = simpleDateFormat.parse(s);
            epoch = date.getTime();
        }
        catch (ParseException ex)
        {
            Log.v("Exception ",ex.toString());
        }
        return epoch;
    };

    public void getEvents() {
        googleClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        String PRAYER_DATA_PATH = "/prayer_data";

        // Create a DataMap object and send it to the data layer
        DataMap dataMap = new DataMap();
        dataMap.putLong("numbers", titles.size());
        for(int x =0; x < titles.size(); x++){
            dataMap.putString("title"+x, titles.get(x));
            dataMap.putLong("starttimes"+x, startTimes.get(x));
        }
        dataMap.putLong("time", System.currentTimeMillis());
        titles.clear();
        startTimes.clear();

        //Requires a new thread to avoid blocking the UI
        new SendToDataLayerThread(PRAYER_DATA_PATH, dataMap).start();
        Toast.makeText(getApplicationContext(), "Sended Data", Toast.LENGTH_SHORT).show();
    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onStop();
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    class SendToDataLayerThread extends Thread {
        String path;
        DataMap dataMap;

        // Constructor for sending data objects to the data layer
        SendToDataLayerThread(String p, DataMap data) {
            path = p;
            dataMap = data;
        }

        public void run() {
            // Construct a DataRequest and send over the data layer
            PutDataMapRequest putDMR = PutDataMapRequest.create(path);
            putDMR.getDataMap().putAll(dataMap);
            PutDataRequest request = putDMR.asPutDataRequest().setUrgent();
            DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleClient, request).await();
            if (result.getStatus().isSuccess()) {
                Log.v("myTag", "DataMap: " + dataMap + " sent successfully to data layer ");
            } else {
                // Log an error
                Log.v("myTag", "ERROR: failed to send DataMap to data layer");
            }
        }
    }

    /**
     * Creating Get Data Task for Getting Data From Web
     */
    class GetDataTask extends AsyncTask<Void, Void, Void> {

        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            /**
             * Progress Dialog for User Interaction
             */
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setTitle("Hey Wait Please...");
            dialog.setMessage("I am getting your JSON");
            dialog.show();
        }

        @Nullable
        @Override
        protected Void doInBackground(Void... params) {

            /**
             * Getting JSON Object from Web Using okHttp
             */
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = jsonParser.getDataFromWeb();

            try {
                /**
                 * Check Whether Its NULL???
                 */
                if (jsonObject != null) {
                    /**
                     * Check Length...
                     */
                    if(jsonObject.length() > 0) {
                        /**
                         * Getting Array named "contacts" From MAIN Json Object
                         */
                        JSONArray array = jsonObject.getJSONArray(Keys.KEY_DATA);

                        /**
                         * Check Length of Array...
                         */
                        int lenArray = array.length();
                        if(lenArray > 0) {
                            for(int jIndex = 0; jIndex < lenArray; jIndex++) {

                                /**
                                 * Getting Inner Object from contacts array...
                                 * and
                                 * From that We will get Name of that Contact
                                 *
                                 */
                                JSONObject innerObject = array.getJSONObject(jIndex);
                                JSONObject dateObject = innerObject.getJSONObject(Keys.KEY_DATE);
                                String today = dateObject.getString(Keys.KEY_READABLE);
                                JSONObject timingsObject = innerObject.getJSONObject(Keys.KEY_TIMINGS);
                                String fajrTime = timingsObject.getString(Keys.KEY_FAJR);
                                String dhuhrTime = timingsObject.getString(Keys.KEY_DHUHR);
                                String asrTime = timingsObject.getString(Keys.KEY_ASR);
                                String maghribTime = timingsObject.getString(Keys.KEY_MAGHRIB);
                                String ishaTime = timingsObject.getString(Keys.KEY_ISHA);

                                titles.add("Fajr Prayer");
                                startTimes.add(convertStringToLong(today,fajrTime,"Fajr"));

                                titles.add("Dhuhr Prayer");
                                startTimes.add(convertStringToLong(today,dhuhrTime,"Dhuhr"));

                                titles.add("Asr Prayer");
                                startTimes.add(convertStringToLong(today,asrTime,"Asr"));

                                titles.add("Maghrib Prayer");
                                startTimes.add(convertStringToLong(today,maghribTime,"Maghrib"));

                                titles.add("Isha Prayer");
                                startTimes.add(convertStringToLong(today,ishaTime,"Isha"));

                            }
                        }
                    }
                } else {

                }
            } catch (JSONException je) {
                Log.i(JSONParser.TAG, "" + je.getLocalizedMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            dialog.dismiss();
            Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
        }
    }
}


