package com.puntnomads.nevermissaprayer;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Callback<MainPojo> {

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
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://aladhan.com/prayer-times-api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // prepare call in Retrofit 2.0
        AladhanAPI aladhanAPI = retrofit.create(AladhanAPI.class);

        // http://api.aladhan.com/timingsByCity/1486192485?city=Dubai&country=AE&method=4
        String timestamp = Long.toString((System.currentTimeMillis()/1000)+43200);
        String url = "http://api.aladhan.com"+"/timingsByCity/" + timestamp + "?city=AbuDhabi&country=AE&method=4";
        Call<MainPojo> call = aladhanAPI.loadPrayerTimes(url);
        //asynchronous call
        call.enqueue(this);
    }

    @Override
    public void onResponse(Call<MainPojo> call, Response<MainPojo> response) {
        String day = response.body().getData().getDate().getReadable();
        String time;

        titles.add("Fajr Prayer");
        time = response.body().getData().getTimings().getFajr();
        startTimes.add(convertStringToLong(day,time));

        titles.add("Dhuhr Prayer");
        time = response.body().getData().getTimings().getDhuhr();
        startTimes.add(convertStringToLong(day,time));

        titles.add("Asr Prayer");
        time = response.body().getData().getTimings().getAsr();
        startTimes.add(convertStringToLong(day,time));

        titles.add("Maghrib Prayer");
        time = response.body().getData().getTimings().getMaghrib();
        startTimes.add(convertStringToLong(day,time));

        titles.add("Isha Prayer");
        time = response.body().getData().getTimings().getIsha();
        startTimes.add(convertStringToLong(day,time));
        Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();

    }

    private long convertStringToLong(String day, String time){
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

    @Override
    public void onFailure(Call<MainPojo> call, Throwable t) {
        Toast.makeText(MainActivity.this, t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

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


}
