package com.perflyst.twire.adapters

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.SharedElementCallback
import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.perflyst.twire.R
import com.perflyst.twire.activities.stream.VODActivity
import com.perflyst.twire.activities.stream.VODActivity.Companion.createVODIntent
import com.perflyst.twire.adapters.MainActivityAdapter.ElementsViewHolder
import com.perflyst.twire.model.VideoOnDemand
import com.perflyst.twire.service.Settings.appearanceStreamStyle
import com.perflyst.twire.service.Settings.getVodProgress
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Created by Sebastian Rask on 16-06-2016.
 */
class VODViewHolder(v: View) : ElementsViewHolder(v) {
    val vPreviewImage: ImageView = v.findViewById(R.id.image_stream_preview)
    val vDisplayName: TextView = v.findViewById(R.id.displayName)
    val vTitle: TextView = v.findViewById(R.id.stream_title)
    val vGame: TextView = v.findViewById(R.id.stream_game_and_viewers)
    val vTimeStamp: TextView = v.findViewById(R.id.timestamp)
    val vProgressBar: ProgressBar = v.findViewById(R.id.progressBar)
    private val vCard: CardView = v.findViewById(R.id.card_view_vod_stream)

    override val previewView: ImageView get() = vPreviewImage

    override val targetsKey: CharSequence
        get() =
            vDisplayName.getText().toString() + vTimeStamp.getText().toString()

    override val elementWrapper: View get() = vCard
}

class VODAdapter(recyclerView: AutoSpanRecyclerView, private val activity: Activity) :
    MainActivityAdapter<VideoOnDemand, VODViewHolder>(
        recyclerView,
        activity
    ) {
    private val vodWatchedImageAlpha = 0.5f
    val topMargin: Int
    val bottomMargin: Int
    private var rightMargin: Int
    private var leftMargin: Int
    private var showName = true

    init {
        rightMargin =
            context.resources.getDimension(R.dimen.stream_card_right_margin).toInt()
        bottomMargin =
            context.resources.getDimension(R.dimen.stream_card_bottom_margin).toInt()
        topMargin = context.resources.getDimension(R.dimen.stream_card_top_margin).toInt()
        leftMargin =
            context.resources.getDimension(R.dimen.stream_card_left_margin).toInt()
    }

    override fun getElementsViewHolder(view: View): VODViewHolder {
        return VODViewHolder(view)
    }

    override fun handleElementOnClick(view: View) {
        val itemPosition = recyclerView.getChildAdapterPosition(view)
        val item = elements[itemPosition]!!
        if (activity is VODActivity) {
            activity.intent
                .putExtra(context.getString(R.string.stream_shared_transition), false)
            activity.startNewVOD(item)
        } else {
            val intent = createVODIntent(item, context, true)

            intent.putExtra(
                context.getString(R.string.stream_preview_url),
                getPreviewUrl(item)
            )
            intent.putExtra(
                context.getString(R.string.stream_preview_alpha),
                if (hasVodBeenWatched(item.videoId!!)) vodWatchedImageAlpha else 1.0f
            )

            val sharedView = view.findViewById<View>(R.id.image_stream_preview)
            sharedView.transitionName = context.getString(R.string.stream_preview_transition)
            val options = ActivityOptions.makeSceneTransitionAnimation(
                activity, sharedView, context.getString(R.string.stream_preview_transition)
            )

            activity.setExitSharedElementCallback(object : SharedElementCallback() {
                @SuppressLint("NewApi")
                override fun onSharedElementEnd(
                    sharedElementNames: MutableList<String?>?,
                    sharedElements: MutableList<View?>?,
                    sharedElementSnapshots: MutableList<View?>?
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
        rightMargin = if ((position + 1) % spanCount != 0) context.resources
            .getDimension(R.dimen.stream_card_margin_half).toInt() else context.resources
            .getDimension(R.dimen.stream_card_right_margin).toInt()

        // If the previous card ISN'T the end of a row, this card ISN'T be the start of a row - Half the left margin
        leftMargin = if (position % spanCount != 0) context.resources
            .getDimension(R.dimen.stream_card_margin_half).toInt() else context.resources
            .getDimension(R.dimen.stream_card_left_margin).toInt()


        // Set the correct margin of the card
        val marginParams = MarginLayoutParams(view.layoutParams)

        if (position < spanCount) { // Give extra top margin to cards in the first row to make sure it doesn't get overlapped by the toolbar
            marginParams.setMargins(leftMargin, topMarginFirst, rightMargin, bottomMargin)
        } else {
            marginParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin)
        }

        view.setLayoutParams(RelativeLayout.LayoutParams(marginParams))
    }

    override fun setViewData(element: VideoOnDemand, viewHolder: VODViewHolder) {
        val metrics = context.resources.displayMetrics
        viewHolder.vPreviewImage.layoutParams.width = metrics.widthPixels

        var gameAndViewers = context.getString(R.string.vod_views, element.views)
        if (!element.gameTitle!!.isEmpty()) {
            gameAndViewers += " - ${element.gameTitle}"
        }
        viewHolder.vTitle.text = element.videoTitle
        viewHolder.vGame.text = gameAndViewers
        viewHolder.vPreviewImage.setVisibility(View.VISIBLE)
        viewHolder.vDisplayName.text = element.displayName
        viewHolder.vTimeStamp.text = getFormattedLengthAndTime(element)
        if (!showName) {
            viewHolder.vDisplayName.visibility = View.GONE
        }

        if (hasVodBeenWatched(element.videoId!!)) {
            val vodProgress = getVodProgress(element.videoId)

            viewHolder.vProgressBar.visibility = View.VISIBLE
            viewHolder.vProgressBar.setPadding(0, 0, 0, 0)

            viewHolder.vPreviewImage.animate().alpha(vodWatchedImageAlpha).setDuration(300)
                .start()
            viewHolder.vProgressBar.setMax(element.length.toInt())
            ObjectAnimator.ofInt(viewHolder.vProgressBar, "progress", vodProgress).setDuration(300)
                .start()
        } else {
            viewHolder.vProgressBar.visibility = View.INVISIBLE
            viewHolder.vPreviewImage.setAlpha(1f)
        }
    }

    private fun hasVodBeenWatched(id: String): Boolean {
        return getVodProgress(id) > 0
    }

    private fun getFormattedLengthAndTime(vod: VideoOnDemand): String {
        val time: String?
        val now = ZonedDateTime.now()
        val vodDate = vod.recordedAt

        val daysAgo = Duration.between(vodDate, now).toDays()
        val milliseconds = vodDate.toInstant().toEpochMilli()
        time = if (daysAgo <= 0) {
            // today
            context.getString(R.string.today)
        } else if (daysAgo == 1L) {
            // yesterday
            context.getString(R.string.yesterday)
        } else if (daysAgo <= 7) {
            // a week ago -> show weekday only
            DateUtils.formatDateTime(context, milliseconds, DateUtils.FORMAT_SHOW_WEEKDAY)
        } else {
            // if more than a week ago and less than a year -> show day and month only
            // if over a year ago -> show full date
            DateUtils.formatDateTime(context, milliseconds, DateUtils.FORMAT_SHOW_DATE)
        }

        return "$time - ${DateUtils.formatElapsedTime(vod.length)}"
    }

    override val layoutResource: Int get() = R.layout.cell_vod

    override val cornerRadiusResource: Int get() = R.dimen.stream_card_corner_radius

    override val topMarginResource: Int get() = R.dimen.stream_card_first_top_margin

    override fun calculateCardWidth(): Int {
        return recyclerView.elementWidth
    }

    override fun compareTo(element: VideoOnDemand, other: VideoOnDemand): Int {
        return element.compareTo(other)
    }

    override fun getPreviewTemplate(element: VideoOnDemand): String {
        return element.previewTemplate
    }

    override fun getPlaceHolder(element: VideoOnDemand, context: Context?): Int {
        return R.drawable.template_stream
    }

    override fun initElementStyle(): String {
        return appearanceStreamStyle
    }

    override fun setExpandedStyle(viewHolder: VODViewHolder) {
        /*
        viewHolder.vTitle.setVisibility(View.VISIBLE);
        viewHolder.vGame.setVisibility(View.VISIBLE);
        viewHolder.sharedPadding.setVisibility(View.VISIBLE);
*/
    }

    override fun setNormalStyle(viewHolder: VODViewHolder) {
        /*
        viewHolder.vTitle.setVisibility(View.GONE);
        viewHolder.vGame.setVisibility(View.VISIBLE);
        viewHolder.sharedPadding.setVisibility(View.VISIBLE);
*/
    }

    override fun setCollapsedStyle(viewHolder: VODViewHolder) {
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
