package com.perflyst.twire.activities;

import static com.perflyst.twire.misc.Utils.getSystemLanguage;

import android.content.Intent;

import com.perflyst.twire.R;
import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.activities.main.LazyMainActivity;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.adapters.StreamsAdapter;
import com.perflyst.twire.model.Game;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.StreamAutoSpanBehaviour;

import java.util.List;

public class GameActivity extends LazyMainActivity<StreamInfo> {
    private Game game;

    @Override
    protected MainActivityAdapter<StreamInfo, ?> constructAdapter(AutoSpanRecyclerView recyclerView) {
        return new StreamsAdapter(recyclerView, this);
    }

    @Override
    public void customizeActivity() {
        Intent intent = getIntent();
        game = intent.getParcelableExtra(getString(R.string.game_intent_key));
        assert game != null;
        mTitleView.setText(game.getGameTitle());
    }

    @Override
    protected int getActivityIconRes() {
        return R.drawable.ic_sports_esports;
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
    public void addToAdapter(List<StreamInfo> streamsToAdd) {
        mAdapter.addList(streamsToAdd);
    }

    @Override
    public List<StreamInfo> getVisualElements() {
        final String languageFilter = Settings.getGeneralFilterTopStreamsByLanguage() ? getSystemLanguage() : null;
        var response = TwireApplication.helix.getStreams(null, getCursor(), null, getLimit(), List.of(game.getGameId()), List.of(languageFilter), null, null).execute();
        setCursor(response.getPagination().getCursor());
        return response.getStreams().stream().map(StreamInfo::new).toList();
    }
}
