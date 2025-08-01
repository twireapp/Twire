package com.perflyst.twire.tasks

import android.content.Context
import com.google.common.util.concurrent.ListenableFutureTask
import com.perflyst.twire.model.ChannelInfo
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.TempStorage
import com.perflyst.twire.utils.Execute
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

/**
 * Connects to the internal database and extracts all the subscription streamer names. creates an
 * object getting all information about the stream and puts it in a list.
 * After it has gotten all the streamer names it rebuilds the cards in the recyclerview
 */
class GetFollowsFromDB(baseContext: Context?) : Callable<MutableMap<String, ChannelInfo>> {
    private val timerStart = System.currentTimeMillis()
    private val twitchUserFollows: ListenableFutureTask<List<ChannelInfo>> =
        ListenableFutureTask.create(GetTwitchUserFollows(baseContext))
    private val baseContext: WeakReference<Context?> = WeakReference<Context?>(baseContext)

    override fun call(): MutableMap<String, ChannelInfo> {
        Timber.d("Entered GetFollowsFromDB")

        val resultList = Service.getStreamerInfoFromDB(baseContext.get())
        Timber.d("%s streamers fetched from database", resultList.size)
        Timber.d(resultList.toString())

        if (!resultList.isEmpty()) {
            // Add the streamers to the static list field to ensure we don't waste time and resources getting the streamers from the database again.
            TempStorage.addLoadedStreamer(resultList.values)
        }

        Execute.background(twitchUserFollows)
        val duration = System.currentTimeMillis() - timerStart
        Timber.d("Completed task in ${TimeUnit.MILLISECONDS.toSeconds(duration)} seconds")

        return resultList
    }

    val isFinished: Boolean
        /**
         * @return The boolean status of the task this AsyncTask starts at the end of onPostExecute
         */
        get() = twitchUserFollows.isDone
}
