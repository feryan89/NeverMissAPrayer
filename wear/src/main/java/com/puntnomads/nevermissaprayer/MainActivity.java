package com.puntnomads.nevermissaprayer;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private TextView mTextView;
    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_BODY_SENSORS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
        // Keep the Wear screen always on (for testing only!)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.text);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.BODY_SENSORS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BODY_SENSORS},
                        MY_PERMISSIONS_REQUEST_BODY_SENSORS);
            }
        }

        scheduleNotification(getNotification("Asr Prayer","Go to the mosque"), System.currentTimeMillis()+10000);

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getBundleExtra("datamap");
            long numbers = data.getLong("numbers");
            /*
            for(int x = 0; x < numbers; x++){
                titles.add(data.getString("title"+x));
                descriptions.add(data.getString("description"+x));
                beginTimes.add(data.getLong("begin"+x));
                endTimes.add(data.getLong("end"+x));
            }
            if(titles.size()==numbers){
                displayInfo();
                sendNotifications();
            }
            */
        }
    }

    private void scheduleNotification(Notification notification, Long beginTime) {

        Intent notificationIntent = new Intent(this, NotificationReceiver.class);
        notificationIntent.putExtra(NotificationReceiver.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(NotificationReceiver.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Date date = new Date(beginTime);
        String dateString = null;
        SimpleDateFormat sdfr = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        dateString = sdfr.format(date);
        Log.v("time", dateString);
        long futureInMillis = beginTime;
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, futureInMillis, pendingIntent);
    }

    public Notification getNotification(String title, String description) {
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(description)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                .build();
        return notification;
    }

    /*
    public void sendNotifications(){
        for(int i = 0; i < titles.size(); i++){
            scheduleNotification(getNotification(titles.get(i),descriptions.get(i), beginTimes.get(i), endTimes.get(i)),
                    beginTimes.get(i), 1000);
        }
    }

    public void displayInfo(){
        String display = "";
        display = "Received from the data Layer\n";
        for(int x = 0; x < titles.size(); x++){
            display += titles.get(x) + ": ";
            display += descriptions.get(x) + "\n";
        }
        mTextView.setText(display);
    }
    */

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_BODY_SENSORS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                }
                return;
            }
        }
    }
}
