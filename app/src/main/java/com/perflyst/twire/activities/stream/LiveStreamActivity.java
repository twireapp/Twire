package com.perflyst.twire.activities.stream;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.R;
import com.perflyst.twire.adapters.MentionAdapter;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.StreamInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sebastian Rask on 18-06-2016.
 */
public class LiveStreamActivity extends StreamActivity {

    private final String LOG_TAG = getClass().getSimpleName();
    private RecyclerView mMentionRecyclerView;
    private MentionAdapter mMentionAdapter;
    private View mMentionContainer;

    public static Intent createLiveStreamIntent(StreamInfo stream, boolean sharedTransition, Context context) {
        Intent liveStreamIntent = new Intent(context, LiveStreamActivity.class);
        liveStreamIntent.putExtra(context.getString(R.string.intent_key_streamer_info), stream.getChannelInfo());
        liveStreamIntent.putExtra(context.getString(R.string.intent_key_stream_viewers), stream.getCurrentViewers());
        liveStreamIntent.putExtra(context.getString(R.string.stream_preview_url), stream.getMediumPreview());
        liveStreamIntent.putExtra(context.getString(R.string.stream_shared_transition), sharedTransition);
        return liveStreamIntent;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_stream;
    }

    @Override
    protected int getVideoContainerResource() {
        return R.id.video_fragment_container;
    }

    @Override
    protected Bundle getStreamArguments() {

        Intent intent = getIntent();
        ChannelInfo mChannelInfo = intent.getParcelableExtra(getResources().getString(R.string.intent_key_streamer_info));
        int currentViewers = intent.getIntExtra(getResources().getString(R.string.intent_key_stream_viewers), -1);

        Bundle args = new Bundle();
        args.putParcelable(getString(R.string.stream_fragment_streamerInfo), mChannelInfo);
        args.putInt(getString(R.string.stream_fragment_viewers), currentViewers);
        args.putBoolean(getString(R.string.stream_fragment_autoplay), true);
        return args;
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        if (savedInstance == null) {
            FragmentManager fm = getSupportFragmentManager();

            if (mMentionRecyclerView == null) {
                mMentionContainer = findViewById(R.id.mention_container);
                mMentionContainer.setVisibility(View.GONE);
                mMentionRecyclerView = findViewById(R.id.mention_recyclerview);
                setupMentionSuggestionRecyclerView();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "Live stream activity stopped");
    }

    @Override
    public void onBackPressed() {
        setMentionSuggestions(new ArrayList<>(), null);
        super.onBackPressed();
    }

    @Override
    protected void update(float[] vectors) {
        if (this.mStreamFragment != null && !this.mStreamFragment.chatOnlyViewVisible) {
            super.update(vectors);
        }
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

    private void setupMentionSuggestionRecyclerView() {
        mMentionAdapter = new MentionAdapter(suggestion -> {
            LiveStreamActivity.this.setMentionSuggestions(new ArrayList<>(), null);
            if (mChatFragment == null) {
                return;
            }

            mChatFragment.insertMentionSuggestion(suggestion);
        });
        mMentionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMentionRecyclerView.setAdapter(mMentionAdapter);
    }
}


