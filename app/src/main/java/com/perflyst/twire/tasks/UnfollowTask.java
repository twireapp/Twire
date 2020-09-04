package com.perflyst.twire.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.perflyst.twire.service.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Sebastian Rask on 18-04-2016.
 */
public class UnfollowTask extends AsyncTask<String, Void, Boolean> {
    private final String LOG_TAG = getClass().getSimpleName();
    private final UnFollowResult callback;

    public UnfollowTask(UnFollowResult callback) {
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        URL url;
        try {
            url = new URL(params[0]);
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setRequestProperty(
                    "Content-Type", "application/x-www-form-urlencoded");
            httpCon.setRequestMethod("DELETE");
            httpCon.setRequestProperty("Client-ID", Service.getApplicationClientID());
            httpCon.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json");
            httpCon.connect();
            int code = httpCon.getResponseCode();

            Log.d(LOG_TAG, "Unfollow response: " + code);

            int UNFOLLOW_SUCCESS = 204;
            return code == UNFOLLOW_SUCCESS;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        callback.onTaskDone(aBoolean);
    }

    public interface UnFollowResult {
        void onTaskDone(Boolean result);
    }
}
