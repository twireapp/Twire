package com.perflyst.twire;

import android.annotation.SuppressLint;
import android.app.Application;

import com.techyourchance.threadposter.BackgroundThreadPoster;
import com.techyourchance.threadposter.UiThreadPoster;

/**
 * Created by SebastianRask on 20-02-2016.
 */
@SuppressLint("StaticFieldLeak") // It is alright to store application context statically
public class TwireApplication extends Application {
    public static final boolean isCrawlerUpdate = false; //ToDo remember to disable for crawler updates

    public static final UiThreadPoster uiThreadPoster = new UiThreadPoster();
    public static final BackgroundThreadPoster backgroundPoster = new BackgroundThreadPoster();
}
