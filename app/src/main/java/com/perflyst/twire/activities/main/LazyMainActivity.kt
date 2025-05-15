package com.perflyst.twire.activities.main

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.perflyst.twire.R
import com.perflyst.twire.misc.LazyFetchingOnScrollListener
import com.perflyst.twire.tasks.GetVisualElementsTask
import com.perflyst.twire.utils.Execute

/**
 * Main Activity that loads it's content only when it is needed.
 */
abstract class LazyMainActivity<T> : MainActivity<T>(), LazyFetchingActivity<T> {
    protected lateinit var mOnScrollListener: LazyFetchingOnScrollListener<T>

    protected var snackbar: Snackbar? = null

    override var cursor: String? = null
    override var limit: Int = 20
    override var maxElementsToFetch: Int = 500

    /**
     * Refreshes the Activity's visual elements by clearing the adapter and setting the cursor to null.
     * After the adapter is finished clearing, new elements will be loaded and added.
     */
    override fun refreshElements() {
        val duration = mAdapter.clear()

        // Fetch new elements after the adapter animations are done
        // ToDo: These elements should be loaded while the animations are running. But not be added until the animations are done.
        Handler().postDelayed({
            cursor = null
            recyclerView.scrollToPosition(0)
            mOnScrollListener.resetAndFetch(recyclerView)
        }, duration.toLong())
    }

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)

        // Set up the snackbar for when the user experience a no reply from twitch's servers
        snackbar = setupSnackbar()

        mSwipeRefreshLayout.setProgressViewOffset(
            true,
            getResources().getDimension(R.dimen.swipe_refresh_start_offset).toInt(),
            getResources().getDimension(R.dimen.swipe_refresh_end_offset).toInt()
        )
        mSwipeRefreshLayout.setOnRefreshListener { this.refreshElements() }

        //  Set up the specialized OnScrollListener
        mRecyclerView.clearOnScrollListeners()
        mOnScrollListener = LazyFetchingOnScrollListener(
            this,
            mMainToolbar,
            mDecorativeToolbar,
            mToolbarShadow,
            mCircleIconWrapper,
            mTitleView,
            this,
            true
        )
        mScrollListener = mOnScrollListener
        mRecyclerView.addOnScrollListener(mScrollListener)

        // After adding elements, check we should fetching more to properly fill the UI.
        mAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                mOnScrollListener.checkForNewElements(mRecyclerView)
            }
        })

        val getElementsTask = GetVisualElementsTask<T>(this)
        Execute.background<MutableList<T>>(getElementsTask)
        startProgress()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        hideErrorView()
    }

    private fun setupSnackbar(): Snackbar {
        val responseMessage = getString(R.string.no_server_response_message)
        val snackbar = Snackbar
            .make(mRecyclerView, responseMessage, Snackbar.LENGTH_LONG)
        snackbar.setAction(
            android.R.string.ok
        ) { v: View? -> scrollToTopAndRefresh() }

        return snackbar
    }

    protected fun shouldShowErrorView() {
        runOnUiThread {
            if (maxElementsToFetch == 0) {
                showErrorView()
            } else {
                hideErrorView()
            }
        }
    }

    override fun notifyUserNoElementsAdded() {
        /*
        We don´t want to notify the user when no elements are added
        if (mAdapter.getItemCount() == 0 && mAdapter.getItemCount() != getMaxElementsToFetch()) {
            if (!snackbar.isShown()) {
                snackbar.show();
            }
        }
         */
    }

    override fun checkIsBackFromMainActivity() {
        shouldShowErrorView()
        super.checkIsBackFromMainActivity()
    }

    override fun startProgress() {
        mProgressView.show()
    }

    override fun stopProgress() {
        mProgressView.hide()
    }

    override fun startRefreshing() {
        mSwipeRefreshLayout.isRefreshing = true
    }

    override fun stopRefreshing() {
        mSwipeRefreshLayout.isRefreshing = false
    }
}
