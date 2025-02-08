package com.perflyst.twire.adapters;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.SharedElementCallback;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.stream.VODActivity;
import com.perflyst.twire.model.VideoOnDemand;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Created by Sebastian Rask on 16-06-2016.
 */

class VODViewHolder extends MainActivityAdapter.ElementsViewHolder {
    final ImageView vPreviewImage;
    final TextView vDisplayName, vTitle, vGame, vTimeStamp;
    final ProgressBar vProgressBar;
    private final CardView vCard;

    VODViewHolder(View v) {
        super(v);
        vCard = v.findViewById(R.id.card_view_vod_stream);
        vPreviewImage = v.findViewById(R.id.image_stream_preview);
        vDisplayName = v.findViewById(R.id.displayName);
        vTitle = v.findViewById(R.id.stream_title);
        vGame = v.findViewById(R.id.stream_game_and_viewers);
        vTimeStamp = v.findViewById(R.id.timestamp);
        vProgressBar = v.findViewById(R.id.progressBar);
    }

    @Override
    public ImageView getPreviewView() {
        return vPreviewImage;
    }

    @Override
    public CharSequence getTargetsKey() {
        return vDisplayName.getText().toString() + vTimeStamp.getText().toString();
    }

    @Override
    public View getElementWrapper() {
        return vCard;
    }
}

public class VODAdapter extends MainActivityAdapter<VideoOnDemand, VODViewHolder> {
    private final float VOD_WATCHED_IMAGE_ALPHA = 0.5f;
    private final int topMargin, bottomMargin;
    private final Activity activity;
    private int rightMargin, leftMargin;
    private boolean showName;

    public VODAdapter(AutoSpanRecyclerView recyclerView, Activity aActivity) {
        super(recyclerView, aActivity);
        activity = aActivity;
        rightMargin = (int) getContext().getResources().getDimension(R.dimen.stream_card_right_margin);
        bottomMargin = (int) getContext().getResources().getDimension(R.dimen.stream_card_bottom_margin);
        topMargin = (int) getContext().getResources().getDimension(R.dimen.stream_card_top_margin);
        leftMargin = (int) getContext().getResources().getDimension(R.dimen.stream_card_left_margin);
        showName = true;
    }

    @Override
    VODViewHolder getElementsViewHolder(View view) {
        return new VODViewHolder(view);
    }

    @Override
    void handleElementOnClick(final View view) {
        final int itemPosition = getRecyclerView().getChildAdapterPosition(view);
        VideoOnDemand item = getElements().get(itemPosition);
        if (activity instanceof VODActivity) {
            activity.getIntent().putExtra(getContext().getString(R.string.stream_shared_transition), false);
            ((VODActivity) activity).startNewVOD(item);
        } else {
            Intent intent = VODActivity.createVODIntent(item, getContext(), true);

            intent.putExtra(getContext().getString(R.string.stream_preview_url), getPreviewUrl(item));
            intent.putExtra(getContext().getString(R.string.stream_preview_alpha), hasVodBeenWatched(item.videoId) ? VOD_WATCHED_IMAGE_ALPHA : 1.0f);

            final View sharedView = view.findViewById(R.id.image_stream_preview);
            sharedView.setTransitionName(getContext().getString(R.string.stream_preview_transition));
            final ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                    activity, sharedView, getContext().getString(R.string.stream_preview_transition));

            activity.setExitSharedElementCallback(new SharedElementCallback() {
                @SuppressLint("NewApi")
                @Override
                public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
                    super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots);

                    notifyItemChanged(itemPosition);
                    activity.setExitSharedElementCallback(null);
                }
            });

            activity.startActivity(intent, options.toBundle());
        }
    }

    @Override
    void setViewLayoutParams(View view, int position) {
        int spanCount = getRecyclerView().getSpanCount();

        // If this card ISN'T the end of a row - Half the right margin
        rightMargin = (position + 1) % spanCount != 0
                ? (int) getContext().getResources().getDimension(R.dimen.stream_card_margin_half)
                : (int) getContext().getResources().getDimension(R.dimen.stream_card_right_margin);

        // If the previous card ISN'T the end of a row, this card ISN'T be the start of a row - Half the left margin
        leftMargin = position % spanCount != 0
                ? (int) getContext().getResources().getDimension(R.dimen.stream_card_margin_half)
                : (int) getContext().getResources().getDimension(R.dimen.stream_card_left_margin);


        // Set the correct margin of the card
        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(view.getLayoutParams());

        if (position < spanCount) { // Give extra top margin to cards in the first row to make sure it doesn't get overlapped by the toolbar
            marginParams.setMargins(leftMargin, getTopMargin(), rightMargin, bottomMargin);
        } else {
            marginParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
        }

        view.setLayoutParams(new RelativeLayout.LayoutParams(marginParams));
    }

    @Override
    void setViewData(VideoOnDemand element, VODViewHolder viewHolder) {
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        viewHolder.vPreviewImage.getLayoutParams().width = metrics.widthPixels;

        String gameAndViewers = getContext().getString(R.string.vod_views, element.views);
        if (!element.gameTitle.isEmpty()) {
            gameAndViewers += " - " + element.gameTitle;
        }
        viewHolder.vTitle.setText(element.videoTitle);
        viewHolder.vGame.setText(gameAndViewers);
        viewHolder.vPreviewImage.setVisibility(View.VISIBLE);
        viewHolder.vDisplayName.setText(element.displayName);
        viewHolder.vTimeStamp.setText(getFormattedLengthAndTime(element));
        if (!showName) {
            viewHolder.vDisplayName.setVisibility(View.GONE);
        }

        if (hasVodBeenWatched(element.videoId)) {
            int vodProgress = Settings.getVodProgress(element.videoId);

            viewHolder.vProgressBar.setVisibility(View.VISIBLE);
            viewHolder.vProgressBar.setPadding(0, 0, 0, 0);

            viewHolder.vPreviewImage.animate().alpha(VOD_WATCHED_IMAGE_ALPHA).setDuration(300).start();
            viewHolder.vProgressBar.setMax((int) element.length);
            ObjectAnimator.ofInt(viewHolder.vProgressBar, "progress", vodProgress).setDuration(300).start();
        } else {
            viewHolder.vProgressBar.setVisibility(View.INVISIBLE);
            viewHolder.vPreviewImage.setAlpha(1f);
        }
    }

    private boolean hasVodBeenWatched(String id) {
        return Settings.getVodProgress(id) > 0;
    }

    private String getFormattedLengthAndTime(VideoOnDemand vod) {
        String time;
        ZonedDateTime now = ZonedDateTime.now(), vodDate = vod.recordedAt;

        long daysAgo = Duration.between(vodDate, now).toDays();
        long milliseconds = vodDate.toInstant().toEpochMilli();
        if (daysAgo <= 0) {
            // today
            time = getContext().getString(R.string.today);
        } else if (daysAgo == 1) {
            // yesterday
            time = getContext().getString(R.string.yesterday);
        } else if (daysAgo <= 7) {
            // a week ago -> show weekday only
            time = DateUtils.formatDateTime(getContext(), milliseconds, DateUtils.FORMAT_SHOW_WEEKDAY);
        } else {
            // if more than a week ago and less than a year -> show day and month only
            // if over a year ago -> show full date
            time = DateUtils.formatDateTime(getContext(), milliseconds, DateUtils.FORMAT_SHOW_DATE);
        }

        return time + " - " + DateUtils.formatElapsedTime(vod.length);
    }

    @Override
    int getLayoutResource() {
        return R.layout.cell_vod;
    }

    @Override
    int getCornerRadiusResource() {
        return R.dimen.stream_card_corner_radius;
    }

    @Override
    int getTopMarginResource() {
        return R.dimen.stream_card_first_top_margin;
    }

    @Override
    int calculateCardWidth() {
        return getRecyclerView().getElementWidth();
    }

    @Override
    int compareTo(VideoOnDemand element, VideoOnDemand other) {
        return element.compareTo(other);
    }

    @Override
    String getPreviewTemplate(VideoOnDemand element) {
        return element.previewTemplate;
    }

    @Override
    int getPlaceHolder(VideoOnDemand element, Context context) {
        return R.drawable.template_stream;
    }

    @Override
    public String initElementStyle() {
        return Settings.getAppearanceStreamStyle();
    }

    @Override
    protected void setExpandedStyle(VODViewHolder viewHolder) {
/*
        viewHolder.vTitle.setVisibility(View.VISIBLE);
        viewHolder.vGame.setVisibility(View.VISIBLE);
        viewHolder.sharedPadding.setVisibility(View.VISIBLE);
*/
    }

    @Override
    protected void setNormalStyle(VODViewHolder viewHolder) {
/*
        viewHolder.vTitle.setVisibility(View.GONE);
        viewHolder.vGame.setVisibility(View.VISIBLE);
        viewHolder.sharedPadding.setVisibility(View.VISIBLE);
*/
    }

    @Override
    protected void setCollapsedStyle(VODViewHolder viewHolder) {
/*
        viewHolder.vTitle.setVisibility(View.GONE);
        viewHolder.vGame.setVisibility(View.GONE);
        viewHolder.sharedPadding.setVisibility(View.GONE);
*/
    }

    public void setShowName(boolean showName) {
        this.showName = showName;
    }
}
