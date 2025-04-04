package com.perflyst.twire.adapters;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.SharedElementCallback;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.ChannelActivity;
import com.perflyst.twire.misc.PreviewTarget;
import com.perflyst.twire.misc.RoundImageAnimation;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.service.SubscriptionsDbHelper;
import com.perflyst.twire.utils.Execute;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;

import java.util.List;

/**
 * Created by Sebastian Rask on 04-04-2016.
 */

class StreamerInfoViewHolder extends MainActivityAdapter.ElementsViewHolder {
    final TextView vDisplayName;
    private final ImageView vProfileLogoImage;
    private final CardView vCard;

    StreamerInfoViewHolder(View v) {
        super(v);
        vDisplayName = v.findViewById(R.id.displayName);
        vProfileLogoImage = v.findViewById(R.id.profileLogoImageView);
        vCard = v.findViewById(R.id.card_view);
    }

    @Override
    public ImageView getPreviewView() {
        return vProfileLogoImage;
    }

    @Override
    public CharSequence getTargetsKey() {
        return vDisplayName.getText();
    }

    @Override
    public View getElementWrapper() {
        return vCard;
    }
}

public class ChannelsAdapter extends MainActivityAdapter<ChannelInfo, StreamerInfoViewHolder> {
    private final int regMargin;
    private final Activity activity;

    public ChannelsAdapter(AutoSpanRecyclerView recyclerView, Context aContext, Activity aActivity) {
        super(recyclerView, aContext);
        regMargin = (int) getContext().getResources().getDimension(R.dimen.subscription_card_margin);
        activity = aActivity;
    }

    @Override
    StreamerInfoViewHolder getElementsViewHolder(View view) {
        return new StreamerInfoViewHolder(view);
    }

    @Override
    void handleElementOnClick(final View view) {
        int itemPosition = getRecyclerView().getChildAdapterPosition(view);
        final ChannelInfo item = getElements().get(itemPosition);
        final StreamerInfoViewHolder vh = (StreamerInfoViewHolder) getRecyclerView().getChildViewHolder(view);
        final PreviewTarget previewTarget = getTargets().get(vh.getTargetsKey());

        // Create intent for opening StreamerInfo activity. Send the StreamerInfo object with
        // the intent, and flag it to make sure it creates a new task on the history stack
        final Intent intent = new Intent(getContext(), ChannelActivity.class);
        intent.putExtra(getContext().getString(R.string.channel_info_intent_object), item);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        View sharedView = view.findViewById(R.id.profileLogoImageView);
        sharedView.setTransitionName(getContext().getString(R.string.streamerInfo_transition));
        final ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                activity, sharedView, getContext().getString(R.string.streamerInfo_transition));

        activity.setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
                super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots);

                if (!sharedElements.isEmpty() && sharedElements.get(0) != null && previewTarget != null) {
                    View element = sharedElements.get(0);
                    Animation anim = new RoundImageAnimation(element.getWidth() / 2, 0, (ImageView) element, previewTarget.getPreview());
                    anim.setDuration(200);
                    anim.setInterpolator(new DecelerateInterpolator());
                    view.startAnimation(anim);
                }
                activity.setExitSharedElementCallback(null);
            }
        });
        activity.startActivity(intent, options.toBundle());
    }

    @Override
    void setViewLayoutParams(View view, int position) {
        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(view.getLayoutParams());
        int spanCount = getRecyclerView().getSpanCount();

        // If this card ISN'T the end of a row - Half the right margin
        int rightMargin = (position + 1) % spanCount != 0
                ? (int) getContext().getResources().getDimension(R.dimen.subscription_card_margin_half)
                : regMargin;

        // If the previous card ISN'T the end of a row, this card ISN'T be the start of a row - Half the left margin
        int leftMargin = position % spanCount != 0
                ? (int) getContext().getResources().getDimension(R.dimen.subscription_card_margin_half)
                : regMargin;

        int topMargin = position < spanCount ? getTopMargin() : 0;

        marginParams.setMargins(leftMargin, topMargin, rightMargin, regMargin);

        view.setLayoutParams(new RelativeLayout.LayoutParams(marginParams));
    }

    @Override
    void setViewData(ChannelInfo element, StreamerInfoViewHolder viewHolder) {
        viewHolder.vDisplayName.setText(element.getDisplayName());
        viewHolder.vDisplayName.forceLayout();
    }

    @Override
    int getLayoutResource() {
        return R.layout.cell_channel;
    }

    @Override
    int getCornerRadiusResource() {
        return R.dimen.subscription_card_corner_radius;
    }

    @Override
    int getTopMarginResource() {
        return R.dimen.subscription_card_first_top_margin;
    }

    @Override
    int calculateCardWidth() {
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int spanCount = screenWidth / ((int) getContext().getResources().getDimension(R.dimen.subscription_card_width) + (int) getContext().getResources().getDimension(R.dimen.subscription_card_margin));
        return (int) (screenWidth / (double) spanCount) - (int) (getContext().getResources().getDimension(R.dimen.subscription_card_margin) * 2) - (int) (getContext().getResources().getDimension(R.dimen.subscription_card_elevation) * 2);
    }

    @Override
    int compareTo(ChannelInfo element, ChannelInfo other) {
        return element.compareTo(other);
    }

    @Override
    String getPreviewTemplate(ChannelInfo element) {
        if (element.logoURL == null) return null;
        return element.logoURL.toString();
    }

    @Override
    int getPlaceHolder(ChannelInfo element, Context context) {
        return R.drawable.ic_profile_template_300p;
    }

    @Override
    void refreshPreview(ChannelInfo element, Context context, Runnable callback) {
        Execute.background(() -> {
            var mChannelInfo = Service.getStreamerInfoFromUserId(element.getUserId());
            if (mChannelInfo != null && element.logoURL != mChannelInfo.logoURL && mChannelInfo.logoURL != null) {
                element.logoURL = mChannelInfo.logoURL;
                Execute.ui(callback);

                var values = new ContentValues();
                values.put(SubscriptionsDbHelper.COLUMN_LOGO_URL, element.logoURL.toString());
                Service.updateStreamerInfoDbWithValues(
                        values,
                        context,
                        element.getUserId()
                );
            }
        });
    }

    @Override
    public String initElementStyle() {
        return Settings.getAppearanceChannelStyle();
    }

    @Override
    protected void setExpandedStyle(StreamerInfoViewHolder viewHolder) {
        // This is not support for Follow cards
    }

    @Override
    protected void setNormalStyle(StreamerInfoViewHolder viewHolder) {
        viewHolder.vDisplayName.setVisibility(View.VISIBLE);
    }

    @Override
    protected void setCollapsedStyle(StreamerInfoViewHolder viewHolder) {
        viewHolder.vDisplayName.setVisibility(View.GONE);
    }
}
