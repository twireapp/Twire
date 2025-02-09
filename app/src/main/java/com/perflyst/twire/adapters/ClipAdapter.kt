package com.perflyst.twire.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.SharedElementCallback
import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.github.twitch4j.helix.domain.Clip
import com.perflyst.twire.R
import com.perflyst.twire.activities.stream.ClipActivity
import com.perflyst.twire.activities.stream.ClipActivity.Companion.createClipIntent
import com.perflyst.twire.adapters.MainActivityAdapter.ElementsViewHolder
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.appearanceStreamStyle
import com.perflyst.twire.utils.Execute
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Created by Sebastian Rask on 16-06-2016.
 */
class ClipViewHolder(v: View) : ElementsViewHolder(v) {
    val vPreviewImage: ImageView = v.findViewById(R.id.image_stream_preview)
    val vDisplayName: TextView = v.findViewById(R.id.displayName)
    val vTitle: TextView = v.findViewById(R.id.stream_title)
    val vGame: TextView = v.findViewById(R.id.stream_game_and_viewers)
    val vTimeStamp: TextView = v.findViewById(R.id.timestamp)
    private val vCard: CardView = v.findViewById(R.id.card_view_vod_stream)

    override fun getPreviewView(): ImageView {
        return vPreviewImage
    }

    override fun getTargetsKey(): CharSequence {
        return vDisplayName.text.toString() + vTimeStamp.text.toString()
    }

    override fun getElementWrapper(): View {
        return vCard
    }
}

class ClipAdapter(recyclerView: AutoSpanRecyclerView, private val activity: Activity) :
    MainActivityAdapter<Clip, ClipViewHolder>(
        recyclerView,
        activity
    ) {
    private val topMargin: Int
    private val bottomMargin: Int
    private var rightMargin: Int
    private var leftMargin: Int
    private var showName = true

    init {
        rightMargin = context.resources.getDimension(R.dimen.stream_card_right_margin).toInt()
        bottomMargin = context.resources.getDimension(R.dimen.stream_card_bottom_margin).toInt()
        topMargin = context.resources.getDimension(R.dimen.stream_card_top_margin).toInt()
        leftMargin = context.resources.getDimension(R.dimen.stream_card_left_margin).toInt()
    }

    override fun getElementsViewHolder(view: View): ClipViewHolder {
        return ClipViewHolder(view)
    }

    override fun handleElementOnClick(view: View) {
        val itemPosition = recyclerView.getChildAdapterPosition(view)
        val item = elements[itemPosition]!!
        if (activity is ClipActivity) {
            activity.getIntent()
                .putExtra(context.getString(R.string.stream_shared_transition), false)
            activity.startNewClip(item)
        } else {
            val intent = createClipIntent(
                item, Service.getStreamerInfoFromUserId(item.broadcasterId),
                context, true
            )

            intent.putExtra(context.getString(R.string.stream_preview_url), item.thumbnailUrl)

            val sharedView = view.findViewById<View>(R.id.image_stream_preview)
            sharedView.transitionName = context.getString(R.string.stream_preview_transition)
            val options = ActivityOptions.makeSceneTransitionAnimation(
                activity, sharedView, context.getString(R.string.stream_preview_transition)
            )

            activity.setExitSharedElementCallback(object : SharedElementCallback() {
                @SuppressLint("NewApi")
                override fun onSharedElementEnd(
                    sharedElementNames: List<String>,
                    sharedElements: List<View>,
                    sharedElementSnapshots: List<View>
                ) {
                    super.onSharedElementEnd(
                        sharedElementNames,
                        sharedElements,
                        sharedElementSnapshots
                    )

                    notifyItemChanged(itemPosition)
                    activity.setExitSharedElementCallback(null)
                }
            })

            activity.startActivity(intent, options.toBundle())
        }
    }

    override fun setViewLayoutParams(view: View, position: Int) {
        val spanCount = recyclerView.spanCount

        // If this card ISN'T the end of a row - Half the right margin
        rightMargin =
            if ((position + 1) % spanCount != 0) context.resources.getDimension(R.dimen.stream_card_margin_half)
                .toInt() else context.resources.getDimension(R.dimen.stream_card_right_margin)
                .toInt()

        // If the previous card ISN'T the end of a row, this card ISN'T be the start of a row - Half the left margin
        leftMargin =
            if (position % spanCount != 0) context.resources.getDimension(R.dimen.stream_card_margin_half)
                .toInt() else context.resources.getDimension(R.dimen.stream_card_left_margin)
                .toInt()


        // Set the correct margin of the card
        val marginParams = MarginLayoutParams(view.layoutParams)

        if (position < spanCount) { // Give extra top margin to cards in the first row to make sure it doesn't get overlapped by the toolbar
            marginParams.setMargins(leftMargin, getTopMargin(), rightMargin, bottomMargin)
        } else {
            marginParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin)
        }

        view.layoutParams = RelativeLayout.LayoutParams(marginParams)
    }

    override fun setViewData(element: Clip, viewHolder: ClipViewHolder) {
        val metrics = context.resources.displayMetrics
        viewHolder.vPreviewImage.layoutParams.width = metrics.widthPixels

        val gameAndViewers = context.getString(R.string.vod_views, element.viewCount)
        if (element.gameId.isNotEmpty()) {
            Execute.background({ Service.gameNameCache.get(element.gameId) }, { name ->
                viewHolder.vGame.text = "${viewHolder.vGame.text} - $name"
            })
        }
        viewHolder.vTitle.text = element.title
        viewHolder.vGame.text = gameAndViewers
        viewHolder.vPreviewImage.visibility = View.VISIBLE
        viewHolder.vDisplayName.text = element.broadcasterName
        viewHolder.vTimeStamp.text = getFormattedLengthAndTime(element)
        if (!showName) {
            viewHolder.vDisplayName.visibility = View.GONE
        }
    }

    private fun getFormattedLengthAndTime(clip: Clip): String {
        val time: String
        val now = ZonedDateTime.now()
        val vodDate = clip.createdAtInstant

        val daysAgo = Duration.between(vodDate, now).toDays()
        val milliseconds = vodDate.toEpochMilli()
        time = if (daysAgo <= 0) {
            // today
            context.getString(R.string.today)
        } else if (daysAgo == 1L) {
            // yesterday
            context.getString(R.string.yesterday)
        } else if (daysAgo <= 7) {
            // a week ago -> show weekday only
            DateUtils.formatDateTime(
                context,
                milliseconds,
                DateUtils.FORMAT_SHOW_WEEKDAY
            )
        } else {
            // if more than a week ago and less than a year -> show day and month only
            // if over a year ago -> show full date
            DateUtils.formatDateTime(
                context,
                milliseconds,
                DateUtils.FORMAT_SHOW_DATE
            )
        }

        return time + " - " + DateUtils.formatElapsedTime(Math.round(clip.duration).toLong())
    }

    override fun getLayoutResource(): Int {
        return R.layout.cell_vod
    }

    override fun getCornerRadiusResource(): Int {
        return R.dimen.stream_card_corner_radius
    }

    override fun getTopMarginResource(): Int {
        return R.dimen.stream_card_first_top_margin
    }

    override fun calculateCardWidth(): Int {
        return recyclerView.elementWidth
    }

    override fun compareTo(element: Clip, other: Clip): Int {
        return 0
    }

    override fun getPreviewTemplate(element: Clip): String {
        return element.thumbnailUrl
    }

    override fun getPlaceHolder(element: Clip, context: Context): Int {
        return R.drawable.preview_stream
    }

    override fun initElementStyle(): String {
        return appearanceStreamStyle
    }

    override fun setExpandedStyle(viewHolder: ClipViewHolder) {
        /*
        viewHolder.vTitle.setVisibility(View.VISIBLE);
        viewHolder.vGame.setVisibility(View.VISIBLE);
        viewHolder.sharedPadding.setVisibility(View.VISIBLE);
*/
    }

    override fun setNormalStyle(viewHolder: ClipViewHolder) {
        /*
        viewHolder.vTitle.setVisibility(View.GONE);
        viewHolder.vGame.setVisibility(View.VISIBLE);
        viewHolder.sharedPadding.setVisibility(View.VISIBLE);
*/
    }

    override fun setCollapsedStyle(viewHolder: ClipViewHolder) {
        /*
        viewHolder.vTitle.setVisibility(View.GONE);
        viewHolder.vGame.setVisibility(View.GONE);
        viewHolder.sharedPadding.setVisibility(View.GONE);
*/
    }

    fun setShowName(showName: Boolean) {
        this.showName = showName
    }
}
