package com.puntnomads.nevermissaprayer.parser;

import android.support.annotation.NonNull;
import android.util.Log;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;

public class JSONParser {

    /********
     * URLS
     *******/
    // http://api.aladhan.com/v1/calendarByCity?city=London&country=United%20Kingdom&method=4&month=02&year=2018
    String month = Integer.toString(Calendar.getInstance().get(Calendar.MONTH)+1);
    String year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
    private String MAIN_URL = "http://api.aladhan.com/v1/calendarByCity?city=London&country=GB&method=4&month="+month+"&year="+year;

    /**
     * TAGs Defined Here...
     */
    public static final String TAG = "TAG";

    /**
     * Response
     */
    private static Response response;

    /**
     * Get Table Booking Charge
     *
     * @return JSON Object
     */
    public JSONObject getDataFromWeb() {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(MAIN_URL)
                    .build();
            response = client.newCall(request).execute();
            return new JSONObject(response.body().string());
        } catch (@NonNull IOException | JSONException e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        return null;
    }
}
