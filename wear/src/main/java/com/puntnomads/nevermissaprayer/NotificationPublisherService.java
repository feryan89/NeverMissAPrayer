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
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

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
    private static int initialDistance;
    private static boolean firstValue;
    Vibrator v;
    NotificationManager notificationManager;
    Notification notification;
    int id;
    CountDownTimer timer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoNotDimScreen");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null) {
            firstValue = true;
            distance = 0;
            initialDistance = 0;
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(this, "Count sensor not available!", Toast.LENGTH_LONG).show();
        }

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = intent.getParcelableExtra(NOTIFICATION);
        id = intent.getIntExtra(NOTIFICATION_ID, 0);
        notificationManager.notify(id, notification);

        wakelock.acquire(601000);
        timer = new CountDownTimer(600000, 1000) {
            public void onTick(long millisUntilFinished) {
                if(distance > 49) {
                    notificationManager.notify(id, notification);
                    cancelCountDownTimer();
                }

                Log.v("Millis: ", Long.toString(millisUntilFinished));
                // http://stackoverflow.com/questions/17620641/countdowntimer-in-minutes-and-seconds
                // http://stackoverflow.com/questions/6810416/android-countdowntimer-shows-1-for-two-seconds/6811744#6811744
                if(Math.round(millisUntilFinished/1000) < 301){
                    if(Math.round(millisUntilFinished/1000) % 2 == 0){
                        Log.v("Time: ", Long.toString(Math.round(millisUntilFinished/1000)));
                        v.vibrate(1000);
                        // countdown timer in watch is unreliable when not in charger (stackoverflow)
                    }
                }
            }
            public void onFinish() {
                Log.v("Finish: ", "all");
            }
        }.start();

        Log.v("Finish: ", "timer");
        NotificationReceiver.completeWakefulIntent(intent);
        stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(firstValue){
            initialDistance = (int)event.values[0];
            firstValue = false;
        }
        distance = (int)event.values[0] - initialDistance;
        String msg = "Count: " + distance;
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
