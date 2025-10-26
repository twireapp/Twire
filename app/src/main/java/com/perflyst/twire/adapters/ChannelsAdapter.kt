package com.perflyst.twire.adapters

import android.content.ContentValues
import android.content.Context
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.transition.Fade
import com.perflyst.twire.R
import com.perflyst.twire.activities.ChannelFragment
import com.perflyst.twire.adapters.MainActivityAdapter.ElementsViewHolder
import com.perflyst.twire.misc.navigate
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
    private val fragment: Fragment
) : MainActivityAdapter<ChannelInfo, StreamerInfoViewHolder>(recyclerView, aContext) {
    private val regMargin: Int =
        context.resources.getDimension(R.dimen.subscription_card_margin).toInt()

    override fun getElementsViewHolder(view: View): StreamerInfoViewHolder {
        return StreamerInfoViewHolder(view)
    }

    override fun handleElementOnClick(view: View) {
        val itemPosition = recyclerView.getChildAdapterPosition(view)
        val item = elements[itemPosition]

        val sharedView = view.findViewById<View>(R.id.profileLogoImageView)
        val bundle = bundleOf(
            context.getString(R.string.channel_info_intent_object) to item
        )
        fragment.navigate(
            ChannelFragment::class.java,
            bundle,
            enterAnim = Fade(),
            exitAnim = Fade(),
            sharedElement = sharedView,
            sharedName = context.getString(R.string.streamerInfo_transition)
        )
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
        viewHolder.previewView.transitionName = element.userId
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
