package com.perflyst.twire.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.service.TempStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.TimeUnit;

import okhttp3.Request;

import static com.perflyst.twire.service.Service.SimpleResponse;
import static com.perflyst.twire.service.Service.deleteStreamerInfoFromDB;
import static com.perflyst.twire.service.Service.getApplicationClientID;
import static com.perflyst.twire.service.Service.getStreamerInfoFromUserId;
import static com.perflyst.twire.service.Service.makeRequest;

/**
 * Connects to Twitch to retrieve a list of streamers that a specified user follow.
 * <p>
 * Returns an ArrayList of stream names
 */

public class GetTwitchUserFollows extends AsyncTask<Object, Void, ArrayList<ChannelInfo>> {
    private final String LOG_TAG = getClass().getSimpleName();
    private final long timerStart = System.currentTimeMillis();
    private WeakReference<Context> baseContext;

    @Override
    protected ArrayList<ChannelInfo> doInBackground(Object... params) {
        ArrayList<Integer> userSubs = new ArrayList<>();

        String currentCursor = "";
        baseContext = new WeakReference<>((Context) params[1]);

        Settings mSettings = new Settings(baseContext.get());
        int userId = mSettings.getGeneralTwitchUserID();

        final String BASE_URL = "https://api.twitch.tv/helix/users/follows?first=100&from_id=" + userId + "&after=";

        // Get all the userIds of a users follows
        try {
            while (true) {
                Request request = new Request.Builder()
                        .url(BASE_URL + currentCursor)
                        .header("Client-ID", getApplicationClientID())
                        .header("Authorization", "Bearer " + mSettings.getGeneralTwitchAccessToken())
                        .build();

                SimpleResponse response = makeRequest(request);
                if (response == null)
                    return null;

                JSONObject page = new JSONObject(response.body);
                JSONArray follows = page.getJSONArray("data");

                for (int i = 0; i < follows.length(); i++) {
                    JSONObject follow = follows.getJSONObject(i);
                    userSubs.add(follow.getInt("to_id"));
                }

                JSONObject pagination = page.getJSONObject("pagination");
                if (!pagination.has("cursor"))
                    break;

                currentCursor = pagination.getString("cursor");
            }
        } catch (JSONException e) {
            Log.w(LOG_TAG, e.getMessage());
        }
        // ------- Has now loaded all the user's followed streamers ----------

        ArrayList<Integer> loadedStreamerIds = new ArrayList<>();
        ArrayList<ChannelInfo> streamersToAddToDB = new ArrayList<>();

        // Get and save the streamerName of the already loadedStreamers
        for (ChannelInfo si : TempStorage.getLoadedStreamers())
            loadedStreamerIds.add(si.getUserId());

        // Get the follows of the user defined twitch username
        // If a retrieved follow is not in the loaded streamers - Then add it to the database.
        for (Integer si : loadedStreamerIds) {
            if (!userSubs.contains(si)) {
                boolean result = deleteStreamerInfoFromDB(baseContext.get(), si);
                try {
                    for (ChannelInfo info : TempStorage.getLoadedStreamers()) {
                        if (si.equals(info.getUserId()))
                            TempStorage.removeLoadedStreamer(info);
                    }
                } catch (ConcurrentModificationException e) {
                    e.printStackTrace();
                }

                if (result) {
                    Log.d(LOG_TAG, "Successfully removed " + si + " from database and loadedStreamers");
                } else {
                    Log.e(LOG_TAG, "Failed to remove " + si + " from database and loadedStreamers");
                }
            }
        }

        // Find the Twitch userIds that the app hasn't already loaded. Add it to the list of userIds that will be added to the database
        ArrayList<Integer> IdsToAddToDB = new ArrayList<>();
        ArrayList<StreamerInfoFromIdsThread> streamerInfoThreads = new ArrayList<>();
        for (Integer id : userSubs) {
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
                e.printStackTrace();
            }
        }

        // Get the result from the threads and add the StreamerInfo objects to the result list
        for (StreamerInfoFromIdsThread thread : streamerInfoThreads) {
            streamersToAddToDB.addAll(thread.getStreamers());
        }

        return streamersToAddToDB;
    }

    @Override
    protected void onPostExecute(ArrayList<ChannelInfo> streamersToAddToDB) {
        // If there are any streamers to add to the DB - Create a task and do so.
        if (streamersToAddToDB.size() > 0) {
            Log.d(LOG_TAG, "Starting task to add " + streamersToAddToDB.size() + " to the db");
            Object[] arrayTemp = {streamersToAddToDB, baseContext.get()};
            AddFollowsToDB addFollowsToDBTask = new AddFollowsToDB();
            addFollowsToDBTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, arrayTemp);
        } else {
            Log.d(LOG_TAG, "Found no new streamers to add to the database");
        }

        long duration = System.currentTimeMillis() - this.timerStart;
        Log.d(LOG_TAG, "Completed task in " + TimeUnit.MILLISECONDS.toSeconds(duration) + " seconds");
    }

    private class StreamerInfoFromIdsThread extends Thread {
        private final ArrayList<Integer> userIds;
        private final ArrayList<ChannelInfo> streamers = new ArrayList<>();

        StreamerInfoFromIdsThread(ArrayList<Integer> ids) {
            this.userIds = ids;
        }

        @Override
        public void run() {
            for (Integer name : userIds) {
                ChannelInfo info = getStreamerInfoFromUserId(name);
                if (info != null) {
                    info.setNotifyWhenLive(true); // Enable by default
                }
                streamers.add(info);
            }
            Log.d(LOG_TAG, "Thread - Adding " + streamers.size() + " streamers");
        }

        public ArrayList<ChannelInfo> getStreamers() {
            return streamers;
        }
    }
}
