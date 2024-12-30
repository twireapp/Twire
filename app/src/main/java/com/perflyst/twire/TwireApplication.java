package com.perflyst.twire;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by SebastianRask on 20-02-2016.
 */
public class TwireApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
