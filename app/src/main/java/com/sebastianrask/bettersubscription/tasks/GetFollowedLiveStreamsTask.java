package com.sebastianrask.bettersubscription.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.sebastianrask.bettersubscription.model.StreamInfo;
import com.sebastianrask.bettersubscription.service.Settings;
import com.sebastianrask.bettersubscription.service.StreamsService;

import java.util.List;

/**
 * Created by Sebastian Rask on 17-05-2017.
 */

/***
 * This task will fetch all currently live streams for the logged in user.
 * This task should not be executed for time critical task.
 */
public class GetFollowedLiveStreamsTask extends AsyncTask<Void, Void, List<StreamInfo>> {
	private Context context;
	private FetchLiveStreamsCallback callback;

	public GetFollowedLiveStreamsTask(Context context, FetchLiveStreamsCallback callback) {
		this.context = context;
		this.callback = callback;
	}

	@Override
	protected List<StreamInfo> doInBackground(Void... voids) {
		Settings settings = new Settings(context);
		return StreamsService.fetchAllLiveStreams(context, settings.getGeneralTwitchAccessToken());
	}

	@Override
	protected void onPostExecute(List<StreamInfo> streams) {
		super.onPostExecute(streams);
		callback.onLiveStreamFetched(streams);
	}

	public interface FetchLiveStreamsCallback {
		void onLiveStreamFetched(List<StreamInfo> streams);
	}
}
