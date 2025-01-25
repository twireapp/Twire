package com.perflyst.twire.activities.main;

import com.google.common.collect.Lists;
import com.perflyst.twire.R;
import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.adapters.StreamsAdapter;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.model.UserInfo;
import com.perflyst.twire.service.TempStorage;
import com.perflyst.twire.tasks.GetFollowsFromDB;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.StreamAutoSpanBehaviour;

import java.util.List;

import timber.log.Timber;

public class MyStreamsActivity extends LazyMainActivity<StreamInfo> {

    @Override
    protected int getActivityIconRes() {
        return R.drawable.ic_favorite;
    }

    @Override
    protected int getActivityTitleRes() {
        return R.string.my_streams_activity_title;
    }

    @Override
    protected AutoSpanBehaviour constructSpanBehaviour() {
        return new StreamAutoSpanBehaviour();
    }

    @Override
    protected MainActivityAdapter<StreamInfo, ?> constructAdapter(AutoSpanRecyclerView recyclerView) {
        return new StreamsAdapter(recyclerView, this);
    }

    @Override
    public void addToAdapter(List<StreamInfo> aObjectList) {
        mAdapter.addList(aObjectList);
        Timber.i("Adding Top Streams: %s", aObjectList.size());
    }

    @Override
    public List<StreamInfo> getVisualElements() {
        if (!TempStorage.hasLoadedStreamers()) {
            new GetFollowsFromDB(this).call();
        }

        var channels = TempStorage.getLoadedStreamers().stream().map(UserInfo::getUserId).toList();
        var results = Lists.partition(channels, 100)
                .stream()
                .flatMap(chunk -> TwireApplication.helix.getStreams(null, null, null, 100, null, null, chunk, null)
                        .execute()
                        .getStreams()
                        .stream()
                )
                .map(StreamInfo::new)
                .toList();

        setMaxElementsToFetch(results.size());

        return results;
    }
}
