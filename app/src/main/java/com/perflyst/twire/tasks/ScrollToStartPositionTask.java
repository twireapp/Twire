package com.perflyst.twire.tasks;

import android.os.AsyncTask;

import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.misc.UniversalOnScrollListener;

/**
 * Created by Sebastian Rask on 14-04-2016.
 */
public class ScrollToStartPositionTask extends AsyncTask<Void, Void, Void> {
    private PositionCallBack callBack;
    private RecyclerView recyclerView;
    private UniversalOnScrollListener mScrollListener;

    public ScrollToStartPositionTask(PositionCallBack callBack, RecyclerView recyclerView, UniversalOnScrollListener mScrollListener) {
        this.callBack = callBack;
        this.recyclerView = recyclerView;
        this.mScrollListener = mScrollListener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        recyclerView.smoothScrollToPosition(0);
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

    @Override
    protected void onCancelled() {
        super.onCancelled();
        callBack.cancelled();
    }

    public interface PositionCallBack {
        void positionReached();

        void cancelled();
    }
}
