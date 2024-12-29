package com.perflyst.twire.tasks;

import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.misc.UniversalOnScrollListener;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

/**
 * Created by Sebastian Rask on 14-04-2016.
 */
public class ScrollToStartPositionTask implements Callable<Void> {
    private final WeakReference<RecyclerView> recyclerView;
    private final UniversalOnScrollListener mScrollListener;

    public ScrollToStartPositionTask(RecyclerView recyclerView, UniversalOnScrollListener mScrollListener) {
        this.recyclerView = new WeakReference<>(recyclerView);
        this.mScrollListener = mScrollListener;
    }

    public Void call() {
        recyclerView.get().smoothScrollToPosition(0);

        while (mScrollListener.getAmountScrolled() != 0) {

        }
        return null;
    }
}
