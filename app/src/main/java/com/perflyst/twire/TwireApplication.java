package com.perflyst.twire;

import android.app.Application;

import com.techyourchance.threadposter.BackgroundThreadPoster;
import com.techyourchance.threadposter.UiThreadPoster;

import java.util.HashMap;

import io.sentry.android.core.SentryAndroid;

/**
 * Created by SebastianRask on 20-02-2016.
 */
public class TwireApplication extends Application {
    public static final UiThreadPoster uiThreadPoster = new UiThreadPoster();
    public static final BackgroundThreadPoster backgroundPoster = new BackgroundThreadPoster();

    @Override
    public void onCreate() {
        super.onCreate();

        SentryAndroid.init(this, options -> {
            options.setBeforeSend((event, hint) -> {
                event.setUser(null);
                event.getContexts().remove("device");
                event.getContexts().put("device", new HashMap<String, String>() {
                    {
                        put("model", android.os.Build.MODEL);
                    }
                });
                event.setEnvironment(BuildConfig.BUILD_TYPE);
                return event;
            });
        });
    }
}
