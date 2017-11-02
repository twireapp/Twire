package com.sebastianrask.bettersubscription.broadcasts_and_services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

/**
 * Created by idealMJ on 07/08/16.
 */
public class RemoteControlReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
			final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

			if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
				switch (event.getKeyCode()) {
					case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
						context.startService(new Intent(context, PlayerService.class));
						break;
				}
			}
		}
	}
}
