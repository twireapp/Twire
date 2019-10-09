package com.perflyst.twire.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.perflyst.twire.activities.main.LazyFetchingActivity;

import java.util.ArrayList;
import java.util.List;

public class GetVisualElementsTask<T> extends AsyncTask<LazyFetchingActivity, Void, List<T>> {
    private LazyFetchingActivity mLazyActivity;
    private String LOG_TAG = getClass().getSimpleName();
    private long timerStart = System.currentTimeMillis();
    private boolean isCancelled = false;
    private int total_json_response = 0,
            offset,
            limit;

    @Override
    protected List<T> doInBackground(LazyFetchingActivity... params) {
        List<T> resultList = new ArrayList<>();

        mLazyActivity = params[0];
        offset = mLazyActivity.getCurrentOffset();
        limit = mLazyActivity.getLimit();

        try {
            resultList = mLazyActivity.getVisualElements();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultList;
    }

    @Override
    protected void onPostExecute(List<T> mVisualElements) {
        super.onPostExecute(mVisualElements);
        if (isCancelled) {
            return;
        }

        if (mVisualElements.size() == 0) {
            Log.i(LOG_TAG, "ADDING 0 VISUAL ELEMENTS");
            mLazyActivity.notifyUserNoElementsAdded();
        } else {
            mLazyActivity.setCurrentOffset(offset + limit);
        }

        mLazyActivity.addToAdapter(mVisualElements);
        mLazyActivity.stopProgress();
        mLazyActivity.stopRefreshing();
        long duration = System.currentTimeMillis() - timerStart;
    }

    public boolean getCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }
}
