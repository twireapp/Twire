package com.perflyst.twire.activities.main

import com.perflyst.twire.R
import com.perflyst.twire.TwireApplication
import com.perflyst.twire.adapters.GamesAdapter
import com.perflyst.twire.adapters.MainActivityAdapter
import com.perflyst.twire.model.Game
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.GameAutoSpanBehaviour
import timber.log.Timber

/**
 * Activity that loads and shows the top games on Twitch.
 * The Activity loads the content as it is needed.
 */
class TopGamesActivity : LazyMainActivity<Game>() {
    override val activityIconRes: Int get() = R.drawable.ic_games

    override val activityTitleRes: Int get() = R.string.top_games_activity_title

    override fun constructSpanBehaviour(): AutoSpanBehaviour {
        return GameAutoSpanBehaviour()
    }

    override fun customizeActivity() {
        super.customizeActivity()
        limit = 20
    }

    override fun addToAdapter(aGamesList: MutableList<Game>) {
        mAdapter.addList(aGamesList)
        Timber.i("Adding Top Games: %s", aGamesList.size)
    }

    override fun constructAdapter(recyclerView: AutoSpanRecyclerView): MainActivityAdapter<Game, *> {
        return GamesAdapter(recyclerView, baseContext, this)
    }

    override val visualElements: MutableList<Game>
        get() {
            val response =
                TwireApplication.helix.getTopGames(null, cursor, null, limit.toString())
                    .execute()
            cursor = response.pagination.cursor
            return response.games.map(::Game).toMutableList()
        }
}
