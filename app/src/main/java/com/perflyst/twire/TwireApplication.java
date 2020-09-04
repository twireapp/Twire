package com.perflyst.twire;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.perflyst.twire.utils.TLSSocketFactoryCompat;

import org.conscrypt.Conscrypt;

import java.security.Security;

/**
 * Created by SebastianRask on 20-02-2016.
 */
@SuppressLint("StaticFieldLeak") // It is alright to store application context statically
public class TwireApplication extends MultiDexApplication {
    public static final boolean isCrawlerUpdate = false; //ToDo remember to disable for crawler updates

    @Override
    public void onCreate() {
        super.onCreate();

        // Twitch API requires TLS 1.2, which may be unavailable/not enabled on Android 4.1 - 4.4.
        // Install modern TLS protocols using a security provider, and enable them by default in a
        // custom SSLSocketFactory.
        Security.insertProviderAt(Conscrypt.newProvider(), 1);
        TLSSocketFactoryCompat.setAsDefault();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
