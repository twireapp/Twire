package com.perflyst.twire.tasks;

import android.util.Log;

import com.perflyst.twire.activities.main.LazyFetchingActivity;
import com.perflyst.twire.utils.Execute;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class GetVisualElementsTask<T> implements Callable<List<T>> {
    private final LazyFetchingActivity<T> mLazyActivity;
    private final String LOG_TAG = getClass().getSimpleName();

    public GetVisualElementsTask(LazyFetchingActivity<T> mLazyActivity) {
        this.mLazyActivity = mLazyActivity;
    }

    public final List<T> call() {
        final List<T> resultList = new ArrayList<>();

        try {
            resultList.addAll(mLazyActivity.getVisualElements());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Execute.ui(() -> {
            if (resultList.isEmpty()) {
                Log.i(LOG_TAG, "ADDING 0 VISUAL ELEMENTS");
                mLazyActivity.notifyUserNoElementsAdded();
            }

            mLazyActivity.addToAdapter(resultList);
            mLazyActivity.stopProgress();
            mLazyActivity.stopRefreshing();
        });

        return resultList;
    }
}
