package com.perflyst.twire.activities.main;

import android.os.AsyncTask;

import com.perflyst.twire.R;
import com.perflyst.twire.adapters.ChannelsAdapter;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.TempStorage;
import com.perflyst.twire.tasks.GetFollowsFromDB;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.ChannelAutoSpanBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Activity that shows the user's Twitch Follows.
 * If there are no follows in TempStorage when the activity is created, then the activity initiates an AsyncTask that connects to Twitch, that loads and adds the follows to this activity
 */
public class MyChannelsActivity extends LazyMainActivity<ChannelInfo> {
    @Override
    protected MainActivityAdapter<ChannelInfo, ?> constructAdapter(AutoSpanRecyclerView recyclerView) {
        return new ChannelsAdapter(mRecyclerView, getBaseContext(), this);
    }

    @Override
    protected int getActivityIconRes() {
        return R.drawable.ic_person;
    }

    @Override
    protected int getActivityTitleRes() {
        return R.string.my_channels_activity_title;
    }

    @Override
    protected AutoSpanBehaviour constructSpanBehaviour() {
        return new ChannelAutoSpanBehaviour();
    }

    @Override
    public void addToAdapter(List<ChannelInfo> aObjectList) {
        mAdapter.addList(aObjectList);
    }

    @Override
    public List<ChannelInfo> getVisualElements() throws ExecutionException, InterruptedException {
        if (TempStorage.hasLoadedStreamers()) {
            return new ArrayList<>(TempStorage.getLoadedStreamers());
        }

        GetFollowsFromDB subscriptionsTask = new GetFollowsFromDB();
        subscriptionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getBaseContext());

        return new ArrayList<>(subscriptionsTask.get().values());
    }
}
