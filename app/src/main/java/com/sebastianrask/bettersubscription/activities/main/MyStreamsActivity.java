package com.sebastianrask.bettersubscription.activities.main;

import android.util.Log;

import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.adapters.MainActivityAdapter;
import com.sebastianrask.bettersubscription.adapters.StreamsAdapter;
import com.sebastianrask.bettersubscription.model.StreamInfo;
import com.sebastianrask.bettersubscription.service.JSONService;
import com.sebastianrask.bettersubscription.service.Service;
import com.sebastianrask.bettersubscription.service.Settings;
import com.sebastianrask.bettersubscription.views.recyclerviews.AutoSpanRecyclerView;
import com.sebastianrask.bettersubscription.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.sebastianrask.bettersubscription.views.recyclerviews.auto_span_behaviours.StreamAutoSpanBehaviour;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class MyStreamsActivity extends LazyMainActivity<StreamInfo> {

	@Override
	protected int getActivityIconRes() {
		return R.drawable.ic_my_streams;
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
	protected MainActivityAdapter constructAdapter(AutoSpanRecyclerView recyclerView) {
		return new StreamsAdapter(recyclerView, this);
	}

	@Override
	public void addToAdapter(List<StreamInfo> aObjectList) {
		mOnScrollListener.checkForNewElements(mRecyclerView);
		mAdapter.addList(aObjectList);
		Log.i(LOG_TAG, "Adding Top Streams: " + aObjectList.size());
	}

	@Override
	public List<StreamInfo> getVisualElements() throws JSONException, MalformedURLException {
		final String URL = "https://api.twitch.tv/kraken/streams/followed?oauth_token=" + new Settings(getBaseContext()).getGeneralTwitchAccessToken() + "&limit="+ getLimit() + "&offset=" + getCurrentOffset() + "&stream_type=live";
		final String ARRAY_KEY = "streams";
		final String TOTAL_STREAMS_INT = "_total";

		List<StreamInfo> mResultList = new ArrayList<>();
		String jsonString = Service.urlToJSONString(URL);
		JSONObject fullDataObject = new JSONObject(jsonString);
		JSONArray topStreamsArray = fullDataObject.getJSONArray(ARRAY_KEY);

		Log.d(LOG_TAG, "Total elements: " + fullDataObject.getInt(TOTAL_STREAMS_INT));
		setMaxElementsToFetch(fullDataObject.getInt(TOTAL_STREAMS_INT));
		shouldShowErrorView();

		for (int i = 0; i < topStreamsArray.length(); i++) {
			JSONObject streamObject = topStreamsArray.getJSONObject(i);
			mResultList.add(JSONService.getStreamInfo(getBaseContext(), streamObject, null, false));
		}

		return mResultList;
	}
}

