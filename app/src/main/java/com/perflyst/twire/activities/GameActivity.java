package com.perflyst.twire.activities;

import android.content.Intent;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.main.LazyMainActivity;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.adapters.StreamsAdapter;
import com.perflyst.twire.model.Game;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.service.JSONService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.StreamAutoSpanBehaviour;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static com.perflyst.twire.misc.Utils.getSystemLanguage;

public class GameActivity extends LazyMainActivity<StreamInfo> {
    private Game game;

    @Override
    protected MainActivityAdapter<StreamInfo, ?> constructAdapter(AutoSpanRecyclerView recyclerView) {
        return new StreamsAdapter(recyclerView, this);
    }

    @Override
    public void customizeActivity() {
        Intent intent = getIntent();
        game = intent.getParcelableExtra(getResources().getString(R.string.game_intent_key));
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
        mOnScrollListener.checkForNewElements(mRecyclerView);
        mAdapter.addList(streamsToAdd);
    }

    @Override
    public List<StreamInfo> getVisualElements() throws JSONException, MalformedURLException, UnsupportedEncodingException {
        String gameTitleEncoded = URLEncoder.encode(game.getGameTitle(), "UTF-8");
        String languageFilter = settings.getGeneralFilterTopStreamsByLanguage() ? "&language=" + getSystemLanguage() : "";

        String url = "https://api.twitch.tv/kraken/streams?game=" + gameTitleEncoded + "&limit="
                + getLimit() + "&offset=" + getCurrentOffset() + languageFilter;
        final String GAMES_ARRAY_KEY = "streams";

        List<StreamInfo> mResultList = new ArrayList<>();
        String jsonString = Service.urlToJSONString(url);
        JSONObject fullDataObject = new JSONObject(jsonString);
        JSONArray topStreamsArray = fullDataObject.getJSONArray(GAMES_ARRAY_KEY);

        for (int i = 0; i < topStreamsArray.length(); i++) {
            JSONObject streamObject = topStreamsArray.getJSONObject(i);
            mResultList.add(JSONService.getStreamInfo(getBaseContext(), streamObject, null, false));
        }

        return mResultList;
    }
}
