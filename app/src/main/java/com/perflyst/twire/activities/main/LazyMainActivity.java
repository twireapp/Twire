package com.perflyst.twire.activities.main;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import com.google.android.material.snackbar.Snackbar;
import com.perflyst.twire.R;
import com.perflyst.twire.misc.LazyFetchingOnScrollListener;
import com.perflyst.twire.model.MainElement;
import com.perflyst.twire.tasks.GetVisualElementsTask;

/**
 * Main Activity that loads it's content only when it is needed.
 */
public abstract class LazyMainActivity<T extends Comparable<T> & MainElement> extends MainActivity<T> implements LazyFetchingActivity<T> {
    protected LazyFetchingOnScrollListener<T> mOnScrollListener;

    protected Snackbar snackbar;

    protected int currentOffset = 0;
    protected int elementsToFetchLimit = 10;
    protected int maxElementsToFetch = 500;

    /**
     * Refreshes the Activity's visual elements by clearing the adapter and setting the current offset to one.
     * After the adapter is finished clearing, new elements will be loaded and added.
     */
    @Override
    public void refreshElements() {
        int duration = mAdapter.clear();

        // Fetch new elements after the adapter animations are done
        // ToDo: These elements should be loaded while the animations are running. But not be added until the animations are done.
        new Handler().postDelayed(() -> {
            setCurrentOffset(0);
            getRecyclerView().scrollToPosition(0);
            GetVisualElementsTask<T> getTopGamesTask = new GetVisualElementsTask<>(LazyMainActivity.this);
            getTopGamesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }, duration);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        // Set up the snackbar for when the user experience a no reply from twitch's servers
        snackbar = setupSnackbar();

        mSwipeRefreshLayout.setProgressViewOffset(true, (int) getResources().getDimension(R.dimen.swipe_refresh_start_offset), (int) getResources().getDimension(R.dimen.swipe_refresh_end_offset));
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshElements);

        //  Set up the specialized OnScrollListener
        mRecyclerView.clearOnScrollListeners();
        mOnScrollListener = new LazyFetchingOnScrollListener<>(
                this, mMainToolbar, mDecorativeToolbar, mToolbarShadow, mCircleIconWrapper, mTitleView, LOG_TAG, this, true
        );
        mScrollListener = mOnScrollListener;
        mRecyclerView.addOnScrollListener(mScrollListener);

        GetVisualElementsTask<T> getElementsTask = new GetVisualElementsTask<>(this);
        getElementsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentOffset == 0) {
            startRefreshing();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        hideErrorView();
    }

    private Snackbar setupSnackbar() {
        String responseMessage = getResources().getString(R.string.no_server_response_message);
        String actionString = getResources().getString(R.string.ok);
        Snackbar snackbar = Snackbar
                .make(mRecyclerView, responseMessage, Snackbar.LENGTH_LONG);
        snackbar.setAction(actionString, v -> scrollToTopAndRefresh());

        return snackbar;
    }

    protected void shouldShowErrorView() {
        runOnUiThread(() -> {
            if (mErrorEmoteView != null && mErrorView != null) {
                if (getMaxElementsToFetch() == 0) {
                    showErrorView();
                } else {
                    hideErrorView();
                }
            }
        });
    }

    @Override
    public void notifyUserNoElementsAdded() {
        if (mAdapter.getItemCount() == 0 && mAdapter.getItemCount() != getMaxElementsToFetch()) {
            if (!snackbar.isShown()) {
                snackbar.show();
            }
        }
    }

    @Override
    public void checkIsBackFromMainActivity() {
        shouldShowErrorView();
        super.checkIsBackFromMainActivity();
    }

    @Override
    public void startProgress() {
        mProgressView.start();
    }

    @Override
    public void stopProgress() {
        mProgressView.stop();
    }

    @Override
    public void startRefreshing() {
        mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void stopRefreshing() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public int getMaxElementsToFetch() {
        return maxElementsToFetch;
    }

    @Override
    public void setMaxElementsToFetch(int maxElementsToFetch) {
        this.maxElementsToFetch = maxElementsToFetch;
    }

    @Override
    public int getCurrentOffset() {
        return currentOffset;
    }

    @Override
    public void setCurrentOffset(int currentOffset) {
        this.currentOffset = currentOffset;
    }

    @Override
    public int getLimit() {
        return elementsToFetchLimit;
    }

    @Override
    public void setLimit(int aLimit) {
        elementsToFetchLimit = aLimit;
    }
}
