package com.perflyst.twire.activities.main;

import android.util.Log;

import com.perflyst.twire.R;
import com.perflyst.twire.adapters.GamesAdapter;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.model.Game;
import com.perflyst.twire.service.JSONService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.GameAutoSpanBehaviour;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that loads and shows the user's followed Games from Twitch
 */
public class MyGamesActivity extends LazyMainActivity<Game> {

    @Override
    protected int getActivityIconRes() {
        return R.drawable.ic_my_games;
    }

    @Override
    protected int getActivityTitleRes() {
        return R.string.my_games_activity_title;
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
    protected MainActivityAdapter constructAdapter(AutoSpanRecyclerView recyclerView) {
        return new GamesAdapter(mRecyclerView, getBaseContext(), this);
    }

    /**
     * Methods for functionality and for controlling the SwipeRefreshLayout
     */

    @Override
    public void addToAdapter(List<Game> aGamesList) {
        mOnScrollListener.checkForNewElements(mRecyclerView);
        mAdapter.addList(aGamesList);
        Log.i(LOG_TAG, "Adding My Games: " + aGamesList.size());
    }

    @Override
    public List<Game> getVisualElements() throws JSONException {
        List<Game> resultList = new ArrayList<>();

        //Indentation is meant to mimic the structure of the JSON code
        final String URL = "https://api.twitch.tv/api/users/" + new Settings(getBaseContext()).getGeneralTwitchName() + "/follows/games/live?limit=" + getLimit() + "&offset=" + getCurrentOffset();
        final String TOTAL_NUMBER_OF_GAMES = "_total";
        final String GAMES_ARRAY_KEY = "follows";
        final String VIEWERS_INTEGER_KEY = "viewers";
        final String CHANNELS_INTEGER_KEY = "channels";
        final String GAME_OBJECT_KEY = "game";


        String jsonString = Service.urlToJSONString(URL);
        JSONObject fullDataObject = new JSONObject(jsonString);
        JSONArray gamesArray = fullDataObject.getJSONArray(GAMES_ARRAY_KEY);

        setMaxElementsToFetch(fullDataObject.getInt(TOTAL_NUMBER_OF_GAMES));
        shouldShowErrorView();
        for (int i = 0; i < gamesArray.length(); i++) {
            // Get all the JSON objects we need to get all the required data.
            JSONObject topObject = gamesArray.getJSONObject(i);
            JSONObject gameObject = topObject.getJSONObject(GAME_OBJECT_KEY);

            // Get all the data with the keys
            int viewers = topObject.getInt(VIEWERS_INTEGER_KEY);
            int channels = topObject.getInt(CHANNELS_INTEGER_KEY);

            Game game = JSONService.getGame(gameObject);
            game.setGameViewers(viewers);
            game.setGameStreamers(channels);

            // Add a new Game object to the result list.
            resultList.add(game);
        }

        return resultList;
    }
}
