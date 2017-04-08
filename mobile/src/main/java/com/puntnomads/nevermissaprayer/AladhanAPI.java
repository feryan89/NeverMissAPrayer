package com.puntnomads.nevermissaprayer;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Url;

/**
 * Created by puntnomads on 02/02/2017.
 */

public interface AladhanAPI {
    // String timestamp = Long.toString(System.currentTimeMillis()/1000);
    // http://api.aladhan.com/timingsByCity/1486192485?city=Dubai&country=AE&method=4
    @GET
    Call<MainPojo> loadPrayerTimes(@Url String url);
}
