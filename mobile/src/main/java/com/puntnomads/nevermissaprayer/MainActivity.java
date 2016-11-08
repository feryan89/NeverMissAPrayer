package com.puntnomads.nevermissaprayer;

import android.*;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    // Projection array. Creating indices for this array instead of doing
// dynamic lookups improves performance.
    public static final String[] EVENT_PROJECTION = new String[] {
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
            CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
    };

    private static final String[] INSTANCE_PROJECTION = {
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
    };

    // Request code for READ_CONTACTS. It can be any number > 0.
    private static final int PERMISSIONS_REQUEST_READ_CALENDAR = 1;

    private EditText titlesEditText;
    private EditText descriptionsEditText;
    private TimePicker timePicker;
    private DatePicker datePicker;
    private Button saveButton;
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> descriptions = new ArrayList<String>();
    ArrayList<Long> startTimes = new ArrayList<Long>();

    GoogleApiClient googleClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        titlesEditText = (EditText) findViewById(R.id.titles_editText);
        descriptionsEditText = (EditText) findViewById(R.id.description_editText);
        timePicker = (TimePicker) findViewById(R.id.timePicker);
        datePicker = (DatePicker) findViewById(R.id.datePicker);
        saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                titles.add(titlesEditText.getText().toString());
                Log.v("title:", titlesEditText.getText().toString());
                descriptions.add(descriptionsEditText.getText().toString());
                Log.v("description:", descriptionsEditText.getText().toString());
                Calendar calendar = Calendar.getInstance();
                calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                        timePicker.getHour(), timePicker.getMinute(), 0);
                startTimes.add(calendar.getTimeInMillis());
                Log.v("Start Time:", String.valueOf(calendar.getTimeInMillis()));
                //Log.v("EditText", mEdit.getText().toString());
            }
        });
        // Build a new GoogleApiClient for the the Wearable API
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        askPermissions();
    }

    public void askPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.READ_CALENDAR}, PERMISSIONS_REQUEST_READ_CALENDAR);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            getEvents();
        }
    }

    public void getEvents() {

        //titles.add(title);
        //descriptions.add(description);
        //beginTimes.add(begin);

    }

    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
        googleClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        String WEARABLE_DATA_PATH = "/wearable_data";

        // Create a DataMap object and send it to the data layer
        DataMap dataMap = new DataMap();
        dataMap.putLong("numbers", titles.size());
        for(int x =0; x < titles.size(); x++){
            dataMap.putString("title"+x, titles.get(x));
            dataMap.putString("description"+x, descriptions.get(x));
            dataMap.putLong("begin"+x, startTimes.get(x));
        }
        dataMap.putLong("time", System.currentTimeMillis());

        //Requires a new thread to avoid blocking the UI
        new SendToDataLayerThread(WEARABLE_DATA_PATH, dataMap).start();
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
