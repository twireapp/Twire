package com.perflyst.twire.tasks;

import com.perflyst.twire.activities.main.LazyFetchingActivity;
import com.perflyst.twire.utils.Execute;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import timber.log.Timber;

public class GetVisualElementsTask<T> implements Callable<List<T>> {
    private final LazyFetchingActivity<T> mLazyActivity;

    public GetVisualElementsTask(LazyFetchingActivity<T> mLazyActivity) {
        this.mLazyActivity = mLazyActivity;
    }

    public final List<T> call() {
        final List<T> resultList = new ArrayList<>();

        try {
            resultList.addAll(mLazyActivity.getVisualElements());
        } catch (Exception e) {
            Timber.e(e);
        }

        Execute.ui(() -> {
            if (resultList.isEmpty()) {
                Timber.i("ADDING 0 VISUAL ELEMENTS");
                mLazyActivity.notifyUserNoElementsAdded();
            }

            mLazyActivity.addToAdapter(resultList);
            mLazyActivity.stopProgress();
            mLazyActivity.stopRefreshing();
        });

        return resultList;
    }
}
