package com.perflyst.twire.misc;

import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.activities.main.LazyFetchingActivity;
import com.perflyst.twire.tasks.GetVisualElementsTask;

/**
 * Created by Sebastian on 06-08-2015.
 */
public class LazyFetchingOnScrollListener<T> extends UniversalOnScrollListener {
    private final LazyFetchingActivity<T> mLazyFetchingActivity;
    private GetVisualElementsTask<T> getElementsTask;


    public LazyFetchingOnScrollListener(AppCompatActivity mActivity, Toolbar mMainToolbar, Toolbar mDecorativeToolbar, View mToolbarShadow, View mIconCircle, TextView mIconText, String LOG_TAG, LazyFetchingActivity<T> aLazyFetchingActivity, boolean isMainActivity) {
        super(mActivity, mMainToolbar, mDecorativeToolbar, mToolbarShadow, mIconCircle, mIconText, LOG_TAG, isMainActivity);
        this.getElementsTask = new GetVisualElementsTask<>(aLazyFetchingActivity);
        this.mLazyFetchingActivity = aLazyFetchingActivity;
    }

    public LazyFetchingOnScrollListener(String LOG_TAG, LazyFetchingActivity<T> aLazyFetchingActivity) {
        this(null, null, null, null, null, null, LOG_TAG, aLazyFetchingActivity, false);
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        checkForNewElements(recyclerView);
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        checkForNewElements(recyclerView);
    }

    public void resetAndFetch(RecyclerView recyclerView) {
        getElementsTask.setCancelled(true);
        getElementsTask.cancel(true);
        getElementsTask = new GetVisualElementsTask<>(mLazyFetchingActivity);
        checkForNewElements(recyclerView);
    }

    public void checkForNewElements(RecyclerView recyclerView) {
        int currentOffset = mLazyFetchingActivity.getCurrentOffset();
        int maxElementsToFetchTotal = mLazyFetchingActivity.getMaxElementsToFetch();

        // If the task has already been run, make a new task as a task can only be run once.
        if (getElementsTask.getStatus() == AsyncTask.Status.FINISHED) {
            getElementsTask = new GetVisualElementsTask<>(mLazyFetchingActivity);
        }

        // Only bother to check if we need to fetch more game objects if we are not already in the process of doing so.
        if (getElementsTask.getStatus() != AsyncTask.Status.RUNNING && currentOffset < maxElementsToFetchTotal) {
            GridLayoutManager lm = (GridLayoutManager) recyclerView.getLayoutManager();
            RecyclerView.Adapter mAdapter = recyclerView.getAdapter();
            if (mAdapter == null) {
                return;
            }

            int lastViewPosition = lm.findLastVisibleItemPosition();
            int spanCount = lm.getSpanCount();
            int itemCount = mAdapter.getItemCount();
            final double FETCH_WHEN_BELOW_FIVE = 5;
            final double NUMBER_OF_ROWS = Math.ceil(itemCount / (spanCount * 1.0)); // Round UP to the nearest Integer
            final double LAST_ROW_VISIBLE = Math.ceil(lastViewPosition / (spanCount * 1.0)); // Round UP to the nearest Integer

            // If the Second to last or the last row is visible, then fetch more game objects.
            if (LAST_ROW_VISIBLE >= NUMBER_OF_ROWS - FETCH_WHEN_BELOW_FIVE) {
                getElementsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                mLazyFetchingActivity.startProgress();
            }
        }
    }
}
