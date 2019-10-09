package com.perflyst.twire;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

/**
 * Created by SebastianRask on 20-02-2016.
 */
@SuppressLint("StaticFieldLeak") // It is alright to store application context statically
public class TwireApplication extends MultiDexApplication {
    public static boolean isCrawlerUpdate = false; //ToDo remember to disable for crawler updates
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this.getApplicationContext();

        initNotificationChannels();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void initNotificationChannels() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || notificationManager == null) {
            return;
        }

        notificationManager.createNotificationChannel(
                new NotificationChannel(getString(R.string.live_streamer_notification_id), "New Streamer is live", NotificationManager.IMPORTANCE_LOW)
        );

        notificationManager.createNotificationChannel(
                new NotificationChannel(getString(R.string.stream_cast_notification_id), "Stream Playback Control", NotificationManager.IMPORTANCE_DEFAULT)
        );
    }
}
