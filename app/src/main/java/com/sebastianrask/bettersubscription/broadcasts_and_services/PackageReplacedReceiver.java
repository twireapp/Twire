package com.sebastianrask.bettersubscription.broadcasts_and_services;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sebastianrask.bettersubscription.service.Service;

/**
 * Created by idealMJ on 11/08/16.
 */
public class PackageReplacedReceiver extends BroadcastReceiver {
	private final String LOG_TAG = getClass().getSimpleName();
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LOG_TAG, "Package replaced");
	}
}
