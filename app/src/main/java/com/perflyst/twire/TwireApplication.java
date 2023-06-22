package com.perflyst.twire;

import android.app.Application;

import com.techyourchance.threadposter.BackgroundThreadPoster;
import com.techyourchance.threadposter.UiThreadPoster;

/**
 * Created by SebastianRask on 20-02-2016.
 */
public class TwireApplication extends Application {
    public static final UiThreadPoster uiThreadPoster = new UiThreadPoster();
    public static final BackgroundThreadPoster backgroundPoster = new BackgroundThreadPoster();
}
