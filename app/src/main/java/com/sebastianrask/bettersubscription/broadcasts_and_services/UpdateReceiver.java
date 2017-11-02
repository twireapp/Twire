package com.sebastianrask.bettersubscription.broadcasts_and_services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sebastianrask.bettersubscription.service.Settings;

/**
 * Created by Sebastian Rask Jepsen (SRJ@Idealdev.dk) on 05/03/17.
 */

public class UpdateReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED))
			new Settings(context).setIsUpdated(true);
	}
}
