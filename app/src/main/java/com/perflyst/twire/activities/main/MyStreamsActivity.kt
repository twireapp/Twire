package com.perflyst.twire.activities.main

import com.perflyst.twire.R
import com.perflyst.twire.TwireApplication
import com.perflyst.twire.adapters.MainActivityAdapter
import com.perflyst.twire.adapters.StreamsAdapter
import com.perflyst.twire.model.StreamInfo
import com.perflyst.twire.model.UserInfo
import com.perflyst.twire.service.TempStorage
import com.perflyst.twire.tasks.GetFollowsFromDB
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.StreamAutoSpanBehaviour
import timber.log.Timber

class MyStreamsActivity : LazyMainActivity<StreamInfo>() {
    override val activityIconRes: Int get() = R.drawable.ic_favorite

    override val activityTitleRes: Int get() = R.string.my_streams_activity_title

    override fun constructSpanBehaviour(): AutoSpanBehaviour {
        return StreamAutoSpanBehaviour()
    }

    override fun constructAdapter(recyclerView: AutoSpanRecyclerView): MainActivityAdapter<StreamInfo, *> {
        return StreamsAdapter(recyclerView, this)
    }

    override fun addToAdapter(aObjectList: MutableList<StreamInfo>) {
        mAdapter.addList(aObjectList)
        Timber.i("Adding Top Streams: %s", aObjectList.size)
    }

    override val visualElements: MutableList<StreamInfo>
        get() {
            if (!TempStorage.hasLoadedStreamers()) {
                GetFollowsFromDB(this).call()
            }

            val channels = TempStorage.loadedStreamers.map(UserInfo::userId).toList()
            val results = channels.chunked(100)
                .flatMap {
                    TwireApplication.helix.getStreams(
                        null,
                        null,
                        null,
                        100,
                        null,
                        null,
                        it,
                        null
                    ).execute().streams
                }
                .map(::StreamInfo)
                .toMutableList()

            maxElementsToFetch = results.size

            return results
        }
}
