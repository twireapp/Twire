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
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.model.StreamInfo
import com.perflyst.twire.model.UserInfo
import timber.log.Timber

/**
 * Created by Sebastian Rask on 18-06-2016.
 */
class LiveStreamActivity : StreamActivity() {
    private var mMentionRecyclerView: RecyclerView? = null
    private var mMentionAdapter: MentionAdapter? = null
    private var mMentionContainer: View? = null

    override val layoutResource: Int get() = R.layout.activity_stream

    override val videoContainerResource: Int get() = R.id.video_fragment_container

    override val streamArguments: Bundle
        get() {
            val intent = getIntent()
            val mUserInfo =
                intent.getParcelableExtra<UserInfo?>(getString(R.string.intent_key_streamer_info))
            val currentViewers =
                intent.getIntExtra(getString(R.string.intent_key_stream_viewers), -1)
            val currentStartTime =
                intent.getLongExtra(getString(R.string.intent_key_stream_start_time), 0)
            val title = intent.getStringExtra(getString(R.string.stream_fragment_title))

            val args = Bundle()
            args.putParcelable(getString(R.string.stream_fragment_streamerInfo), mUserInfo)
            args.putInt(getString(R.string.stream_fragment_viewers), currentViewers)
            args.putLong(getString(R.string.stream_fragment_start_time), currentStartTime)
            args.putBoolean(getString(R.string.stream_fragment_autoplay), true)
            args.putString(getString(R.string.stream_fragment_title), title)
            return args
        }

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        if (savedInstance == null) {
            supportFragmentManager

            if (mMentionRecyclerView == null) {
                mMentionContainer = findViewById(R.id.mention_container)
                mMentionContainer!!.visibility = View.GONE
                mMentionRecyclerView = findViewById(R.id.mention_recyclerview)
                setupMentionSuggestionRecyclerView()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.d("Live stream activity stopped")
    }

    override fun onBackPressed() {
        setSuggestions(ArrayList(), null)
        super.onBackPressed()
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
                    getResources().getDimension(R.dimen.chat_mention_suggestions_max_height)
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
            this@LiveStreamActivity.setSuggestions(ArrayList(), null)
            if (mChatFragment == null) {
                return@MentionAdapterDelegate
            }
            mChatFragment!!.insertMentionSuggestion(suggestion!!)
        })
        mMentionRecyclerView!!.setLayoutManager(LinearLayoutManager(this))
        mMentionRecyclerView!!.setAdapter(mMentionAdapter)
    }

    companion object {
        @JvmStatic
        fun createLiveStreamIntent(
            stream: StreamInfo,
            sharedTransition: Boolean,
            context: Context
        ): Intent {
            val liveStreamIntent = Intent(context, LiveStreamActivity::class.java)
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


