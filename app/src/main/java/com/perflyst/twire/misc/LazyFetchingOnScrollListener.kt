package com.perflyst.twire.misc

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.util.concurrent.ListenableFuture
import com.perflyst.twire.activities.main.LazyFetchingActivity
import com.perflyst.twire.tasks.GetVisualElementsTask
import com.perflyst.twire.utils.Execute

/**
 * Created by Sebastian on 06-08-2015.
 */
class LazyFetchingOnScrollListener<T>(
    mActivity: AppCompatActivity?,
    mMainToolbar: Toolbar?,
    mDecorativeToolbar: Toolbar?,
    mToolbarShadow: View?,
    mIconCircle: View?,
    mIconText: TextView?,
    private val mLazyFetchingActivity: LazyFetchingActivity<T>,
    isMainActivity: Boolean
) : UniversalOnScrollListener(
    mActivity,
    mMainToolbar,
    mDecorativeToolbar,
    mToolbarShadow,
    mIconCircle,
    mIconText,
    isMainActivity
) {
    private var getElementsTask: ListenableFuture<MutableList<T>>? = null


    constructor(aLazyFetchingActivity: LazyFetchingActivity<T>) : this(
        null,
        null,
        null,
        null,
        null,
        null,
        aLazyFetchingActivity,
        false
    )

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (dy > 0) checkForNewElements(recyclerView)
    }

    fun resetAndFetch(recyclerView: RecyclerView) {
        if (getElementsTask != null) getElementsTask!!.cancel(true)
        getElementsTask = null
        checkForNewElements(recyclerView)
    }

    fun checkForNewElements(recyclerView: RecyclerView) {
        val maxElementsToFetchTotal = mLazyFetchingActivity.maxElementsToFetch

        val mAdapter = recyclerView.adapter
        if (mAdapter == null) {
            return
        }

        // Only bother to check if we need to fetch more game objects if we are not already in the process of doing so.
        if (getElementsTask == null && mAdapter.itemCount < maxElementsToFetchTotal) {
            val lm = recyclerView.layoutManager as GridLayoutManager?

            // If there are only two rows left, then fetch more.
            if (lm!!.findLastCompletelyVisibleItemPosition() >= lm.getItemCount() - lm.spanCount * 2 - 1) {
                getElementsTask = Execute.background(
                    GetVisualElementsTask(mLazyFetchingActivity)
                ) { elements -> getElementsTask = null }
                mLazyFetchingActivity.startProgress()
            }
        }
    }
}
