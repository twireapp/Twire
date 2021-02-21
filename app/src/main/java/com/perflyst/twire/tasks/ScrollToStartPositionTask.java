package com.perflyst.twire.tasks;

import android.os.AsyncTask;

import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.misc.UniversalOnScrollListener;

import java.lang.ref.WeakReference;

/**
 * Created by Sebastian Rask on 14-04-2016.
 */
public class ScrollToStartPositionTask extends AsyncTask<Void, Void, Void> {
    private final PositionCallBack callBack;
    private final WeakReference<RecyclerView> recyclerView;
    private final UniversalOnScrollListener mScrollListener;

    public ScrollToStartPositionTask(PositionCallBack callBack, RecyclerView recyclerView, UniversalOnScrollListener mScrollListener) {
        this.callBack = callBack;
        this.recyclerView = new WeakReference<>(recyclerView);
        this.mScrollListener = mScrollListener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        recyclerView.get().smoothScrollToPosition(0);
    }

    @Override
    protected Void doInBackground(Void... params) {
        while (mScrollListener.getAmountScrolled() != 0) {

        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        callBack.positionReached();
    }

    public interface PositionCallBack {
        void positionReached();
    }
}
