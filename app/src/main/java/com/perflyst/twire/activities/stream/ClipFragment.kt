package com.perflyst.twire.activities.stream

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.github.twitch4j.helix.domain.Clip
import com.perflyst.twire.R
import com.perflyst.twire.activities.ChannelFragment
import com.perflyst.twire.databinding.ActivityVodBinding
import com.perflyst.twire.model.ChannelInfo
import com.perflyst.twire.utils.Constants
import org.parceler.Parcels

class ClipFragment : VideoFragment<ActivityVodBinding>(ActivityVodBinding::inflate) {
    private var clip: Clip? = null
    private var channel: ChannelInfo? = null
    private var clipsFragment: Fragment? = null
    private var mTitleView: TextView? = null
    private var mViewsView: TextView? = null

    override val videoContainerResource: Int get() = R.id.video_fragment_container

    override val streamArguments: Bundle
        get() {
            if (clip == null) {
                val intent = requireArguments()
                clip = Parcels.unwrap(intent.getParcelable(Constants.KEY_CLIP))
                channel = intent.getParcelable(getString(R.string.channel_info_intent_object))
            }

            val args = Bundle()
            args.putParcelable(Constants.KEY_CLIP, Parcels.wrap(clip))
            args.putParcelable(getString(R.string.stream_fragment_streamerInfo), channel)
            args.putString(getString(R.string.stream_fragment_vod_id), clip!!.id)
            args.putString(getString(R.string.stream_fragment_title), clip!!.title)
            return args
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val fm = childFragmentManager

            if (clipsFragment == null) {
                clipsFragment = ChannelFragment.ClipFragment.newInstance(channel)
                fm.beginTransaction()
                    .replace(R.id.additional_vods_container, clipsFragment as Fragment).commit()
            }
        }

        mTitleView = binding.title
        mViewsView = binding.views

        setClipData()
    }

    override fun onDestroy() {
        super.onDestroy()
        setFragmentResult("fragmentFinished", Bundle.EMPTY)
    }

    private fun setClipData() {
        if (clip != null) {
            mTitleView!!.text = clip!!.title
            mViewsView!!.text = getString(R.string.vod_views, clip!!.viewCount)
        }
    }

    fun startNewClip(clip: Clip?) {
        this.clip = clip
        setClipData()
        resetStream()
    }

    companion object {
        @JvmStatic
        fun createClipIntent(
            clip: Clip,
            channel: ChannelInfo?,
            context: Context,
            transition: Boolean
        ): Intent {
            val intent = Intent(context, ClipFragment::class.java)
            intent.putExtra(Constants.KEY_CLIP, Parcels.wrap(clip))
            intent.putExtra(context.getString(R.string.channel_info_intent_object), channel)
            intent.putExtra(context.getString(R.string.stream_shared_transition), transition)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            return intent
        }
    }
}
