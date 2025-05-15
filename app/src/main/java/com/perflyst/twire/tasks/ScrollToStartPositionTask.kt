package com.perflyst.twire.tasks

import androidx.recyclerview.widget.RecyclerView
import com.perflyst.twire.misc.UniversalOnScrollListener
import java.lang.ref.WeakReference
import java.util.concurrent.Callable

/**
 * Created by Sebastian Rask on 14-04-2016.
 */
class ScrollToStartPositionTask(
    recyclerView: RecyclerView?,
    private val mScrollListener: UniversalOnScrollListener
) : Callable<Void?> {
    private val recyclerView: WeakReference<RecyclerView?> =
        WeakReference<RecyclerView?>(recyclerView)

    override fun call(): Void? {
        recyclerView.get()!!.smoothScrollToPosition(0)

        while (mScrollListener.amountScrolled != 0) {
        }
        return null
    }
}
