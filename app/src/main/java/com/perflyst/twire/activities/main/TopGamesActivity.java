package com.perflyst.twire.activities.main;

import com.perflyst.twire.R;
import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.adapters.GamesAdapter;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.model.Game;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.GameAutoSpanBehaviour;

import java.util.List;

import timber.log.Timber;

/**
 * Activity that loads and shows the top games on Twitch.
 * The Activity loads the content as it is needed.
 */
public class TopGamesActivity extends LazyMainActivity<Game> {

    @Override
    protected int getActivityIconRes() {
        return R.drawable.ic_games;
    }

    @Override
    protected int getActivityTitleRes() {
        return R.string.top_games_activity_title;
    }

    @Override
    protected AutoSpanBehaviour constructSpanBehaviour() {
        return new GameAutoSpanBehaviour();
    }

    @Override
    protected void customizeActivity() {
        super.customizeActivity();
        setLimit(20);
    }

    @Override
    public void addToAdapter(List<Game> aGamesList) {
        mAdapter.addList(aGamesList);
        Timber.i("Adding Top Games: %s", aGamesList.size());
    }

    @Override
    protected MainActivityAdapter<Game, ?> constructAdapter(AutoSpanRecyclerView recyclerView) {
        return new GamesAdapter(recyclerView, getBaseContext(), this);
    }

    @Override
    public List<Game> getVisualElements() {
        var response = TwireApplication.helix.getTopGames(null, getCursor(), null, String.valueOf(getLimit())).execute();
        setCursor(response.getPagination().getCursor());
        return response.getGames().stream().map(Game::new).toList();
    }
}
