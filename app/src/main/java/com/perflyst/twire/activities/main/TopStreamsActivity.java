package com.perflyst.twire.activities.main;

import static com.perflyst.twire.misc.Utils.getSystemLanguage;

import com.perflyst.twire.R;
import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.adapters.StreamsAdapter;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.StreamAutoSpanBehaviour;

import java.util.List;

import timber.log.Timber;

public class TopStreamsActivity extends LazyMainActivity<StreamInfo> {

    @Override
    protected int getActivityIconRes() {
        return R.drawable.ic_group;
    }

    @Override
    protected int getActivityTitleRes() {
        return R.string.top_streams_activity_title;
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
    public void addToAdapter(List<StreamInfo> streamsToAdd) {
        mAdapter.addList(streamsToAdd);
        Timber.i("Adding Top Streams: %s", streamsToAdd.size());
    }

    @Override
    public List<StreamInfo> getVisualElements() {
        final List<String> languageFilter = Settings.getGeneralFilterTopStreamsByLanguage() ? List.of(getSystemLanguage()) : List.of();
        var response = TwireApplication.helix.getStreams(null, getCursor(), null, getLimit(), null, languageFilter, null, null).execute();
        setCursor(response.getPagination().getCursor());
        return response.getStreams().stream().map(StreamInfo::new).toList();
    }
}
