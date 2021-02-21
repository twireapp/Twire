package com.perflyst.twire.tasks;

import android.os.AsyncTask;

import com.perflyst.twire.service.Service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Sebastian Rask on 18-04-2016.
 */
public class FollowTask extends AsyncTask<String, Void, Boolean> {
    private final FollowResult callback;

    public FollowTask(FollowResult callback) {
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        URL url;
        try {
            url = new URL(params[0]);
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("PUT");
            httpCon.setRequestProperty("Client-ID", Service.getApplicationClientID());
            httpCon.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json");
            OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
            out.write("Resource content");
            out.close();
            int response = httpCon.getResponseCode();

            int FOLLOW_UNSUCCESSFUL = 422;
            return response != FOLLOW_UNSUCCESSFUL;
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

    public interface FollowResult {
        void onTaskDone(Boolean result);
    }
}
