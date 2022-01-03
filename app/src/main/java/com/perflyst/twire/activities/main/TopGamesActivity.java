package com.perflyst.twire.activities.main;

import android.util.Log;

import com.perflyst.twire.R;
import com.perflyst.twire.adapters.GamesAdapter;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.model.Game;
import com.perflyst.twire.service.JSONService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.GameAutoSpanBehaviour;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
        mOnScrollListener.checkForNewElements(mRecyclerView);
        mAdapter.addList(aGamesList);
        Log.i(LOG_TAG, "Adding Top Games: " + aGamesList.size());
    }

    @Override
    protected MainActivityAdapter<Game, ?> constructAdapter(AutoSpanRecyclerView recyclerView) {
        return new GamesAdapter(recyclerView, getBaseContext(), this);
    }

    public String pagination = "";

    @Override
    public List<Game> getVisualElements() throws JSONException {
        List<Game> resultList = new ArrayList<>();

        //Indentation is meant to mimic the structure of the JSON code
        final String URL = "https://api.twitch.tv/helix/games/top?first=" + getLimit() + (pagination != "" ? "&after=" + pagination : "");
        String jsonString = Service.urlToJSONStringHelix(URL, this);
        JSONObject fullDataObject = new JSONObject(jsonString);
        JSONArray gamesArray = fullDataObject.getJSONArray("data");
        this.pagination = fullDataObject.getJSONObject("pagination").getString("cursor");

        for (int i = 0; i < gamesArray.length(); i++) {
            // Get all the JSON objects we need to get all the required data.
            JSONObject topObject = gamesArray.getJSONObject(i);

            // Get all the data with the keys
            int viewers = 0;
            int channels = 0;

            Game game = JSONService.getGame(topObject);
            game.setGameViewers(viewers);
            game.setGameStreamers(channels);

            // Add a new Game object to the result list.
            resultList.add(game);
        }

        return resultList;
    }
}
