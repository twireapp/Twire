package com.perflyst.twire.activities.stream

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.perflyst.twire.R
import com.perflyst.twire.activities.ChannelFragment.VodFragment.Companion.newInstance
import com.perflyst.twire.databinding.ActivityVodBinding
import com.perflyst.twire.model.VideoOnDemand

class VODFragment : VideoFragment<ActivityVodBinding>(ActivityVodBinding::inflate) {
    private var mVod: VideoOnDemand? = null
    private var vodsFragments: Fragment? = null
    private var mTitleView: TextView? = null
    private var mViewsView: TextView? = null

    override val videoContainerResource: Int get() = R.id.video_fragment_container

    override val streamArguments: Bundle
        get() {
            if (mVod == null) {
                val intent = requireArguments()
                mVod = intent.getParcelable(getString(R.string.intent_vod))
            }

            val args = Bundle()
            args.putParcelable(getString(R.string.stream_fragment_streamerInfo), mVod!!.channelInfo)
            args.putString(getString(R.string.stream_fragment_vod_id), mVod!!.videoId)
            args.putString(getString(R.string.stream_fragment_title), mVod!!.videoTitle)
            return args
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val fm = childFragmentManager

            if (vodsFragments == null) {
                vodsFragments = newInstance(mVod!!.isBroadcast, mVod!!.channelInfo)
                fm.beginTransaction().replace(R.id.additional_vods_container, vodsFragments!!)
                    .commit()
            }
        }

        mTitleView = requireView().findViewById(R.id.title)
        mViewsView = requireView().findViewById(R.id.views)

        setVodData()
    }

    override fun onDestroy() {
        super.onDestroy()
        setFragmentResult("fragmentFinished", Bundle.EMPTY)
    }

    private fun setVodData() {
        if (mVod != null) {
            mTitleView!!.text = mVod!!.videoTitle
            mViewsView!!.text = getString(R.string.vod_views, mVod!!.views)
        }
    }

    fun startNewVOD(videoOnDemand: VideoOnDemand?) {
        mVod = videoOnDemand
        setVodData()
        resetStream()
    }

    companion object {
        @JvmStatic
        fun createVODIntent(video: VideoOnDemand?, context: Context, transition: Boolean): Intent {
            val intent = Intent(context, VODFragment::class.java)
            intent.putExtra(context.getString(R.string.intent_vod), video)
            intent.putExtra(context.getString(R.string.stream_shared_transition), transition)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            return intent
        }
    }
}
