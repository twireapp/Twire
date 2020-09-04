package com.perflyst.twire.activities.stream;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.ChannelActivity;
import com.perflyst.twire.model.VideoOnDemand;

public class VODActivity extends StreamActivity {
    private VideoOnDemand mVod;
    private Fragment vodsFragments;
    private TextView mTitleView, mViewsView;

    public static Intent createVODIntent(VideoOnDemand video, Context context, boolean transition) {
        Intent intent = new Intent(context, VODActivity.class);
        intent.putExtra(context.getResources().getString(R.string.intent_vod), video);
        intent.putExtra(context.getString(R.string.stream_shared_transition), transition);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_vod;
    }

    @Override
    protected int getVideoContainerResource() {
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

            if (vodsFragments == null) {
                vodsFragments = ChannelActivity.VodFragment.newInstance(mVod.isBroadcast(), mVod.getChannelInfo());
                fm.beginTransaction().replace(R.id.additional_vods_container, vodsFragments).commit();
            }
        }

        mTitleView = findViewById(R.id.title);
        mViewsView = findViewById(R.id.views);

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
}
