package com.perflyst.twire.activities.stream

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.perflyst.twire.R
import com.perflyst.twire.activities.ChannelActivity.VodFragment.Companion.newInstance
import com.perflyst.twire.model.VideoOnDemand

class VODActivity : StreamActivity() {
    private var mVod: VideoOnDemand? = null
    private var vodsFragments: Fragment? = null
    private var mTitleView: TextView? = null
    private var mViewsView: TextView? = null

    override val layoutResource: Int get() = R.layout.activity_vod

    override val videoContainerResource: Int get() = R.id.video_fragment_container

    override val streamArguments: Bundle
        get() {
            if (mVod == null) {
                val intent = getIntent()
                mVod = intent.getParcelableExtra(getString(R.string.intent_vod))
            }

            val args = Bundle()
            args.putParcelable(getString(R.string.stream_fragment_streamerInfo), mVod!!.channelInfo)
            args.putString(getString(R.string.stream_fragment_vod_id), mVod!!.videoId)
            args.putString(getString(R.string.stream_fragment_title), mVod!!.videoTitle)
            return args
        }

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        if (savedInstance == null) {
            val fm = supportFragmentManager

            if (vodsFragments == null) {
                vodsFragments = newInstance(mVod!!.isBroadcast, mVod!!.channelInfo)
                fm.beginTransaction().replace(R.id.additional_vods_container, vodsFragments!!)
                    .commit()
            }
        }

        mTitleView = findViewById(R.id.title)
        mViewsView = findViewById(R.id.views)

        setVodData()
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
            val intent = Intent(context, VODActivity::class.java)
            intent.putExtra(context.getString(R.string.intent_vod), video)
            intent.putExtra(context.getString(R.string.stream_shared_transition), transition)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            return intent
        }
    }
}
