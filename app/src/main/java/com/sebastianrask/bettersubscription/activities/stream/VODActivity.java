package com.sebastianrask.bettersubscription.activities.stream;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.activities.ChannelActivity;
import com.sebastianrask.bettersubscription.model.VideoOnDemand;

public class VODActivity extends StreamActivity {
	public static Intent createVODIntent(VideoOnDemand video, Context context) {
		Intent intent = new Intent(context, VODActivity.class);
		intent.putExtra(context.getResources().getString(R.string.intent_vod), video);
		intent.putExtra(context.getString(R.string.stream_shared_transition), true);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		return intent;
	}

	private final String LOG_TAG = getClass().getSimpleName();
	private VideoOnDemand mVod;
	private Fragment vodsFragments;
	private TextView mTitleView, mViewsView;

	@Override
	protected int getLayoutRessource() {
		return R.layout.activity_vod;
	}

	@Override
	protected int getVideoContainerRessource() {
		return R.id.video_fragment_container;
	}

	@Override
	protected Bundle getStreamArguments() {
		if (mVod == null) {
			Intent intent = getIntent();
			mVod = intent.getParcelableExtra(getResources().getString(R.string.intent_vod));
		}

		Bundle args = new Bundle();
		args.putParcelable(getString(R.string.stream_fragment_streamerInfo), mVod.getChannelInfo());
		args.putString(getString(R.string.stream_fragment_vod_id), mVod.getVideoId());
		args.putInt(getString(R.string.stream_fragment_vod_length), mVod.getLength());
		return args;
	}

	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		if (savedInstance == null) {
			FragmentManager fm = getSupportFragmentManager();

			if(vodsFragments == null) {
				vodsFragments = ChannelActivity.VodFragment.newInstance(mVod.isBroadcast(), mVod.getChannelInfo());
				fm.beginTransaction().replace(R.id.additional_vods_container, vodsFragments).commit();
			}
		}

		mTitleView = (TextView) findViewById(R.id.title);
		mViewsView = (TextView) findViewById(R.id.views);

		setVodData();
	}

	private void setVodData() {
		if (mVod != null) {
			mTitleView.setText(mVod.getVideoTitle());
			mViewsView.setText(getString(R.string.vod_views, mVod.getViews()));
		}
	}

	public void startNewVOD(VideoOnDemand videoOnDemand) {
		mVod = videoOnDemand;
		setVodData();
		resetStream();
	}

	@Override
	public View getMainContentLayout() {
		return findViewById(R.id.main_content);
	}
}
