package com.sebastianrask.bettersubscription;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.sebastianrask.bettersubscription.activities.stream.LiveStreamActivity;
import com.sebastianrask.bettersubscription.misc.SecretKeys;

import io.fabric.sdk.android.Fabric;


/**
 * Created by SebastianRask on 20-02-2016.
 */
public class PocketPlaysApplication extends Application {
	private Tracker mTracker;
	public static boolean isCrawlerUpdate = false; //ToDo remember to disable for crawler updates

	@Override
	public void onCreate() {
		super.onCreate();
		initCastFunctionality();

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
	 * @return tracker
	 */
	synchronized public Tracker getDefaultTracker() {
		if (mTracker == null) {
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			mTracker = analytics.newTracker(R.xml.global_tracker);
			mTracker.enableAdvertisingIdCollection(true);
		}

		return mTracker;
	}

	private void initCastFunctionality() {
		String applicationID = SecretKeys.CHROME_CAST_APPLICATION_ID;
		CastConfiguration options = new CastConfiguration.Builder(applicationID)
											.enableAutoReconnect()
											.enableDebug()
											.enableWifiReconnection()
											.setCastControllerImmersive(false)
											.setTargetActivity(LiveStreamActivity.class)
											.enableNotification()
											.addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_PLAY_PAUSE, true)
											.addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_DISCONNECT,true)
											.enableLockScreen()
											.build();
		VideoCastManager castManager = VideoCastManager.initialize(getApplicationContext(), options);
	}
}
