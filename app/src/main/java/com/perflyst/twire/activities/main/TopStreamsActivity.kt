package com.perflyst.twire.activities.main

import com.perflyst.twire.R
import com.perflyst.twire.TwireApplication
import com.perflyst.twire.adapters.MainActivityAdapter
import com.perflyst.twire.adapters.StreamsAdapter
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.model.StreamInfo
import com.perflyst.twire.service.Settings.generalFilterTopStreamsByLanguage
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.StreamAutoSpanBehaviour
import timber.log.Timber

class TopStreamsActivity : LazyMainActivity<StreamInfo>() {
    override val activityIconRes: Int get() = R.drawable.ic_group

    override val activityTitleRes: Int get() = R.string.top_streams_activity_title

    override fun constructSpanBehaviour(): AutoSpanBehaviour {
        return StreamAutoSpanBehaviour()
    }

    override fun constructAdapter(recyclerView: AutoSpanRecyclerView): MainActivityAdapter<StreamInfo, *> {
        return StreamsAdapter(recyclerView, this)
    }

    override fun addToAdapter(streamsToAdd: MutableList<StreamInfo>) {
        mAdapter.addList(streamsToAdd)
        Timber.i("Adding Top Streams: %s", streamsToAdd.size)
    }

    override val visualElements: MutableList<StreamInfo>
        get() {
            val languageFilter =
                if (generalFilterTopStreamsByLanguage) listOf<String?>(Utils.systemLanguage) else listOf()
            val response = TwireApplication.helix.getStreams(
                null,
                cursor,
                null,
                limit,
                null,
                languageFilter,
                null,
                null
            ).execute()
            cursor = response.pagination.cursor
            return response.streams.map(::StreamInfo).toMutableList()
        }
}
