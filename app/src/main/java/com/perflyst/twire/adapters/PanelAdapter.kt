package com.perflyst.twire.adapters

import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.perflyst.twire.R
import com.perflyst.twire.adapters.PanelAdapter.PanelViewHolder
import com.perflyst.twire.model.Panel
import timber.log.Timber

/**
 * Created by Sebastian Rask on 24-02-2017.
 */
class PanelAdapter(private val mActivity: Activity) : RecyclerView.Adapter<PanelViewHolder?>() {
    private val mPanels: MutableList<Panel> = ArrayList()

    fun addPanels(panels: MutableList<Panel>) {
        mPanels.addAll(panels)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanelViewHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.cell_panel, parent, false)

        return PanelViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return mPanels.size
    }

    override fun onBindViewHolder(holder: PanelViewHolder, position: Int) {
        val mPanel = mPanels[position]

        val imageUrl = mPanel.getmImageUrl()
        if (imageUrl != null && !imageUrl.isEmpty() && (imageUrl != "null")) {
            Glide.with(mActivity).load(mPanel.getmImageUrl()).into(holder.mImageView)
        }

        val link = mPanel.getmLinkUrl()
        if (link != null && !link.isEmpty() && (link != "null")) {
            holder.itemView.setOnClickListener { view: View? ->
                val mTabs = CustomTabsIntent.Builder()
                mTabs.setStartAnimations(
                    mActivity,
                    R.anim.slide_in_bottom_anim,
                    R.anim.fade_out_semi_anim
                )
                mTabs.setExitAnimations(
                    mActivity,
                    R.anim.fade_in_semi_anim,
                    R.anim.slide_out_bottom_anim
                )
                try {
                    mTabs.build().launchUrl(mActivity, Uri.parse(link))
                } catch (e: ActivityNotFoundException) {
                    Timber.e(e)
                }
            }
        }


        if (mPanel.getmHtml().isEmpty() || mPanel.getmHtml() == "null") {
            holder.mHtmlText.visibility = View.GONE
        } else {
            holder.mHtmlText.visibility = View.VISIBLE
            holder.mHtmlText.text = HtmlCompat.fromHtml(
                mPanel.getmHtml(),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            holder.mHtmlText.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    class PanelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mImageView: ImageView = itemView.findViewById(R.id.panel_image)
        val mHtmlText: TextView = itemView.findViewById(R.id.panel_html)
    }
}
