package com.perflyst.twire.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.perflyst.twire.activities.main.LazyFetchingActivity;

import java.util.ArrayList;
import java.util.List;

public class GetVisualElementsTask<T> extends AsyncTask<Void, Void, List<T>> {
    private final LazyFetchingActivity<T> mLazyActivity;
    private final String LOG_TAG = getClass().getSimpleName();
    private boolean isCancelled = false;
    private int offset,
            limit;

    public GetVisualElementsTask(LazyFetchingActivity<T> mLazyActivity) {
        this.mLazyActivity = mLazyActivity;
    }

    @Override
    protected final List<T> doInBackground(Void... params) {
        List<T> resultList = new ArrayList<>();

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

        if (mVisualElements.isEmpty()) {
            Log.i(LOG_TAG, "ADDING 0 VISUAL ELEMENTS");
            mLazyActivity.notifyUserNoElementsAdded();
        } else {
            mLazyActivity.setCurrentOffset(offset + limit);
        }

        mLazyActivity.addToAdapter(mVisualElements);
        mLazyActivity.stopProgress();
        mLazyActivity.stopRefreshing();
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }
}
