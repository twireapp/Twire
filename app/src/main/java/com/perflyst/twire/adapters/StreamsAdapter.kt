package com.perflyst.twire.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.perflyst.twire.R
import com.perflyst.twire.activities.ChannelActivity
import com.perflyst.twire.activities.stream.LiveStreamActivity.Companion.createLiveStreamIntent
import com.perflyst.twire.adapters.MainActivityAdapter.ElementsViewHolder
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.model.StreamInfo
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.appearanceStreamStyle
import com.perflyst.twire.utils.Execute
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView

/**
 * Created by Sebastian Rask on 11-04-2016.
 */
class StreamViewHolder(v: View) : ElementsViewHolder(v) {
    val vPreviewImage: ImageView = v.findViewById(R.id.image_stream_preview)
    val vDisplayName: TextView = v.findViewById(R.id.displayName)
    val vTitle: TextView = v.findViewById(R.id.stream_title)
    val vGame: TextView = v.findViewById(R.id.stream_game_and_viewers)
    val vOnlineSince: TextView = v.findViewById(R.id.stream_online_since)
    val sharedPadding: View = v.findViewById(R.id.shared_padding)
    private val vCard: CardView = v.findViewById(R.id.cardView_online_streams)

    override val previewView: ImageView get() = vPreviewImage

    override val targetsKey: CharSequence = vDisplayName.getText()

    override val elementWrapper: View get() = vCard
}

class StreamsAdapter(recyclerView: AutoSpanRecyclerView, private val activity: Activity) :
    MainActivityAdapter<StreamInfo, StreamViewHolder>(
        recyclerView,
        activity
    ) {
    private val topMargin: Int
    private val bottomMargin: Int
    private var rightMargin: Int
    private var leftMargin: Int

    init {
        rightMargin =
            context.resources.getDimension(R.dimen.stream_card_right_margin).toInt()
        bottomMargin =
            context.resources.getDimension(R.dimen.stream_card_bottom_margin).toInt()
        topMargin = context.resources.getDimension(R.dimen.stream_card_top_margin).toInt()
        leftMargin =
            context.resources.getDimension(R.dimen.stream_card_left_margin).toInt()
    }


    override fun getElementsViewHolder(view: View): StreamViewHolder {
        return StreamViewHolder(view)
    }

    @SuppressLint("NewApi")
    override fun handleElementOnClick(view: View) {
        val itemPosition = recyclerView.getChildAdapterPosition(view)

        if (itemPosition < 0 || elements.size <= itemPosition) {
            return
        }

        val item = elements[itemPosition]!!
        val intent = createLiveStreamIntent(item, true, context)

        val sharedView = view.findViewById<View>(R.id.image_stream_preview)
        sharedView.transitionName = context.getString(R.string.stream_preview_transition)
        val options = ActivityOptions.makeSceneTransitionAnimation(
            activity, sharedView, context.getString(R.string.stream_preview_transition)
        )
        activity.startActivity(intent, options.toBundle())
    }

    override fun handleElementOnLongClick(view: View) {
        val itemPosition = recyclerView.getChildAdapterPosition(view)

        val item = elements[itemPosition]!!
        val userInfo = item.userInfo

        Execute.background {
            val mChannelInfo = Service.getStreamerInfoFromUserId(userInfo.userId)
            val intent = Intent(context, ChannelActivity::class.java)
            intent.putExtra(
                context.getString(R.string.channel_info_intent_object),
                mChannelInfo
            )
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)
            activity.overridePendingTransition(
                R.anim.slide_in_right_anim,
                R.anim.fade_out_semi_anim
            )
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

    override fun adapterSpecial(viewHolder: StreamViewHolder) {
        val previewImageWidth = context.resources
            .displayMetrics.widthPixels / recyclerView.spanCount
            .toDouble() - leftMargin - rightMargin
        val previewImageHeight = previewImageWidth / (16 / 9.0)
        viewHolder.vPreviewImage.setMinimumHeight(previewImageHeight.toInt())
    }

    override fun setViewData(element: StreamInfo, viewHolder: StreamViewHolder) {
        val metrics = context.resources.displayMetrics
        viewHolder.vPreviewImage.layoutParams.width = metrics.widthPixels

        val viewers =
            context.getString(R.string.my_streams_cell_current_viewers, element.currentViewers)
        val gameAndViewers = "$viewers - ${element.game}"

        viewHolder.vDisplayName.text = element.userInfo.displayName
        viewHolder.vTitle.text = element.title
        viewHolder.vGame.text = gameAndViewers
        viewHolder.vOnlineSince.text = Utils.getOnlineSince(element.startedAt)
        viewHolder.vPreviewImage.setVisibility(View.VISIBLE)
    }

    override val layoutResource: Int get() = R.layout.cell_stream

    override val cornerRadiusResource: Int get() = R.dimen.stream_card_corner_radius

    override val topMarginResource: Int get() = R.dimen.stream_card_first_top_margin

    override fun calculateCardWidth(): Int {
        return recyclerView.elementWidth
    }

    override fun compareTo(element: StreamInfo, other: StreamInfo): Int {
        return element.compareTo(other)
    }

    override fun getPreviewTemplate(element: StreamInfo): String? {
        return element.previewTemplate
    }

    override fun getPlaceHolder(element: StreamInfo, context: Context?): Int {
        return R.drawable.template_stream
    }

    override fun initElementStyle(): String {
        return appearanceStreamStyle
    }

    override fun setExpandedStyle(viewHolder: StreamViewHolder) {
        viewHolder.vTitle.visibility = View.VISIBLE
        viewHolder.vGame.visibility = View.VISIBLE
        viewHolder.sharedPadding.visibility = View.VISIBLE
    }

    override fun setNormalStyle(viewHolder: StreamViewHolder) {
        viewHolder.vTitle.visibility = View.GONE
        viewHolder.vGame.visibility = View.VISIBLE
        viewHolder.sharedPadding.visibility = View.VISIBLE
    }

    override fun setCollapsedStyle(viewHolder: StreamViewHolder) {
        viewHolder.vTitle.visibility = View.GONE
        viewHolder.vGame.visibility = View.GONE
        viewHolder.sharedPadding.visibility = View.GONE
    }
}
