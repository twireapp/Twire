package com.perflyst.twire.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.JSONService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.service.TempStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.TimeUnit;

/**
 * Connects to Twitch to retrieve a list of streamers that a specified user follow.
 *
 * @return An ArrayList of streamnames
 */

public class GetTwitchUserFollows extends AsyncTask<Object, Void, ArrayList<ChannelInfo>> {
    private final String LOG_TAG = getClass().getSimpleName();
    private final String NAME_STRING_KEY = "name";
    private long timerStart = System.currentTimeMillis();
    private Context baseContext;

    private ArrayList<Integer> getFollowIdsFromJSONObject(JSONObject mJSON) throws JSONException {
        String FOLLOWS_ARRAY_KEY = "follows";
        JSONArray followsArray = mJSON.getJSONArray(FOLLOWS_ARRAY_KEY);
        ArrayList<Integer> followIds = new ArrayList<>();

        // For every JSON object (follow) in the array, add it to the list of followed streamers
        for (int j = 0; j < followsArray.length(); j++) {
            String CHANNEL_OBJECT_KEY = "channel";
            JSONObject channelObject = followsArray.getJSONObject(j).getJSONObject(CHANNEL_OBJECT_KEY);
            try {
                ChannelInfo mChannelInfo = JSONService.getStreamerInfo(channelObject, false);
                Service.updateStreamerInfoDb(mChannelInfo, baseContext); // Update the current database object
                followIds.add(mChannelInfo.getUserId());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        return followIds;
    }

    @Override
    protected ArrayList<ChannelInfo> doInBackground(Object... params) {
        ArrayList<Integer> userSubs = new ArrayList<>();

        int currentOffset = 0;
        String userName = (String) params[0];
        baseContext = (Context) params[1];

        Settings mSettings = new Settings(baseContext);
        int userId = mSettings.getGeneralTwitchUserID();

        int MAXIMUM_FOLLOWS_FOR_QUERY = 20;
        final String BASE_URL = "https://api.twitch.tv/kraken/users/"
                + userId
                + "/follows/channels?direction=DESC&limit="
                + MAXIMUM_FOLLOWS_FOR_QUERY
                + "&offset=" + currentOffset + "&sortby=created_at";


        // Get all the userIds of a users follows
        try {
            // First create a JSON string to get the total amount of follows. Then create a similar JSON string with a limit of the total amount of follows
            JSONObject JSONStringFull = new JSONObject(Service.urlToJSONString(BASE_URL));
            String TOTAL_FOLLOWS_INTEGER_KEY = "_total";
            int total = JSONStringFull.getInt(TOTAL_FOLLOWS_INTEGER_KEY);
            int roundedTotal = ((total + (MAXIMUM_FOLLOWS_FOR_QUERY - 1)) / MAXIMUM_FOLLOWS_FOR_QUERY) * MAXIMUM_FOLLOWS_FOR_QUERY;// Round up the nearest MAXIMUM

            int numberOfQueries = roundedTotal / MAXIMUM_FOLLOWS_FOR_QUERY;
            ArrayList<FollowIdsFromURLThread> mThreads = new ArrayList<>();
            for (int i = 0; i < numberOfQueries - 1; i++) {
                currentOffset += MAXIMUM_FOLLOWS_FOR_QUERY;
                String newURL = "https://api.twitch.tv/kraken/users/"
                        + userId
                        + "/follows/channels?direction=DESC&limit="
                        + MAXIMUM_FOLLOWS_FOR_QUERY
                        + "&offset=" + currentOffset + "&sortby=created_at";

                FollowIdsFromURLThread mFollowThread = new FollowIdsFromURLThread(newURL);
                mFollowThread.start();
                mThreads.add(mFollowThread);
            }

            // Make sure all the threads finish
            for (Thread mThread : mThreads) {
                try {
                    mThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Get the results from the threads and add them to the "result" list
            userSubs.addAll(getFollowIdsFromJSONObject(JSONStringFull));
            for (FollowIdsFromURLThread mThread : mThreads) {
                userSubs.addAll(mThread.getFollowIds());
            }

            Log.d(LOG_TAG, "Follows Found " + userSubs.size() + " - Should be " + total + " - With " + mThreads.size() + " threads");


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
                boolean result = Service.deleteStreamerInfoFromDB(baseContext, si);
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
            Object[] arrayTemp = {streamersToAddToDB, baseContext};
            AddFollowsToDB addFollowsToDBTask = new AddFollowsToDB();
            addFollowsToDBTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, arrayTemp);
        } else {
            Log.d(LOG_TAG, "Found no new streamers to add to the database");
        }

        long duration = System.currentTimeMillis() - this.timerStart;
        Log.d(LOG_TAG, "Completed task in " + TimeUnit.MILLISECONDS.toSeconds(duration) + " seconds");
    }

    private class FollowIdsFromURLThread extends Thread {
        ArrayList<Integer> mFollowIds = new ArrayList<>();
        private String URL;

        FollowIdsFromURLThread(String URL) {
            this.URL = URL;
        }

        @Override
        public void run() {
            try {
                JSONObject mJSON = new JSONObject(Service.urlToJSONString(URL));
                mFollowIds = getFollowIdsFromJSONObject(mJSON);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        ArrayList<Integer> getFollowIds() {
            return mFollowIds;
        }
    }

    private class StreamerInfoFromIdsThread extends Thread {
        private ArrayList<Integer> userIds;
        private ArrayList<ChannelInfo> streamers = new ArrayList<>();

        StreamerInfoFromIdsThread(ArrayList<Integer> ids) {
            this.userIds = ids;
        }

        @Override
        public void run() {
            for (Integer name : userIds) {
                ChannelInfo info = Service.getStreamerInfoFromUserId(name);
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
