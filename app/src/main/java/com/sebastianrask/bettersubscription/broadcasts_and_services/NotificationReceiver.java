package com.sebastianrask.bettersubscription.broadcasts_and_services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.service.notification.StatusBarNotification;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Joiner;
import com.sebastianrask.bettersubscription.BuildConfig;
import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.activities.main.MyStreamsActivity;
import com.sebastianrask.bettersubscription.activities.stream.LiveStreamActivity;
import com.sebastianrask.bettersubscription.model.ChannelInfo;
import com.sebastianrask.bettersubscription.model.StreamInfo;
import com.sebastianrask.bettersubscription.service.Service;
import com.sebastianrask.bettersubscription.service.Settings;
import com.sebastianrask.bettersubscription.service.StreamsService;
import com.sebastianrask.bettersubscription.service.SubscriptionsDbHelper;

import net.nrask.srjneeds.util.ImageUtil;
import net.nrask.srjneeds.util.SRJUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationReceiver extends BroadcastReceiver {
    private String LOG_TAG = getClass().getSimpleName();
    private final String GROUP_KEY = "LiveStream";
    private final int NOTIFICATION_ID = 8760;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG) {
            Toast.makeText(context, "NOTIFICATION ON RECEIVE", Toast.LENGTH_LONG).show();
        }
        Settings settings = new Settings(context);

        // Only run if notifications are enabled.
        if(settings.isNotificationsDisabled()) {
            return;
        }

        updateNotificationChannelIfNeeded(context);

        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            Log.d(LOG_TAG, "Device booted - Scheduling new Alarm!");
            Service.startNotifications(context);
        } else {
            FetchNotificationDataTask fetchNotificationDataTask = new FetchNotificationDataTask(context);
            fetchNotificationDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void updateNotificationChannelIfNeeded(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || notificationManager == null) {
            return;
        }

        NotificationChannel channel = notificationManager.getNotificationChannel(context.getString(R.string.live_streamer_notification_id));
        if (new Settings(context).getNotificationsSound()) {

            channel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
            );
        } else {
            channel.setSound(null, null);
        }
    }

    private boolean isInQuietTime(Settings settings) {
        boolean isInQuietHours = false;

        int currentHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int currentMinuteOfHour = Calendar.getInstance().get(Calendar.MINUTE);

        int quietHourStartHour = settings.getNotificationsQuietStartHour();
        int quietHourStartMinute = settings.getNotificationsQuietStartMinute();

        int quietHourEndHour = settings.getNotificationsQuietEndHour();
        int quietHourEndMinute = settings.getNotificationsQuietEndMinute();

        // The Quiet time ends on the day after the start
        if(quietHourEndHour < quietHourStartHour || (quietHourEndHour == quietHourStartHour && quietHourEndMinute < quietHourStartMinute) ) {
            if(currentHourOfDay > quietHourStartHour || currentHourOfDay < quietHourEndHour) {
                isInQuietHours = true;
            } else if(currentHourOfDay == quietHourStartHour && currentMinuteOfHour >= quietHourStartMinute) {
                isInQuietHours = true;
            } else if(currentHourOfDay == quietHourEndHour && currentMinuteOfHour <= quietHourEndMinute) {
                isInQuietHours = true;
            }
        } else { // The quiet time starts and ends on the same day
            if(currentHourOfDay > quietHourStartHour && currentHourOfDay < quietHourEndHour) {
                isInQuietHours = true;
            } else if(currentHourOfDay == quietHourStartHour && currentHourOfDay < quietHourEndHour && currentMinuteOfHour >= quietHourStartMinute) {
                isInQuietHours = true;
            } else if(currentHourOfDay == quietHourEndHour && currentHourOfDay > quietHourStartHour && currentMinuteOfHour <= quietHourEndMinute) {
                isInQuietHours = true;
            } else if(quietHourEndHour == quietHourStartHour && currentMinuteOfHour >= quietHourStartMinute && currentMinuteOfHour <= quietHourEndMinute) {
                isInQuietHours = true;
            }
        }

        // In the special case where the user has set the start and end time to the exact same time
        if(quietHourEndHour == quietHourStartHour && quietHourStartMinute == quietHourEndMinute)
            isInQuietHours = false;

        return isInQuietHours;
    }

    private void makeBundledNotifications(NotificationFetchData notificationData, Context context) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager == null) { return; }

        // Remove all notifications for streams that are no longer live.
        for (StreamInfo stream : notificationData.getNewOfflineStreams()) {
            mNotificationManager.cancel(stream.getChannelInfo().getNotificationTag(), NOTIFICATION_ID);
        }

        // Update all current notification. E.g. current game and viewer count
        updateCurrentNotifications(notificationData, mNotificationManager, context);

        // If no new streams are live. Don't bother.
        List<StreamWithImage> newLiveStreams = notificationData.getNewOnlineStreams();
        if (newLiveStreams.isEmpty()) {
            return;
        }

        // Make a new bundled notification for every new stream. Get one of the stream icons to show for the summary notification
        Bitmap largeIconSummaryNotification = createBundledStreamNotificationsAndGetLargeIcon(
                newLiveStreams,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M, // Only show viewer count if we can update the notification,
                mNotificationManager,
                context
        );

        createStreamSummaryNotification(largeIconSummaryNotification, notificationData, mNotificationManager, context);
    }

    private void createStreamSummaryNotification(Bitmap largeIcon, NotificationFetchData notificationData, NotificationManager manager, Context context) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MyStreamsActivity.class);
        stackBuilder.addNextIntent(new Intent(context, MyStreamsActivity.class));
        PendingIntent clickIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationTextData notificationText = constructMainNotificationText(notificationData, context);
        if (notificationText.getTitle().isEmpty() || notificationText.getContent().isEmpty()) {
            return;
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, context.getString(R.string.live_streamer_notification_id))
                .setAutoCancel(true)
                .setContentTitle(notificationText.getTitle())
                .setContentText(notificationText.getContent())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText.getContent()))
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setContentIntent(clickIntent)
                .setSubText(notificationText.getSubtext())
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.ic_notification_icon_refresh)
                .setColor(ContextCompat.getColor(context, R.color.primary));

        Settings settings = new Settings(context);
        boolean toVibrate = settings.getNotificationsVibrations();
        boolean toWakeScreen = settings.getNotificationsScreenWake();
        boolean toPlaySound = settings.getNotificationsSound();
        boolean toBlinkLED = settings.getNotificationsLED();
        boolean isQuietHoursEnabled = settings.getNotificationsQuietHours();
        boolean isInQuietHours = isQuietHoursEnabled && isInQuietTime(settings);

        if(toWakeScreen && !isInQuietHours) {
            PowerManager.WakeLock screenOn = null;
            PowerManager powerManager = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
            if (powerManager != null) {
                screenOn = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "pocketplays:lock");
            }
            if (screenOn != null) {
                screenOn.acquire(2000);
            }
        }

        if(toVibrate && !isInQuietHours) {
            // Set the notification to vibrate
            final int DELAY = 0;
            final int VIBRATE_DURATION = 100;
            mBuilder.setVibrate(new long[] {
                    DELAY,
                    VIBRATE_DURATION,
                    0,
                    0
            });
        }
        if(toPlaySound && !isInQuietHours)
            mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        if(toBlinkLED && !isInQuietHours)
            mBuilder.setLights(Color.BLUE, 5000, 5000);

        manager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private Bitmap createBundledStreamNotificationsAndGetLargeIcon(List<StreamWithImage> liveStreams, boolean showViewers, NotificationManager notificationManager, Context context) {
        Bitmap largeIcon = null;
        ArrayList<Integer> channelIdsToIgnore = new SubscriptionsDbHelper(context).getUsersNotToNotify();
        for (StreamWithImage stream : liveStreams) {
            ChannelInfo channel = stream.getStream().getChannelInfo();

            // Skip if user has specifically chosen not to get notifications for this stream.
            if (channelIdsToIgnore.contains(channel.getUserId())) {
                continue;
            }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                notificationManager.notify(
                        channel.getNotificationTag(),
                        NOTIFICATION_ID,
                        createStreamNotification (
                                stream.getStream(),
                                stream.getImage(),
                                showViewers,
                                context
                        )
                );
            }


            if (largeIcon == null) {
                largeIcon = stream.getImage();
            }
        }

        return largeIcon;
    }

    private void updateCurrentNotifications(NotificationFetchData notificationFetchData, NotificationManager notificationManager, Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        // This try-catch is needed because of an Android 6.0 issue where calling getActiveNotifications may throw a nullpointerexception
        // https://github.com/googlesamples/android-ActiveNotifications/issues/1
        try {
            if (notificationManager == null || notificationManager.getActiveNotifications() == null) {
                return;
            }
        } catch (NullPointerException e) {
            return;
        }


        // Nested for loops :/
        for (StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
            for (StreamInfo stream : notificationFetchData.getCurrentlyOnlineStreams()) {
                if (stream.getChannelInfo().getNotificationTag().equals(statusBarNotification.getTag())) {
                    Notification notification = createStreamNotification(
                            stream,
                            getLargeIconFromNotification(statusBarNotification.getNotification(), context),
                            true,
                            context
                    );

                    notificationManager.notify(
                            stream.getChannelInfo().getNotificationTag(),
                            NOTIFICATION_ID,
                            notification
                    );
                }
            }
        }
    }

    private Notification createStreamNotification(StreamInfo stream, @Nullable Bitmap image, boolean showViewers, Context context) {
        Intent resultIntent = LiveStreamActivity.createLiveStreamIntent(stream, false, context);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MyStreamsActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent clickIntent = stackBuilder.getPendingIntent(
                stream.getChannelInfo().getDisplayName().hashCode(),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationTextData textData = constructStreamNotificationText(stream, context);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, context.getString(R.string.live_streamer_notification_id))
                .setAutoCancel(false)
                .setContentTitle(textData.getTitle())
                .setContentText(textData.getContent())
                .setGroup(GROUP_KEY)
                .setWhen(stream.getStartedAt())
                .setShowWhen(true)
                .setSmallIcon(R.drawable.ic_notification_icon_refresh)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setContentIntent(clickIntent)
                .setSound(null);

        if (image != null) {
            mBuilder.setLargeIcon(image);
        }

        if (showViewers) {
            mBuilder.setSubText(textData.getSubtext());
        }

        return mBuilder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private Bitmap getLargeIconFromNotification(Notification notification, Context context) {
        if (notification == null || notification.getLargeIcon() == null) {
            return null;
        }

        Drawable iconDrawable = notification.getLargeIcon().loadDrawable(context);
        if (iconDrawable == null) {
            return null;
        }

        return ImageUtil.drawableToBitmap(iconDrawable);
    }

    private NotificationTextData constructStreamNotificationText(StreamInfo stream, Context context) {
        ChannelInfo channel = stream.getChannelInfo();
        return new NotificationTextData(
                channel.getDisplayName(),
                context.getString(R.string.notification_channel_is_playing_game_short, stream.getGame()),
                context.getString(R.string.notification_stream_viewers, stream.getCurrentViewers())
        );
    }

    private NotificationTextData constructMainNotificationText(NotificationFetchData fetchData, Context context) {
        List<StreamWithImage> newLiveStreams = new ArrayList<>();

        ArrayList<Integer> channelIdsToIgnore = new SubscriptionsDbHelper(context).getUsersNotToNotify();
        for (StreamWithImage streamWithImage : fetchData.getNewOnlineStreams()) {
            if (channelIdsToIgnore.contains(streamWithImage.getStream().getChannelInfo().getUserId())) {
                continue;
            }

            newLiveStreams.add(streamWithImage);
        }

        String formattedDisplayNames = constructFormattedDisplayNames(new ArrayList<>(newLiveStreams), context);
        String streamsJustWentLive = constructNumberStreamsLive(newLiveStreams.size(), context);

        boolean multipleNewStreams = newLiveStreams.size() > 1;
        String title = multipleNewStreams ? streamsJustWentLive : formattedDisplayNames;
        String content = "";
        if (multipleNewStreams) {
            content = formattedDisplayNames;
        } else if (!newLiveStreams.isEmpty()) {
            StreamInfo singleStream = newLiveStreams.get(0).getStream();
            content = context.getString(R.string.notification_channel_is_playing_game, singleStream.getChannelInfo().getDisplayName(), singleStream.getGame());
        }

        return new NotificationTextData(title, content, "");
    }

    private String constructAdditionalStreamsLive(int streamsLive, Context context) {
        if (streamsLive <= 0) {
            return "";
        }

        @StringRes int stringRessource = streamsLive > 1 ? R.string.notification_additional_multiple_live : R.string.notification_additional_single_live;
        return context.getString(stringRessource, streamsLive);
    }

    private String constructFormattedDisplayNames(List<StreamWithImage> newOnlineStreams, Context context) {
        if (newOnlineStreams.isEmpty()) {
            return "";
        }

        StreamInfo lastStream = newOnlineStreams.remove(newOnlineStreams.size() - 1).getStream();
        String formattedDisplayNames = Joiner.on(context.getString(R.string.notification_channel_list_separator)).join(newOnlineStreams);

        if (formattedDisplayNames.isEmpty()) {
            formattedDisplayNames = context.getString(R.string.notification_channel_single_live, lastStream.getChannelInfo().getDisplayName());
        } else {
            formattedDisplayNames = context.getString(R.string.notification_channel_multiple_live, formattedDisplayNames, lastStream.toString());
        }

        return formattedDisplayNames;
    }

    private String constructNumberStreamsLive(int numberOfLiveStreams, Context context) {
        return context.getString(R.string.notification_number_streams_live, numberOfLiveStreams);
    }

    // TODO: Asynctask should not be inner class as it might create leaks.
    private class FetchNotificationDataTask extends AsyncTask<Void, Void, NotificationFetchData> {
        private Context context;

        public FetchNotificationDataTask(Context context) {
            this.context = context;
        }

        @Override
        protected NotificationFetchData doInBackground(Void... voids) {
            Settings settings = new Settings(context);

            // Only execute the task if the device is connected to a network
            if(!Service.isNetworkConnectedThreadOnly(context)) {
                return null;
            }

            List<StreamInfo> currentlyLive = StreamsService.fetchAllLiveStreams(context, settings.getGeneralTwitchAccessToken());

            // Get Streams that was not live last notification check.
            List<StreamWithImage> newLiveStreams = new ArrayList<>();
            List<StreamInfo> liveChannelsFromLastNotificationCheck = settings.getLastNotificationCheckLiveChannels();
            for (StreamInfo liveStream : currentlyLive) {
                if (!liveChannelsFromLastNotificationCheck.contains(liveStream)) {
                    Bitmap channelLogo = SRJUtil.getBitmapFromURL(liveStream.getChannelInfo().getLowPreview());
                    if (channelLogo != null) {
                        channelLogo = ImageUtil.getRoundedBitmap(channelLogo);
                    }

                    newLiveStreams.add(new StreamWithImage(liveStream, channelLogo));
                }
            }

            // Get Streams that have gone offline since last notification check.
            List<StreamInfo> noLongerLiveStreams = new ArrayList<>();
            for (StreamInfo stream : liveChannelsFromLastNotificationCheck) {
                if (!currentlyLive.contains(stream)) {
                    noLongerLiveStreams.add(stream);
                }
            }

            // Update the persisted list of live channels, that is needed for next notification check
            settings.setLastNotificationCheckLiveChannels(currentlyLive);

            // Sort the streams by amount of followers
            Collections.sort(newLiveStreams, (lhs, rhs) -> lhs.getStream().getChannelInfo().getFollowers() - rhs.getStream().getChannelInfo().getFollowers());

            return new NotificationFetchData(newLiveStreams, currentlyLive, noLongerLiveStreams);
        }

        @Override
        protected void onPostExecute(NotificationFetchData data) {
            super.onPostExecute(data);
            if (data == null) {
                return;
            }
            makeBundledNotifications(data, context);
        }
    }

    private class NotificationFetchData {
        private List<StreamWithImage> mNewOnlineStreams;
        private List<StreamInfo> mCurrentlyOnlineStreams;
        private List<StreamInfo> mNewOfflineStreams;

        public NotificationFetchData(List<StreamWithImage> mNewOnlineStreams, List<StreamInfo> mCurrentlyOnlineStreams, List<StreamInfo> mNewOfflineStreams) {
            this.mNewOnlineStreams = mNewOnlineStreams;
            this.mCurrentlyOnlineStreams = mCurrentlyOnlineStreams;
            this.mNewOfflineStreams = mNewOfflineStreams;
        }

        public List<StreamWithImage> getNewOnlineStreams() {
            return mNewOnlineStreams;
        }

        public List<StreamInfo> getCurrentlyOnlineStreams() {
            return mCurrentlyOnlineStreams;
        }

        public List<StreamInfo> getNewOfflineStreams() {
            return mNewOfflineStreams;
        }
    }

    private class NotificationTextData {
        private String title;
        private String content;
        private String subtext;

        public NotificationTextData(String title, String content, String subtext) {
            this.title = title;
            this.content = content;
            this.subtext = subtext;
        }

        public String getSubtext() {
            return subtext;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }

    private class StreamWithImage {
        private StreamInfo stream;
        private Bitmap image;

        public StreamWithImage(StreamInfo stream, Bitmap image) {
            this.stream = stream;
            this.image = image;
        }

        public StreamInfo getStream() {
            return stream;
        }

        public Bitmap getImage() {
            return image;
        }

        @Override
        public String toString() {
            return stream.toString();
        }
    }
}

