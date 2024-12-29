package com.perflyst.twire.misc;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.ListenableFuture;
import com.perflyst.twire.activities.main.LazyFetchingActivity;
import com.perflyst.twire.tasks.GetVisualElementsTask;
import com.perflyst.twire.utils.Execute;

import java.util.List;

/**
 * Created by Sebastian on 06-08-2015.
 */
public class LazyFetchingOnScrollListener<T> extends UniversalOnScrollListener {
    private final LazyFetchingActivity<T> mLazyFetchingActivity;
    @Nullable
    private ListenableFuture<List<T>> getElementsTask;


    public LazyFetchingOnScrollListener(AppCompatActivity mActivity, Toolbar mMainToolbar, Toolbar mDecorativeToolbar, View mToolbarShadow, View mIconCircle, TextView mIconText, String LOG_TAG, LazyFetchingActivity<T> aLazyFetchingActivity, boolean isMainActivity) {
        super(mActivity, mMainToolbar, mDecorativeToolbar, mToolbarShadow, mIconCircle, mIconText, LOG_TAG, isMainActivity);
        this.mLazyFetchingActivity = aLazyFetchingActivity;
    }

    public LazyFetchingOnScrollListener(String LOG_TAG, LazyFetchingActivity<T> aLazyFetchingActivity) {
        this(null, null, null, null, null, null, LOG_TAG, aLazyFetchingActivity, false);
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (dy > 0) checkForNewElements(recyclerView);
    }

    public void resetAndFetch(RecyclerView recyclerView) {
        if (getElementsTask != null) getElementsTask.cancel(true);
        getElementsTask = null;
        checkForNewElements(recyclerView);
    }

    public void checkForNewElements(RecyclerView recyclerView) {
        int maxElementsToFetchTotal = mLazyFetchingActivity.getMaxElementsToFetch();

        RecyclerView.Adapter mAdapter = recyclerView.getAdapter();
        if (mAdapter == null) {
            return;
        }

        // Only bother to check if we need to fetch more game objects if we are not already in the process of doing so.
        if (getElementsTask == null && mAdapter.getItemCount() < maxElementsToFetchTotal) {
            GridLayoutManager lm = (GridLayoutManager) recyclerView.getLayoutManager();

            // If there are only two rows left, then fetch more.
            if (lm.findLastCompletelyVisibleItemPosition() >= lm.getItemCount() - lm.getSpanCount() * 2 - 1) {
                getElementsTask = Execute.background(
                        new GetVisualElementsTask<>(mLazyFetchingActivity),
                        (elements) -> getElementsTask = null
                );
                mLazyFetchingActivity.startProgress();
            }
        }
    }
}
