package com.perflyst.twire.tasks;

import static com.perflyst.twire.service.Service.getStreamerInfoFromUserId;

import android.content.Context;

import com.github.twitch4j.helix.domain.OutboundFollow;
import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.service.SubscriptionsDbHelper;
import com.perflyst.twire.service.TempStorage;
import com.perflyst.twire.utils.Execute;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Connects to Twitch to retrieve a list of streamers that a specified user follow.
 * <p>
 * Returns an ArrayList of stream names
 */

public class GetTwitchUserFollows implements Callable<ArrayList<ChannelInfo>> {
    private final long timerStart = System.currentTimeMillis();
    private final WeakReference<Context> baseContext;

    public GetTwitchUserFollows(Context baseContext) {
        this.baseContext = new WeakReference<>(baseContext);
    }

    public ArrayList<ChannelInfo> call() {
        ArrayList<String> userSubs = new ArrayList<>();

        String currentCursor = "";

        if (!Settings.isLoggedIn()) {
            return new ArrayList<>();
        }

        String userId = Settings.getGeneralTwitchUserID();

        // Get all the userIds of a users follows
        do {
            var response = TwireApplication.helix.getFollowedChannels(null, userId, null, 100, currentCursor).execute();
            var follows = response.getFollows();
            if (follows != null)
                userSubs.addAll(follows.stream().map(OutboundFollow::getBroadcasterId).toList());

            currentCursor = response.getPagination().getCursor();
        } while (currentCursor != null);
        // ------- Has now loaded all the user's followed streamers ----------

        ArrayList<String> loadedStreamerIds = new ArrayList<>();
        ArrayList<ChannelInfo> streamersToAddToDB = new ArrayList<>();

        try (SubscriptionsDbHelper helper = new SubscriptionsDbHelper(baseContext.get())) {
            for (ChannelInfo si : TempStorage.getLoadedStreamers()) {
                String streamerId = si.getUserId();
                // If the streamer was followed by the user on Twitch but is no longer followed, remove it from the database
                if (!userSubs.contains(streamerId) && Service.isUserTwitch(streamerId, baseContext.get())) {
                    helper.getWritableDatabase().delete(SubscriptionsDbHelper.TABLE_NAME, SubscriptionsDbHelper.COLUMN_ID + " = ?", new String[] {String.valueOf(streamerId)});
                    TempStorage.removeLoadedStreamer(si);
                    continue;
                }

                // Get and save the streamerName of the already loadedStreamers
                loadedStreamerIds.add(streamerId);
            }
        }

        // Find the Twitch userIds that the app hasn't already loaded. Add it to the list of userIds that will be added to the database
        ArrayList<String> IdsToAddToDB = new ArrayList<>();
        ArrayList<StreamerInfoFromIdsThread> streamerInfoThreads = new ArrayList<>();
        for (String id : userSubs) {
            if (!loadedStreamerIds.contains(id)) {
                IdsToAddToDB.add(id);
            }
        }

        // Create the threads with part of the userIds
        final int NAMES_PER_THREAD = 20;
        for (int i = 0; i < IdsToAddToDB.size(); i += NAMES_PER_THREAD) {
            int lastIndex = i + NAMES_PER_THREAD;
            if (lastIndex > IdsToAddToDB.size()) {
                lastIndex = IdsToAddToDB.size();
            }
            StreamerInfoFromIdsThread streamerInfoThread = new StreamerInfoFromIdsThread(new ArrayList<>(IdsToAddToDB.subList(i, lastIndex)));
            streamerInfoThread.start();
            streamerInfoThreads.add(streamerInfoThread);
        }

        // Wait for the threads to finish
        for (Thread thread : streamerInfoThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Timber.e(e);
            }
        }

        // Get the result from the threads and add the StreamerInfo objects to the result list
        for (StreamerInfoFromIdsThread thread : streamerInfoThreads) {
            streamersToAddToDB.addAll(thread.getStreamers());
        }

        // If there are any streamers to add to the DB - Create a task and do so.
        if (!streamersToAddToDB.isEmpty()) {
            Timber.d("Starting task to add " + streamersToAddToDB.size() + " to the db");
            Execute.background(new AddFollowsToDB(baseContext.get(), streamersToAddToDB));
        } else {
            Timber.d("Found no new streamers to add to the database");
        }

        long duration = System.currentTimeMillis() - this.timerStart;
        Timber.d("Completed task in " + TimeUnit.MILLISECONDS.toSeconds(duration) + " seconds");

        return streamersToAddToDB;
    }

    private class StreamerInfoFromIdsThread extends Thread {
        private final ArrayList<String> userIds;
        private final ArrayList<ChannelInfo> streamers = new ArrayList<>();

        StreamerInfoFromIdsThread(ArrayList<String> ids) {
            this.userIds = ids;
        }

        @Override
        public void run() {
            for (String name : userIds) {
                ChannelInfo info = getStreamerInfoFromUserId(name);
                if (info != null) {
                    info.setNotifyWhenLive(true); // Enable by default
                }
                streamers.add(info);
            }
            Timber.d("Thread - Adding " + streamers.size() + " streamers");
        }

        public ArrayList<ChannelInfo> getStreamers() {
            return streamers;
        }
    }
}
