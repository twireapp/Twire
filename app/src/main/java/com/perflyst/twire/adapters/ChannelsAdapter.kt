package com.perflyst.twire.adapters

import android.app.Activity
import android.app.ActivityOptions
import android.app.SharedElementCallback
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.perflyst.twire.R
import com.perflyst.twire.activities.ChannelActivity
import com.perflyst.twire.adapters.MainActivityAdapter.ElementsViewHolder
import com.perflyst.twire.misc.RoundImageAnimation
import com.perflyst.twire.model.ChannelInfo
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.appearanceChannelStyle
import com.perflyst.twire.service.SubscriptionsDbHelper
import com.perflyst.twire.utils.Execute
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView

/**
 * Created by Sebastian Rask on 04-04-2016.
 */
class StreamerInfoViewHolder(v: View) : ElementsViewHolder(v) {
    val vDisplayName: TextView = v.findViewById(R.id.displayName)
    private val vProfileLogoImage: ImageView = v.findViewById(R.id.profileLogoImageView)
    private val vCard: CardView = v.findViewById(R.id.card_view)

    override val previewView: ImageView get() = vProfileLogoImage

    override val targetsKey: CharSequence get() = vDisplayName.getText()

    override val elementWrapper: View get() = vCard
}

class ChannelsAdapter(
    recyclerView: AutoSpanRecyclerView,
    aContext: Context,
    private val activity: Activity
) : MainActivityAdapter<ChannelInfo, StreamerInfoViewHolder>(recyclerView, aContext) {
    private val regMargin: Int =
        context.resources.getDimension(R.dimen.subscription_card_margin).toInt()

    override fun getElementsViewHolder(view: View): StreamerInfoViewHolder {
        return StreamerInfoViewHolder(view)
    }

    override fun handleElementOnClick(view: View) {
        val itemPosition = recyclerView.getChildAdapterPosition(view)
        val item = elements[itemPosition]
        val vh = recyclerView.getChildViewHolder(view) as StreamerInfoViewHolder
        val previewTarget = targets.get(vh.targetsKey)

        // Create intent for opening StreamerInfo activity. Send the StreamerInfo object with
        // the intent, and flag it to make sure it creates a new task on the history stack
        val intent = Intent(context, ChannelActivity::class.java)
        intent.putExtra(context.getString(R.string.channel_info_intent_object), item)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val sharedView = view.findViewById<View>(R.id.profileLogoImageView)
        sharedView.transitionName = context.getString(R.string.streamerInfo_transition)
        val options = ActivityOptions.makeSceneTransitionAnimation(
            activity, sharedView, context.getString(R.string.streamerInfo_transition)
        )

        activity.setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onSharedElementEnd(
                sharedElementNames: MutableList<String?>?,
                sharedElements: MutableList<View?>,
                sharedElementSnapshots: MutableList<View?>?
            ) {
                super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots)

                if (!sharedElements.isEmpty() && sharedElements[0] != null && previewTarget != null) {
                    val element = sharedElements[0]!!
                    val anim: Animation = RoundImageAnimation(
                        element.width / 2,
                        0,
                        element as ImageView,
                        previewTarget.preview
                    )
                    anim.setDuration(200)
                    anim.interpolator = DecelerateInterpolator()
                    view.startAnimation(anim)
                }
                activity.setExitSharedElementCallback(null)
            }
        })
        activity.startActivity(intent, options.toBundle())
    }

    override fun setViewLayoutParams(view: View, position: Int) {
        val marginParams = MarginLayoutParams(view.layoutParams)
        val spanCount = recyclerView.spanCount

        // If this card ISN'T the end of a row - Half the right margin
        val rightMargin = if ((position + 1) % spanCount != 0) context.resources
            .getDimension(R.dimen.subscription_card_margin_half).toInt() else
            regMargin

        // If the previous card ISN'T the end of a row, this card ISN'T be the start of a row - Half the left margin
        val leftMargin = if (position % spanCount != 0) context.resources
            .getDimension(R.dimen.subscription_card_margin_half).toInt() else
            regMargin

        val topMargin = if (position < spanCount) topMarginFirst else 0

        marginParams.setMargins(leftMargin, topMargin, rightMargin, regMargin)

        view.setLayoutParams(RelativeLayout.LayoutParams(marginParams))
    }

    override fun setViewData(element: ChannelInfo, viewHolder: StreamerInfoViewHolder) {
        viewHolder.vDisplayName.text = element.displayName
        viewHolder.vDisplayName.forceLayout()
    }

    override val layoutResource: Int get() = R.layout.cell_channel

    override val cornerRadiusResource: Int get() = R.dimen.subscription_card_corner_radius

    override val topMarginResource: Int get() = R.dimen.subscription_card_first_top_margin

    override fun calculateCardWidth(): Int {
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val spanCount =
            screenWidth / (context.resources.getDimension(R.dimen.subscription_card_width)
                .toInt() + context.resources
                .getDimension(R.dimen.subscription_card_margin).toInt())
        return (screenWidth / spanCount.toDouble()).toInt() - (context.resources
            .getDimension(R.dimen.subscription_card_margin) * 2).toInt() - (context.resources
            .getDimension(R.dimen.subscription_card_elevation) * 2).toInt()
    }

    override fun compareTo(element: ChannelInfo, other: ChannelInfo): Int {
        return element.compareTo(other)
    }

    override fun getPreviewTemplate(element: ChannelInfo): String? {
        if (element.logoURL == null) return null
        return element.logoURL.toString()
    }

    override fun getPlaceHolder(element: ChannelInfo, context: Context?): Int {
        return R.drawable.ic_profile_template_300p
    }

    override fun refreshPreview(element: ChannelInfo, context: Context?, callback: Runnable) {
        Execute.background {
            val mChannelInfo = Service.getStreamerInfoFromUserId(element.userId)
            if (mChannelInfo != null && element.logoURL !== mChannelInfo.logoURL && mChannelInfo.logoURL != null) {
                element.logoURL = mChannelInfo.logoURL
                Execute.ui(callback)

                val values = ContentValues()
                values.put(SubscriptionsDbHelper.COLUMN_LOGO_URL, element.logoURL.toString())
                Service.updateStreamerInfoDbWithValues(
                    values,
                    context,
                    element.userId
                )
            }
        }
    }

    override fun initElementStyle(): String {
        return appearanceChannelStyle
    }

    override fun setExpandedStyle(viewHolder: StreamerInfoViewHolder) {
        // This is not support for Follow cards
    }

    override fun setNormalStyle(viewHolder: StreamerInfoViewHolder) {
        viewHolder.vDisplayName.visibility = View.VISIBLE
    }

    override fun setCollapsedStyle(viewHolder: StreamerInfoViewHolder) {
        viewHolder.vDisplayName.visibility = View.GONE
    }
}
