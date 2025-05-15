package com.perflyst.twire.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.perflyst.twire.R
import com.perflyst.twire.activities.GameActivity
import com.perflyst.twire.activities.main.MainActivity
import com.perflyst.twire.adapters.MainActivityAdapter.ElementsViewHolder
import com.perflyst.twire.model.Game
import com.perflyst.twire.service.Settings.appearanceGameStyle
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView

/**
 * Created by Sebastian Rask on 11-04-2016.
 */
class GameViewHolder(v: View) : ElementsViewHolder(v) {
    val sharedPadding: View = v.findViewById(R.id.shared_padding)

    /*
    TextView getGameViewers() {
        return vGameViewers;
    }
     */
    //private final TextView vGameViewers;
    val gameTitle: TextView = v.findViewById(R.id.game_card_title)
    private val gamePreview: ImageView = v.findViewById(R.id.image_game_preview)

    //vGameViewers = v.findViewById(R.id.game_viewers);
    val card: CardView = v.findViewById(R.id.cardView_game)

    override val previewView: ImageView get() = this.gamePreview

    override val targetsKey: CharSequence get() = gameTitle.getText()

    override val elementWrapper: View get() = this.card
}

class GamesAdapter(
    recyclerView: AutoSpanRecyclerView,
    aContext: Context,
    private val mActivity: Activity?
) : MainActivityAdapter<Game, GameViewHolder>(recyclerView, aContext) {
    private val regMargin: Int = context.resources.getDimension(R.dimen.game_card_margin).toInt()
    private val bottomMargin: Int =
        context.resources.getDimension(R.dimen.game_card_bottom_margin).toInt()
    private val previewAspectRatio: Double = 272.0 / 380.0

    override fun adapterSpecial(viewHolder: GameViewHolder) {
        viewHolder.previewView
            .setMinimumHeight((cardWidth / previewAspectRatio).toInt())
        viewHolder.previewView.forceLayout()
    }

    override fun getElementsViewHolder(view: View): GameViewHolder {
        return GameViewHolder(view)
    }

    override fun handleElementOnClick(view: View) {
        val itemPosition = recyclerView.getChildAdapterPosition(view)
        val gameClicked = elements[itemPosition]

        val intent = Intent(context, GameActivity::class.java)
        intent.putExtra(context.getString(R.string.game_intent_key), gameClicked)

        //intent.putExtra(context.getString(R.string.game_intent_image_key), Service.getDrawableByteArray(viewHolder.getGamePreview().getDrawable()));
        when (mActivity) {
            null -> {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }

            is MainActivity<*> -> {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                mActivity.transitionToOtherMainActivity(intent)
            }

            else -> {
                mActivity.startActivity(intent)
                mActivity.overridePendingTransition(
                    R.anim.slide_in_right_anim,
                    R.anim.fade_out_semi_anim
                )
            }
        }
    }

    override fun setViewLayoutParams(view: View, position: Int) {
        val marginParams = MarginLayoutParams(view.layoutParams)
        val spanCount = recyclerView.spanCount

        // If this card ISN'T the end of a row - Half the right margin
        val rightMargin = if ((position + 1) % spanCount != 0) context.resources
            .getDimension(R.dimen.game_card_margin_half).toInt() else
            regMargin

        // If the previous card ISN'T the end of a row, this card ISN'T be the start of a row - Half the left margin
        val leftMargin = if (position % spanCount != 0) context.resources
            .getDimension(R.dimen.game_card_margin_half).toInt() else
            regMargin

        val topMargin = if (position < spanCount) topMarginFirst else regMargin

        marginParams.setMargins(
            leftMargin, topMargin,
            rightMargin, bottomMargin
        )

        val newLayoutParams = RelativeLayout.LayoutParams(marginParams)
        view.setLayoutParams(newLayoutParams)
    }

    override fun setViewData(element: Game, viewHolder: GameViewHolder) {
        // Set the data on the holder's views
        val mGameTitle = element.gameTitle
        //String mGameViewers = Integer.toString(element.getGameViewers());
        viewHolder.gameTitle.text = mGameTitle
        /*
        if (element.getGameViewers() == -1) {
            viewHolder.getGameViewers().setVisibility(View.GONE);
            if (getElementStyle().equals(context.getString(R.string.card_style_normal)))
                viewHolder.sharedPadding.setVisibility(View.GONE);
        } else {
            viewHolder.getGameViewers().setText(context.getString(R.string.game_viewers, mGameViewers));
        }
         */
    }

    override val layoutResource: Int get() = R.layout.cell_game

    override val cornerRadiusResource: Int get() = R.dimen.game_card_corner_radius

    override val topMarginResource: Int get() = R.dimen.game_card_first_top_margin

    override fun calculateCardWidth(): Int {
        return recyclerView.elementWidth
    }

    override fun compareTo(element: Game, other: Game): Int {
        return element.compareTo(other)
    }

    override fun getPreviewTemplate(element: Game): String? {
        return element.previewTemplate
    }

    override val width: String get() = "300"

    override val height: String get() = "400"

    override fun getPlaceHolder(element: Game, context: Context?): Int {
        return R.drawable.template_game
    }

    override fun initElementStyle(): String {
        return appearanceGameStyle
    }

    override fun setExpandedStyle(viewHolder: GameViewHolder) {
        viewHolder.gameTitle.visibility = View.VISIBLE
        //viewHolder.getGameViewers().setVisibility(View.VISIBLE);
        viewHolder.sharedPadding.visibility = View.VISIBLE
    }

    override fun setNormalStyle(viewHolder: GameViewHolder) {
        viewHolder.gameTitle.visibility = View.GONE
        //viewHolder.getGameViewers().setVisibility(View.VISIBLE);
        viewHolder.sharedPadding.visibility = View.VISIBLE
    }

    override fun setCollapsedStyle(viewHolder: GameViewHolder) {
        viewHolder.gameTitle.visibility = View.GONE
        //viewHolder.getGameViewers().setVisibility(View.GONE);
        viewHolder.sharedPadding.visibility = View.GONE
    }
}
