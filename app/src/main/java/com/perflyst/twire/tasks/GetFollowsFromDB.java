package com.perflyst.twire.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.service.TempStorage;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Connects to the internal database and extracts all the subscription streamer names. creates an
 * object getting all information about the stream and puts it in a list.
 * After it has gotten all the streamer names it rebuilds the cards in the recyclerview
 */
public class GetFollowsFromDB extends AsyncTask<Context, Void, Map<String, ChannelInfo>> {
    private final long timerStart = System.currentTimeMillis();
    private final String LOG_TAG = getClass().getSimpleName();
    private final GetTwitchUserFollows twitchUserFollows;
    private final WeakReference<Context> baseContext;

    public GetFollowsFromDB(Context baseContext) {
        this.baseContext = new WeakReference<>(baseContext);
        twitchUserFollows = new GetTwitchUserFollows(baseContext);
    }

    protected Map<String, ChannelInfo> doInBackground(Context... params) {
        Log.d(LOG_TAG, "Entered GetFollowsFromDB");

        Map<String, ChannelInfo> resultList = Service.getStreamerInfoFromDB(baseContext.get());
        Log.d(LOG_TAG, resultList.size() + " streamers fetched from database");
        Log.d(LOG_TAG, resultList.toString());

        return resultList;
    }

    protected void onPostExecute(Map<String, ChannelInfo> subscriptions) {
        if (subscriptions != null && subscriptions.size() > 0) {
            // Add the streamers to the static list field to ensure we don't waste time and resources getting the streamers from the database again.
            TempStorage.addLoadedStreamer(subscriptions.values());
        }

        twitchUserFollows.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Settings(baseContext.get()).getGeneralTwitchName(), this.baseContext.get());
        long duration = System.currentTimeMillis() - timerStart;
        Log.d(LOG_TAG, "Completed task in " + TimeUnit.MILLISECONDS.toSeconds(duration) + " seconds");
    }

    /**
     * @return The boolean status of the task this AsyncTask starts at the end of onPostExecute
     */
    public boolean isFinished() {
        return twitchUserFollows.getStatus() == Status.FINISHED;
    }
}
