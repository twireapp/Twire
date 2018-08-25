package com.sebastianrask.bettersubscription;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import io.fabric.sdk.android.Fabric;


/**
 * Created by SebastianRask on 20-02-2016.
 */
@SuppressLint("StaticFieldLeak") // It is alright to store application context statically
public class PocketPlaysApplication extends MultiDexApplication {
    private static Tracker mTracker;
    private static Context mContext;

    public static boolean isCrawlerUpdate = false; //ToDo remember to disable for crawler updates

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this.getApplicationContext();

        initNotificationChannels();

        if (!BuildConfig.DEBUG) {
            try {
                Fabric.with(this, new Crashlytics());

                final Fabric fabric = new Fabric.Builder(this)
                        .kits(new Crashlytics())
                        .debuggable(true)
                        .build();
                Fabric.with(fabric);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     *
     * @return tracker
     */
    static synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(mContext);
            mTracker = analytics.newTracker(R.xml.global_tracker);
            mTracker.enableAdvertisingIdCollection(true);
            mTracker.enableExceptionReporting(true);
        }

        return mTracker;
    }

    public static void trackEvent(@StringRes int category, @StringRes int action, @Nullable String label) {
        PocketPlaysApplication.trackEvent(mContext.getString(category), mContext.getString(action), label, null);
    }

    public static void trackEvent(String category, String action, @Nullable String label, @Nullable Long value) {
        HitBuilders.EventBuilder builder = new HitBuilders.EventBuilder().setCategory(category).setAction(action);

        if (label != null) {
            builder.setLabel(label);
        }

        if (value != null) {
            builder.setValue(value);
        }

        Tracker tracker = getDefaultTracker();
        if (tracker != null && tracker.isInitialized() && !isCrawlerUpdate) {
            tracker.send(builder.build());
        }
    }

    private void initNotificationChannels() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || notificationManager == null) {
            return;
        }

        notificationManager.createNotificationChannel(
                new NotificationChannel(getString(R.string.live_streamer_notification_id), "New Streamer is live", NotificationManager.IMPORTANCE_DEFAULT)
        );

        notificationManager.createNotificationChannel(
                new NotificationChannel(getString(R.string.stream_cast_notification_id), "Stream Playback Control", NotificationManager.IMPORTANCE_DEFAULT)
        );
    }
}
