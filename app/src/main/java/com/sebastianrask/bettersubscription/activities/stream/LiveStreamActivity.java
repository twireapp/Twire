package com.sebastianrask.bettersubscription.activities.stream;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.adapters.MentionAdapter;
import com.sebastianrask.bettersubscription.fragments.ChatFragment;
import com.sebastianrask.bettersubscription.fragments.StreamFragment;
import com.sebastianrask.bettersubscription.model.ChannelInfo;
import com.sebastianrask.bettersubscription.model.StreamInfo;
import com.sebastianrask.bettersubscription.service.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sebastian Rask on 18-06-2016.
 */
public class LiveStreamActivity extends StreamActivity {

	public static Intent createLiveStreamIntent(StreamInfo stream, boolean sharedTransition, Context context) {
		Intent liveStreamIntent = new Intent(context, LiveStreamActivity.class);
		liveStreamIntent.putExtra(context.getString(R.string.intent_key_streamer_info), stream.getChannelInfo());
		liveStreamIntent.putExtra(context.getString(R.string.intent_key_stream_viewers), stream.getCurrentViewers());
		liveStreamIntent.putExtra(context.getString(R.string.stream_preview_url), stream.getMediumPreview());
		liveStreamIntent.putExtra(context.getString(R.string.stream_shared_transition), sharedTransition);
		return liveStreamIntent;
	}

	private String LOG_TAG = getClass().getSimpleName();
	private ChatFragment mChatFragment;
	private RecyclerView mMentionRecyclerView;
	private Settings settings;
	private MentionAdapter mMentionAdapter;
	private View mMentionContainer;

	@Override
	protected int getLayoutRessource() {
		return R.layout.activity_stream;
	}

	@Override
	protected int getVideoContainerRessource() {
		return R.id.video_fragment_container;
	}

	@Override
	protected Bundle getStreamArguments() {
		boolean autoPlay = true;

		Intent intent = getIntent();
		ChannelInfo mChannelInfo = intent.getParcelableExtra(getResources().getString(R.string.intent_key_streamer_info));
		int currentViewers = intent.getIntExtra(getResources().getString(R.string.intent_key_stream_viewers), -1);

		if (mChannelInfo == null) {
			try {
				VideoCastManager mCastManager = VideoCastManager.getInstance();
				mCastManager.reconnectSessionIfPossible(5);

				MediaInfo mediaInfo = mCastManager.getRemoteMediaInformation();
				if (mediaInfo != null) {
					MediaMetadata metadata = mediaInfo.getMetadata();
					mChannelInfo = new Gson().fromJson(metadata.getString(getString(R.string.stream_fragment_streamerInfo)), new TypeToken<ChannelInfo>() {
					}.getType());
					autoPlay = false;
				}

			} catch (TransientNetworkDisconnectionException | NoConnectionException e) {
				e.printStackTrace();
			}
		}

		Bundle args = new Bundle();
		args.putParcelable(getString(R.string.stream_fragment_streamerInfo), mChannelInfo);
		args.putInt(getString(R.string.stream_fragment_viewers), currentViewers);
		args.putBoolean(getString(R.string.stream_fragment_autoplay), autoPlay);
		return args;
	}

	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		if (savedInstance == null) {
			FragmentManager fm = getSupportFragmentManager();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				getWindow().setEnterTransition(constructTransitions());
				getWindow().setReturnTransition(constructTransitions());
			}

			if (mChatFragment == null) {
				mChatFragment = ChatFragment.getInstance(getStreamArguments());
				fm.beginTransaction().replace(R.id.chat_fragment, mChatFragment).commit();
			}

			if (mMentionRecyclerView == null) {
				mMentionContainer = findViewById(R.id.mention_container);
				mMentionContainer.setVisibility(View.GONE);
				mMentionRecyclerView = (RecyclerView) findViewById(R.id.mention_recyclerview);
				setupMentionSuggestionRecyclerView();
			}
		}

		settings = new Settings(this);
		checkCreateLandscapeChat();
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(LOG_TAG, "Live stream activity stopped");
	}

	@Override
	public void onBackPressed() {
		setMentionSuggestions(new ArrayList<String>(), null);
		if (mChatFragment == null || (mChatFragment.notifyBackPressed())) {
			super.onBackPressed();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		checkCreateLandscapeChat();
	}

	@Override
	protected void update(float[] vectors) {
		if (this.mStreamFragment != null && !this.mStreamFragment.chatOnlyViewVisible) {
			super.update(vectors);
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private TransitionSet constructTransitions() {
		int[] slideTargets = {R.id.ChatRecyclerView, R.id.chat_input, R.id.chat_input_divider};

		Transition slideTransition = new Slide(Gravity.BOTTOM);
		Transition fadeTransition = new Fade();

		for (int slideTarget : slideTargets) {
			slideTransition.addTarget(slideTarget);
			fadeTransition.excludeTarget(slideTarget, true);
		}

		TransitionSet set = new TransitionSet();
		set.addTransition(slideTransition);
		set.addTransition(fadeTransition);
		return set;
	}

	public void setMentionSuggestions(List<String> mentionSuggestions, @Nullable final Rect inputRect) {
		if (mMentionAdapter == null) {
			return;
		}

		mMentionAdapter.setSuggestions(mentionSuggestions);

		if (inputRect == null) {
			return;
		}

		if (mentionSuggestions.isEmpty()) {
			mMentionContainer.setVisibility(View.GONE);
		} else {
			mMentionContainer.setVisibility(View.VISIBLE);
		}

		mMentionContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				mMentionContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				//ToDo: Check height of container and adjust if necessary
				float maxHeight = getResources().getDimension(R.dimen.chat_mention_suggestions_max_height);
				float currentHeight = mMentionContainer.getHeight();

				/*
				if (maxHeight < currentHeight) {
					mMentionContainer.setLayoutParams(new RelativeLayout.LayoutParams(
							mMentionContainer.getLayoutParams().width,
							(int) maxHeight
					));

					currentHeight = maxHeight;
				}
				*/

				mMentionContainer.setY(inputRect.top - inputRect.height() - (int) currentHeight);
			}
		});
	}

	private void checkCreateLandscapeChat() {
		int orientation = getResources().getConfiguration().orientation;
		View chat = findViewById(R.id.chat_fragment);
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) findViewById(R.id.chat_landscape_fragment).getLayoutParams();
			lp.width = (int) (StreamFragment.getScreenWidth(this) * (settings.getChatLandscapeWidth() / 100.0));
			Log.d(LOG_TAG, "TARGET WIDTH: " + lp.width);
			chat.setLayoutParams(lp);
		} else {
			chat.setLayoutParams(findViewById(R.id.chat_placement_wrapper).getLayoutParams());
		}
	}

	private void setupMentionSuggestionRecyclerView() {
		mMentionAdapter = new MentionAdapter(new MentionAdapter.MentionAdapterDelegate() {
			@Override
			public void onSuggestionClick(String suggestion) {
				LiveStreamActivity.this.setMentionSuggestions(new ArrayList<String>(), null);
				if (mChatFragment == null) {
					return;
				}

				mChatFragment.insertMentionSuggestion(suggestion);
			}
		});
		mMentionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		mMentionRecyclerView.setAdapter(mMentionAdapter);
	}
}


