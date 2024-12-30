package com.perflyst.twire.tasks;

import android.content.Context;

import com.google.common.util.concurrent.ListenableFutureTask;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.TempStorage;
import com.perflyst.twire.utils.Execute;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Connects to the internal database and extracts all the subscription streamer names. creates an
 * object getting all information about the stream and puts it in a list.
 * After it has gotten all the streamer names it rebuilds the cards in the recyclerview
 */
public class GetFollowsFromDB implements Callable<Map<String, ChannelInfo>> {
    private final long timerStart = System.currentTimeMillis();
    private final ListenableFutureTask<ArrayList<ChannelInfo>> twitchUserFollows;
    private final WeakReference<Context> baseContext;

    public GetFollowsFromDB(Context baseContext) {
        this.baseContext = new WeakReference<>(baseContext);
        twitchUserFollows = ListenableFutureTask.create(new GetTwitchUserFollows(baseContext));
    }

    public Map<String, ChannelInfo> call() {
        Timber.d("Entered GetFollowsFromDB");

        Map<String, ChannelInfo> resultList = Service.getStreamerInfoFromDB(baseContext.get());
        Timber.d("%s streamers fetched from database", resultList.size());
        Timber.d(resultList.toString());

        if (!resultList.isEmpty()) {
            // Add the streamers to the static list field to ensure we don't waste time and resources getting the streamers from the database again.
            TempStorage.addLoadedStreamer(resultList.values());
        }

        Execute.background(twitchUserFollows);
        long duration = System.currentTimeMillis() - timerStart;
        Timber.d("Completed task in " + TimeUnit.MILLISECONDS.toSeconds(duration) + " seconds");

        return resultList;
    }

    /**
     * @return The boolean status of the task this AsyncTask starts at the end of onPostExecute
     */
    public boolean isFinished() {
        return twitchUserFollows.isDone();
    }
}
