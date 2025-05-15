package com.perflyst.twire.activities

import com.perflyst.twire.R
import com.perflyst.twire.TwireApplication
import com.perflyst.twire.activities.main.LazyMainActivity
import com.perflyst.twire.adapters.MainActivityAdapter
import com.perflyst.twire.adapters.StreamsAdapter
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.model.Game
import com.perflyst.twire.model.StreamInfo
import com.perflyst.twire.service.Settings.generalFilterTopStreamsByLanguage
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.StreamAutoSpanBehaviour

class GameActivity : LazyMainActivity<StreamInfo>() {
    private var game: Game? = null

    override fun constructAdapter(recyclerView: AutoSpanRecyclerView): MainActivityAdapter<StreamInfo, *> {
        return StreamsAdapter(recyclerView, this)
    }

    public override fun customizeActivity() {
        val intent = getIntent()
        game = intent.getParcelableExtra(getString(R.string.game_intent_key))
        checkNotNull(game)
        mTitleView.text = game!!.gameTitle
    }

    override val activityIconRes: Int get() = R.drawable.ic_sports_esports

    override val activityTitleRes: Int get() = R.string.my_streams_activity_title

    override fun constructSpanBehaviour(): AutoSpanBehaviour {
        return StreamAutoSpanBehaviour()
    }

    override fun addToAdapter(streamsToAdd: MutableList<StreamInfo>) {
        mAdapter.addList(streamsToAdd)
    }

    override val visualElements: MutableList<StreamInfo>
        get() {
            val languageFilter =
                if (generalFilterTopStreamsByLanguage) Utils.systemLanguage else null
            val response = TwireApplication.helix.getStreams(
                null,
                cursor,
                null,
                limit,
                listOf(game!!.gameId),
                listOf(languageFilter),
                null,
                null
            ).execute()
            cursor = response.pagination.cursor
            return response.streams.map(::StreamInfo).toMutableList()
        }
}
