package com.perflyst.twire.activities.main;

import android.os.Bundle;
import android.os.Handler;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.perflyst.twire.R;
import com.perflyst.twire.misc.LazyFetchingOnScrollListener;
import com.perflyst.twire.tasks.GetVisualElementsTask;
import com.perflyst.twire.utils.Execute;

/**
 * Main Activity that loads it's content only when it is needed.
 */
public abstract class LazyMainActivity<T> extends MainActivity<T> implements LazyFetchingActivity<T> {
    protected LazyFetchingOnScrollListener<T> mOnScrollListener;

    protected Snackbar snackbar;

    protected String cursor = null;
    protected int elementsToFetchLimit = 20;
    protected int maxElementsToFetch = 500;

    /**
     * Refreshes the Activity's visual elements by clearing the adapter and setting the cursor to null.
     * After the adapter is finished clearing, new elements will be loaded and added.
     */
    @Override
    public void refreshElements() {
        int duration = mAdapter.clear();

        // Fetch new elements after the adapter animations are done
        // ToDo: These elements should be loaded while the animations are running. But not be added until the animations are done.
        new Handler().postDelayed(() -> {
            setCursor(null);
            getRecyclerView().scrollToPosition(0);
            mOnScrollListener.resetAndFetch(getRecyclerView());
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
                this, mMainToolbar, mDecorativeToolbar, mToolbarShadow, mCircleIconWrapper, mTitleView, this, true
        );
        mScrollListener = mOnScrollListener;
        mRecyclerView.addOnScrollListener(mScrollListener);

        // After adding elements, check we should fetching more to properly fill the UI.
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                mOnScrollListener.checkForNewElements(mRecyclerView);
            }
        });

        GetVisualElementsTask<T> getElementsTask = new GetVisualElementsTask<>(this);
        Execute.background(getElementsTask);
        startProgress();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        hideErrorView();
    }

    private Snackbar setupSnackbar() {
        String responseMessage = getString(R.string.no_server_response_message);
        Snackbar snackbar = Snackbar
                .make(mRecyclerView, responseMessage, Snackbar.LENGTH_LONG);
        snackbar.setAction(android.R.string.ok, v -> scrollToTopAndRefresh());

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
        /*
        We don´t want to notify the user when no elements are added
        if (mAdapter.getItemCount() == 0 && mAdapter.getItemCount() != getMaxElementsToFetch()) {
            if (!snackbar.isShown()) {
                snackbar.show();
            }
        }
         */
    }

    @Override
    public void checkIsBackFromMainActivity() {
        shouldShowErrorView();
        super.checkIsBackFromMainActivity();
    }

    @Override
    public void startProgress() {
        mProgressView.show();
    }

    @Override
    public void stopProgress() {
        mProgressView.hide();
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
    public String getCursor() {
        return cursor;
    }

    @Override
    public void setCursor(String cursor) {
        this.cursor = cursor;
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
