package com.puntnomads.nevermissaprayer;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by puntnomads on 28/10/2016.
 */


// Send a notification when the prayer time is reached. Then start timer for 5 minutes and start counting steps.
// if 50 steps are measured then stop the timer. if the timer finishes, then start vibrating the watch every 10 seconds.
// if 50 steps has been measured the vibrations get cancelled.

public class NotificationPublisherService extends Service implements SensorEventListener {

    public static String NOTIFICATION_ID = "notification-id";
    public static String NOTIFICATION = "notification";
    private SensorManager sensorManager;
    private static int distance;
    NotificationManager notificationManager;
    Notification notification;
    int id;
    CountDownTimer timer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(this, "Count sensor not available!", Toast.LENGTH_LONG).show();
        }

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = intent.getParcelableExtra(NOTIFICATION);
        id = intent.getIntExtra(NOTIFICATION_ID, 0);
        //notificationManager.notify(id, notification);

        timer = new CountDownTimer(300000, 1000) {
            public void onTick(long millisUntilFinished) {
                Log.i("Time-", "seconds remaining: " + millisUntilFinished / 1000);
                if(distance>9){
                notificationManager.notify(id, notification);
                cancelCountDownTimer();
                }
            }
            public void onFinish() {
                //startVibrating();
            }
        }.start();

        NotificationReceiver.completeWakefulIntent(intent);
        // If we get killed, after returning from here, restart
        return START_NOT_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        distance = (int)event.values[0];
        String msg = "Count: " + (int)event.values[0];
        Log.i("", msg);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public void cancelCountDownTimer(){
        timer.cancel();
    }
}
