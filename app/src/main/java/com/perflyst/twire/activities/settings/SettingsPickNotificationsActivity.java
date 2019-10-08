package com.perflyst.twire.activities.settings;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.FollowingFetcher;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.tasks.GetFollowsFromDB;

import java.util.ArrayList;
import java.util.List;

public class SettingsPickNotificationsActivity extends ThemeActivity implements FollowingFetcher {
    private PickStreamersAdapter mAdapter;
    private View mErrorText;
    private TextView mErrorEmoji;
    private MenuItem toggleMenuItem;
    private Boolean showEnableAll;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_pick_notifications);

        mAdapter = new PickStreamersAdapter();

        mErrorText = findViewById(R.id.error_view);
        mErrorEmoji = findViewById(R.id.emote_error_view);
        RecyclerView mRecyclerView = findViewById(R.id.streamers_recycler_view);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        GetFollowsFromDB subscriptionsTask = new GetFollowsFromDB(this);
        subscriptionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getBaseContext());

        final Toolbar toolbar = findViewById(R.id.settings_choose_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.choose_streamers_activity_settings_notifications_title));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_right_anim);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_toggle_streamers) {
            @StringRes int newTitleRes;
            boolean enable;
            if (item.getTitle().equals(getString(R.string.disable_all))) {
                // Disable all
                enable = false;
                newTitleRes = R.string.enable_all;
            } else {
                //Enable all
                enable = true;
                newTitleRes = R.string.disable_all;
            }

            Service.updateStreamerInfoNotificationSettingForAll(getBaseContext(), enable);
            item.setTitle(getString(newTitleRes));

            for (ChannelInfo channelInfo : mAdapter.mStreamers) {
                channelInfo.setNotifyWhenLive(enable);
            }

            mAdapter.notifyDataSetChanged();
        } else {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings_notifications_choose_streamers, menu);
        toggleMenuItem = menu.findItem(R.id.menu_toggle_streamers);

        if (showEnableAll != null && showEnableAll) {
            toggleMenuItem.setTitle(getString(R.string.enable_all));
        }

        return true;
    }

    // FollowingFetcher
    @Override
    public void addStreamer(ChannelInfo streamer) {
        mAdapter.addStreamer(streamer);
    }

    @Override
    public void addStreamers(List<ChannelInfo> streamers) {
        mAdapter.addStreamers(streamers);
    }

    @Override
    public void showErrorView() {
        mErrorText.setVisibility(View.VISIBLE);
        mErrorEmoji.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean isEmpty() {
        return mAdapter.getItemCount() == 0;
    }

    @Override
    public void notifyFinishedAdding() {
        showEnableAll = true;

        for (ChannelInfo channelInfo : mAdapter.mStreamers) {
            if (channelInfo.isNotifyWhenLive()) {
                showEnableAll = false;
                break;
            }
        }

        if (showEnableAll && toggleMenuItem != null) {
            toggleMenuItem.setTitle(getString(R.string.enable_all));
        }
    }

    private class PickStreamerViewHolder extends RecyclerView.ViewHolder {
        private CheckedTextView checkedTextView;

        PickStreamerViewHolder(View itemView) {
            super(itemView);
            checkedTextView = itemView.findViewById(R.id.streamer_name);
        }
    }

    private class PickStreamersAdapter extends RecyclerView.Adapter<PickStreamerViewHolder> {
        List<ChannelInfo> mStreamers;

        PickStreamersAdapter() {
            mStreamers = new ArrayList<>();
        }

        @Override
        public PickStreamerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.notification_pick_streamer, parent, false);

            return new PickStreamerViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final PickStreamerViewHolder holder, int position) {
            final ChannelInfo streamer = mStreamers.get(position);

            holder.checkedTextView.setText(streamer.getDisplayName());
            holder.checkedTextView.setChecked(streamer.isNotifyWhenLive());
            holder.itemView.setOnClickListener(v -> {
                streamer.setNotifyWhenLive(!holder.checkedTextView.isChecked());
                holder.checkedTextView.setChecked(!holder.checkedTextView.isChecked());

                Service.updateStreamerInfoNotificationSetting(streamer, getBaseContext());
            });
        }

        @Override
        public int getItemCount() {
            return mStreamers.size();
        }

        void addStreamers(List<ChannelInfo> streamers) {
            mStreamers.addAll(streamers);
            notifyDataSetChanged();
        }

        void addStreamer(ChannelInfo streamer) {
            mStreamers.add(streamer);
            notifyItemInserted(mStreamers.size() - 1);
        }

    }
}
