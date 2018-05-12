package com.sebastianrask.bettersubscription.broadcasts_and_services;


import android.app.NotificationChannel;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.ResultReceiver;
import android.support.v4.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.activities.stream.LiveStreamActivity;
import com.sebastianrask.bettersubscription.activities.stream.VODActivity;
import com.sebastianrask.bettersubscription.model.ChannelInfo;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;

public class PlayerService extends Service {
	private final String LOG_TAG = getClass().getSimpleName();
	private final int NOTIFICATION_ID = 1;
	private String url, mVodId;
	private ChannelInfo mChannelInfo;
	private boolean isVod;
	private int vodLength, currentProgress;
	private Intent mNotificationIntent;
	private NotificationCompat.Action lastNotificationAction;
	private ResultReceiver mResultReceiver;

	private static PlayerService instance = null;

	public static final String ACTION_PLAY 		= "action_play";
	public static final String ACTION_PAUSE 	= "action_pause";
	public static final String ACTION_REWIND 	= "action_rewind";
	public static final String ACTION_FORWARD 	= "action_fast_forward";
	public static final String ACTION_NEXT 		= "action_next";
	public static final String ACTION_PREVIOUS 	= "action_previous";
	public static final String ACTION_STOP 		= "action_stop";
	public static final String ACTION_SEEK		= "action_seek";

	public static final int DELEGATE_PLAY		= 1;
	public static final int DELEGATE_PAUSE		= 2;
	public static final int DELEGATE_STOP		= 3;
	public static final int DELEGATE_SEEK_TO	= 4;
	public static final String DELEGATE_SEEK_TO_POSITION = "seekToPosition";

	public static final String URL 				= "url";
	public static final String STREAMER_INFO 	= "streamerInfo";
	public static final String IS_VOD 			= "isVod";
	public static final String VOD_ID			= "vodId";
	public static final String VOD_LENGTH 		= "vodLength";
	public static final String VOD_PROGRESS		= "vodProgress";
	public static final String INTENT 			= "notificationIntent";
	public static final String RECEIVER_DELEGATE = "receiverDelegate";

	public static Intent createPlayServiceIntent(Context context, String url, ChannelInfo channel,
												 boolean isVod, int vodLength, String vodId, int currentProgress,
												 Intent activityIntent, ResultReceiver audioOnlyReceiverDelegate) {
		Intent playerService = new Intent(context, PlayerService.class);
		playerService.putExtra(PlayerService.URL, url);
		playerService.putExtra(PlayerService.STREAMER_INFO, channel);
		playerService.putExtra(PlayerService.IS_VOD, isVod);
		playerService.putExtra(PlayerService.VOD_LENGTH, vodLength);
		playerService.putExtra(PlayerService.VOD_ID, vodId);
		playerService.putExtra(PlayerService.VOD_PROGRESS, currentProgress);
		playerService.putExtra(PlayerService.INTENT, activityIntent);
		playerService.putExtra(PlayerService.RECEIVER_DELEGATE, audioOnlyReceiverDelegate);

		return playerService;
	}

	private MediaSessionCompat mediaSession;
	private MediaPlayer mp;
	private MediaSessionCompat.Callback mCallbacks = new MediaSessionCompat.Callback() {

		@Override
		public void onSeekTo(long pos) {
			super.onSeekTo(pos);

			if (mResultReceiver != null) {
				Bundle bundle = new Bundle();
				bundle.putInt(DELEGATE_SEEK_TO_POSITION, (int) (pos/1000));
				mResultReceiver.send(DELEGATE_SEEK_TO, bundle);
			}
		}

		@Override
		public void onPlayFromUri(Uri uri, Bundle extras) {
			super.onPlayFromUri(uri, extras);
			Log.d(LOG_TAG, "Playing from uri: " + uri.getPath());
		}

		@Override
		public void onPause() {
			super.onPause();
			if (mp == null || mediaSession == null) {
				return;
			}

			mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
												  .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0.0f)
												  .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE).build());

			mp.pause();
			buildNotification( generateAction( R.drawable.ic_play_arrow_black_36dp, getString(R.string.play), ACTION_PLAY ) );

			if (mResultReceiver != null) {
				mResultReceiver.send(DELEGATE_PAUSE, null);
			}
		}

		@Override
		public void onPlay() {
			super.onPlay();
			if (mp == null || mediaSession == null) {
				return;
			}
			if (!isVod) {
				try {
					mp = getNewMediaPlayer();
					mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
						@Override
						public void onPrepared(MediaPlayer mediaPlayer) {
							mp.start();
						}
					});
					mp.prepareAsync();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				mp.start();
			}

			mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
												  .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
												  .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE).build());

			buildNotification( generateAction( R.drawable.ic_pause_black_36dp, getString(R.string.pause), ACTION_PAUSE ) );

			if (mResultReceiver != null) {
				mResultReceiver.send(DELEGATE_PLAY, null);
			}
		}

		@Override
		public void onStop() {
			super.onStop();
			stopSession();

			if (mResultReceiver != null) {
				mResultReceiver.send(DELEGATE_STOP, null);
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOG_TAG, "onCreate");
	}

	private void stopSession() {
		if (mp == null || mediaSession == null) {
			return;
		}

		mp.stop();
		mp.release();
		mediaSession.release();
		mediaSession.setActive(false);
		mp = null;

		NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(NOTIFICATION_ID);
	}

	public static PlayerService getInstance() {
		return instance;
	}

	private void initMediaSession(Intent intent) throws IOException {
		if (intent != null && intent.hasExtra(URL)) {
			String oldUrl = url;
			instance 		= this;
			url 			= intent.getStringExtra(URL);
			mChannelInfo 	= intent.getParcelableExtra(STREAMER_INFO);
			isVod 			= intent.getBooleanExtra(IS_VOD, false);
			vodLength 		= intent.getIntExtra(VOD_LENGTH, -1);
			mVodId 			= intent.getStringExtra(VOD_ID);
			currentProgress = intent.getIntExtra(VOD_PROGRESS, -1);
			mNotificationIntent = intent.getParcelableExtra(INTENT);
			mResultReceiver = intent.getParcelableExtra(RECEIVER_DELEGATE);

			if (mediaSession != null
						&& mediaSession.isActive()
						&& oldUrl != null
						&& !oldUrl.equals(url)
						&& mp != null) {
				//mCallbacks.onStop();
				stopSession();
			}

			mp = getNewMediaPlayer();

			mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mediaPlayer) {
					if (isVod) {
						mp.seekTo(currentProgress * 1000);
					}
					mCallbacks.onPlay();
				}
			});
			mp.prepareAsync();


			ComponentName receiver = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
			mediaSession = new MediaSessionCompat(this, "PlayerService AUDIO", receiver, null);
			mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
										  MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

			mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
												  .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0)
												  .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
												  .build());

			mediaSession.setMetadata(getMediaSessionData(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher)));
			mediaSession.setCallback(mCallbacks);

			AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
				@Override
				public void onAudioFocusChange(int focusChange) {
					// Ignore
				}
			}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
			mediaSession.setActive(true);
			if (mChannelInfo.getLogoURLString() != null) {
				loadStreamerInfoAlbumArt();
			}
		}
	}

	private MediaPlayer getNewMediaPlayer() throws IOException {
		MediaPlayer mediaPlayer = new MediaPlayer();
		mediaPlayer.setDataSource(this, Uri.parse(url));
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setLooping(false);

		return mediaPlayer;
	}

	private void loadStreamerInfoAlbumArt() {
		Target loadArtTarget = new Target() {
			@Override
			public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
				if (mediaSession != null && mediaSession.isActive()) {
					mediaSession.setMetadata(getMediaSessionData(bitmap));
				}
				mChannelInfo.setLogoImage(bitmap);
				if (lastNotificationAction != null) {
					buildNotification(lastNotificationAction);
				}
			}

			@Override
			public void onBitmapFailed(Drawable errorDrawable) {

			}

			@Override
			public void onPrepareLoad(Drawable placeHolderDrawable) {

			}
		};
		Picasso.with(this).load(mChannelInfo.getLogoURLString()).into(loadArtTarget);
	}

	private MediaMetadataCompat getMediaSessionData(Bitmap albumArt) {
		return new MediaMetadataCompat.Builder()
				.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mChannelInfo.getDisplayName())
				.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1)
				.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
				.build();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (mediaSession == null || intent.hasExtra(URL)) {
			try {
				initMediaSession(intent);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			handleIntent(intent);
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mediaSession != null) {
			mediaSession.release();
		}
	}

	private NotificationCompat.Action generateAction( int icon, String title, String intentAction ) {
		Intent intent = new Intent( getApplicationContext(), PlayerService.class );
		intent.setAction( intentAction );
		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
		return new NotificationCompat.Action.Builder( icon, title, pendingIntent ).build();
	}

	private void buildNotification( NotificationCompat.Action action ) {
		lastNotificationAction = action;

		Intent intent = new Intent( getApplicationContext(), PlayerService.class );
		intent.setAction( ACTION_STOP );
		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);

		Class toClass = isVod ? VODActivity.class : LiveStreamActivity.class;
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this).addParentStack(toClass).addNextIntent(mNotificationIntent);
		PendingIntent onClickPendingIntent = stackBuilder.getPendingIntent(
				0,
				PendingIntent.FLAG_UPDATE_CURRENT
		);

		NotificationCompat.Builder noti = (NotificationCompat.Builder) new NotificationCompat.Builder( this )
										.setSmallIcon( R.drawable.ic_notification_icon_refresh )
										.setContentTitle( mChannelInfo.getDisplayName() )
										.setDeleteIntent( pendingIntent )
										.addAction(action)
										.addAction(generateAction(R.drawable.ic_clear_black_36dp, getString(R.string.stop_lower), ACTION_STOP))
										.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
														  .setMediaSession(mediaSession.getSessionToken())
														  .setShowCancelButton(true)
														  .setShowActionsInCompactView(0,1)
										)
										.setShowWhen(false)
										.setAutoCancel(false)
										.setContentIntent(onClickPendingIntent);

		if (mChannelInfo.getLogoImage() != null) {
			noti.setLargeIcon(mChannelInfo.getLogoImage());
		}

		NotificationManager notificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
		if (notificationManager == null) return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			notificationManager.createNotificationChannel(
                    new NotificationChannel("streamCastNotification", "Stream Playback Control", NotificationManager.IMPORTANCE_DEFAULT)
            );
		}
		notificationManager.notify( NOTIFICATION_ID, noti.build() );
	}

	private void handleIntent( Intent intent ) {
		if( intent == null || intent.getAction() == null || mCallbacks == null) {
			return;
		}

		String action = intent.getAction();

		if( action.equalsIgnoreCase( ACTION_PLAY ) ) {
			mCallbacks.onPlay();
		} else if( action.equalsIgnoreCase( ACTION_PAUSE ) ) {
			mCallbacks.onPause();
		} else if( action.equalsIgnoreCase( ACTION_FORWARD) ) {
			mCallbacks.onFastForward();
		} else if( action.equalsIgnoreCase( ACTION_REWIND ) ) {
			mCallbacks.onRewind();
		} else if( action.equalsIgnoreCase( ACTION_PREVIOUS ) ) {
			mCallbacks.onSkipToPrevious();
		} else if( action.equalsIgnoreCase( ACTION_NEXT ) ) {
			mCallbacks.onSkipToNext();
		} else if( action.equalsIgnoreCase( ACTION_STOP ) ) {
			mCallbacks.onStop();
		} else if( action.equalsIgnoreCase( ACTION_SEEK ) && intent.hasExtra( VOD_PROGRESS ) ) {
			if (mp != null && mediaSession.isActive()) {
				mp.seekTo(intent.getIntExtra( VOD_PROGRESS, 0 ));
			}
		}
	}

	public MediaSessionCompat getMediaSession() {
		return mediaSession;
	}

	public MediaPlayer getMediaPlayer() {
		return mp;
	}

	public boolean isPlayingVod() {
		return isVod;
	}

	public String getUrl() {
		return url;
	}

	public String getVodId() {
		return mVodId;
	}

	public ChannelInfo getStreamerInfo() {
		return mChannelInfo;
	}

	public void registerDelegate(ResultReceiver resultReceiver) {
		this.mResultReceiver = resultReceiver;
	}

	public void unregisterDelegate() {
		this.mResultReceiver = null;
	}

}
