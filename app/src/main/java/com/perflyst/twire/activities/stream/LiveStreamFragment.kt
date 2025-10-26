package com.perflyst.twire.activities.stream

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.perflyst.twire.R
import com.perflyst.twire.adapters.MentionAdapter
import com.perflyst.twire.adapters.MentionAdapter.MentionAdapterDelegate
import com.perflyst.twire.databinding.ActivityStreamBinding
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.model.StreamInfo
import com.perflyst.twire.model.UserInfo
import timber.log.Timber

/**
 * Created by Sebastian Rask on 18-06-2016.
 */
class LiveStreamFragment : VideoFragment<ActivityStreamBinding>(ActivityStreamBinding::inflate) {
    private var mMentionRecyclerView: RecyclerView? = null
    private var mMentionAdapter: MentionAdapter? = null
    private var mMentionContainer: View? = null

    override val videoContainerResource: Int get() = R.id.video_fragment_container

    override val streamArguments: Bundle
        get() {
            val intent = requireArguments()
            val mUserInfo =
                intent.getParcelable<UserInfo?>(getString(R.string.intent_key_streamer_info))
            val currentViewers =
                intent.getInt(getString(R.string.intent_key_stream_viewers), -1)
            val currentStartTime =
                intent.getLong(getString(R.string.intent_key_stream_start_time), 0)
            val title = intent.getString(getString(R.string.stream_fragment_title))
            val previewUrl = intent.getString(getString(R.string.stream_preview_url))
            val sharedTransition =
                intent.getBoolean(getString(R.string.stream_shared_transition), false)

            val args = Bundle()
            args.putParcelable(getString(R.string.stream_fragment_streamerInfo), mUserInfo)
            args.putInt(getString(R.string.stream_fragment_viewers), currentViewers)
            args.putLong(getString(R.string.stream_fragment_start_time), currentStartTime)
            args.putBoolean(getString(R.string.stream_fragment_autoplay), true)
            args.putString(getString(R.string.stream_fragment_title), title)
            args.putString(getString(R.string.stream_preview_url), previewUrl)
            args.putBoolean(getString(R.string.stream_shared_transition), sharedTransition)
            return args
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null && mMentionRecyclerView == null) {
            mMentionContainer = view.findViewById(R.id.mention_container)
            mMentionContainer!!.visibility = View.GONE
            mMentionRecyclerView = view.findViewById(R.id.mention_recyclerview)
            setupMentionSuggestionRecyclerView()
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.d("Live stream activity stopped")
    }

    override fun onBackPressed(): Boolean {
        setSuggestions(ArrayList(), null)
        return super.onBackPressed()
    }

    fun setSuggestions(suggestions: MutableList<String>, inputRect: Rect?) {
        if (mMentionAdapter == null) {
            return
        }

        mMentionAdapter!!.setSuggestions(suggestions)

        if (inputRect == null) {
            return
        }

        mMentionContainer!!.visibility = if (suggestions.isEmpty()) View.GONE else View.VISIBLE

        mMentionContainer!!.getViewTreeObserver()
            .addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mMentionContainer!!.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                    //ToDo: Check height of container and adjust if necessary
                    resources.getDimension(R.dimen.chat_mention_suggestions_max_height)
                    val currentHeight = mMentionContainer!!.height.toFloat()

                    /*
                if (maxHeight < currentHeight) {
                    mMentionContainer.setLayoutParams(new RelativeLayout.LayoutParams(
                            mMentionContainer.getLayoutParams().width,
                            (int) maxHeight
                    ));

                    currentHeight = maxHeight;
                }
*/
                    mMentionContainer!!.y =
                        (inputRect.top - inputRect.height() - currentHeight.toInt()).toFloat()
                }
            })
    }

    private fun setupMentionSuggestionRecyclerView() {
        mMentionAdapter = MentionAdapter(MentionAdapterDelegate { suggestion: String? ->
            this@LiveStreamFragment.setSuggestions(ArrayList(), null)
            if (mChatFragment == null) {
                return@MentionAdapterDelegate
            }
            mChatFragment!!.insertMentionSuggestion(suggestion!!)
        })
        mMentionRecyclerView!!.setLayoutManager(LinearLayoutManager(requireContext()))
        mMentionRecyclerView!!.setAdapter(mMentionAdapter)
    }

    companion object {
        @JvmStatic
        fun createLiveStreamIntent(
            stream: StreamInfo,
            sharedTransition: Boolean,
            context: Context
        ): Intent {
            val liveStreamIntent = Intent(context, LiveStreamFragment::class.java)
            liveStreamIntent.putExtra(
                context.getString(R.string.intent_key_streamer_info),
                stream.userInfo
            )
            liveStreamIntent.putExtra(
                context.getString(R.string.intent_key_stream_viewers),
                stream.currentViewers
            )
            liveStreamIntent.putExtra(
                context.getString(R.string.intent_key_stream_start_time),
                stream.startedAt
            )
            liveStreamIntent.putExtra(
                context.getString(R.string.stream_preview_url),
                Utils.getPreviewUrl(stream.previewTemplate)
            )
            liveStreamIntent.putExtra(
                context.getString(R.string.stream_shared_transition),
                sharedTransition
            )
            liveStreamIntent.putExtra(
                context.getString(R.string.stream_fragment_title),
                stream.title
            )
            return liveStreamIntent
        }
    }
}


