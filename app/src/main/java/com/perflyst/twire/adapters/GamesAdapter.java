package com.perflyst.twire.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.GameActivity;
import com.perflyst.twire.activities.main.MainActivity;
import com.perflyst.twire.model.Game;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;

/**
 * Created by Sebastian Rask on 11-04-2016.
 */
class GameViewHolder extends MainActivityAdapter.ElementsViewHolder {
    final View sharedPadding;
    private final TextView vGameViewers;
    private final TextView vGameTitle;
    private final ImageView vGamePreview;
    private final CardView vCard;

    GameViewHolder(View v) {
        super(v);
        vGamePreview = v.findViewById(R.id.image_game_preview);
        vGameTitle = v.findViewById(R.id.game_card_title);
        vGameViewers = v.findViewById(R.id.game_viewers);
        vCard = v.findViewById(R.id.cardView_game);
        sharedPadding = v.findViewById(R.id.shared_padding);
    }

    TextView getGameViewers() {
        return vGameViewers;
    }

    TextView getGameTitle() {
        return vGameTitle;
    }

    private ImageView getGamePreview() {
        return vGamePreview;
    }

    public CardView getCard() {
        return vCard;
    }

    @Override
    public ImageView getPreviewView() {
        return getGamePreview();
    }

    @Override
    public CharSequence getTargetsKey() {
        return vGameTitle.getText();
    }

    @Override
    public View getElementWrapper() {
        return getCard();
    }
}

public class GamesAdapter extends MainActivityAdapter<Game, GameViewHolder> {
    private final int regMargin;
    private final int bottomMargin;
    private final double previewAspectRatio;
    private final Activity mActivity;

    public GamesAdapter(AutoSpanRecyclerView recyclerView, Context aContext, @Nullable Activity aActivity) {
        super(recyclerView, aContext);
        regMargin = (int) getContext().getResources().getDimension(R.dimen.game_card_margin);
        bottomMargin = (int) getContext().getResources().getDimension(R.dimen.game_card_bottom_margin);
        previewAspectRatio = 272.0 / 380.0;
        mActivity = aActivity;
    }

    @Override
    protected void adapterSpecial(GameViewHolder viewHolder) {
        viewHolder.getPreviewView().setMinimumHeight((int) (getCardWidth() / previewAspectRatio));
        viewHolder.getPreviewView().forceLayout();
    }

    @Override
    GameViewHolder getElementsViewHolder(View view) {
        return new GameViewHolder(view);
    }

    @Override
    void handleElementOnClick(View view) {
        int itemPosition = getRecyclerView().getChildAdapterPosition(view);
        Game gameClicked = getElements().get(itemPosition);

        Intent intent = new Intent(getContext(), GameActivity.class);
        intent.putExtra(getContext().getResources().getString(R.string.game_intent_key), gameClicked);
        //intent.putExtra(getContext().getResources().getString(R.string.game_intent_image_key), Service.getDrawableByteArray(viewHolder.getGamePreview().getDrawable()));

        if (mActivity == null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        } else if (mActivity instanceof MainActivity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            ((MainActivity) mActivity).transitionToOtherMainActivity(intent);
        } else {
            mActivity.startActivity(intent);
            mActivity.overridePendingTransition(R.anim.slide_in_right_anim, R.anim.fade_out_semi_anim);
        }

    }

    @Override
    void setViewLayoutParams(View view, int position) {
        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(view.getLayoutParams());
        int spanCount = getRecyclerView().getSpanCount();

        // If this card ISN'T the end of a row - Half the right margin
        int rightMargin = ((position + 1) % spanCount != 0)
                ? (int) getContext().getResources().getDimension(R.dimen.game_card_margin_half)
                : regMargin;

        // If the previous card ISN'T the end of a row, this card ISN'T be the start of a row - Half the left margin
        int leftMargin = ((position) % spanCount != 0)
                ? (int) getContext().getResources().getDimension(R.dimen.game_card_margin_half)
                : regMargin;

        int topMargin = position < spanCount ? getTopMargin() : regMargin;

        marginParams.setMargins(
                leftMargin, topMargin,
                rightMargin, bottomMargin
        );

        RelativeLayout.LayoutParams newLayoutParams = new RelativeLayout.LayoutParams(marginParams);
        view.setLayoutParams(newLayoutParams);
    }

    @Override
    void setViewData(Game element, GameViewHolder viewHolder) {
        // Set the data on the holder's views
        String mGameTitle = element.getGameTitle();
        String mGameViewers = Integer.toString(element.getGameViewers());
        viewHolder.getGameTitle().setText(mGameTitle);
        if (element.getGameViewers() == -1) {
            viewHolder.getGameViewers().setVisibility(View.GONE);
            if (getElementStyle().equals(getContext().getString(R.string.card_style_normal)))
                viewHolder.sharedPadding.setVisibility(View.GONE);
        } else {
            viewHolder.getGameViewers().setText(getContext().getString(R.string.game_viewers, mGameViewers));
        }
    }

    @Override
    int getLayoutResource() {
        return R.layout.cell_game;
    }

    @Override
    int getCornerRadiusResource() {
        return R.dimen.game_card_corner_radius;
    }

    @Override
    int getTopMarginResource() {
        return R.dimen.game_card_first_top_margin;
    }

    @Override
    int calculateCardWidth() {
        return getRecyclerView().getElementWidth();
    }

    @Override
    public String initElementStyle() {
        return getSettings().getAppearanceGameStyle();
    }

    @Override
    protected void setExpandedStyle(GameViewHolder viewHolder) {
        viewHolder.getGameTitle().setVisibility(View.VISIBLE);
        viewHolder.getGameViewers().setVisibility(View.VISIBLE);
        viewHolder.sharedPadding.setVisibility(View.VISIBLE);
    }

    @Override
    protected void setNormalStyle(GameViewHolder viewHolder) {
        viewHolder.getGameTitle().setVisibility(View.GONE);
        viewHolder.getGameViewers().setVisibility(View.VISIBLE);
        viewHolder.sharedPadding.setVisibility(View.VISIBLE);
    }

    @Override
    protected void setCollapsedStyle(GameViewHolder viewHolder) {
        viewHolder.getGameTitle().setVisibility(View.GONE);
        viewHolder.getGameViewers().setVisibility(View.GONE);
        viewHolder.sharedPadding.setVisibility(View.GONE);
    }
}
