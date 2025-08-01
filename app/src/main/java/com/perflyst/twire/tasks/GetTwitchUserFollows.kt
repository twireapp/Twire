package com.perflyst.twire.tasks

import android.content.Context
import com.perflyst.twire.TwireApplication
import com.perflyst.twire.model.ChannelInfo
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.generalTwitchUserID
import com.perflyst.twire.service.Settings.isLoggedIn
import com.perflyst.twire.service.SubscriptionsDbHelper
import com.perflyst.twire.service.TempStorage
import com.perflyst.twire.utils.Execute
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

/**
 * Connects to Twitch to retrieve a list of streamers that a specified user follow.
 *
 *
 * Returns an ArrayList of stream names
 */
class GetTwitchUserFollows(baseContext: Context?) : Callable<List<ChannelInfo>> {
    private val timerStart = System.currentTimeMillis()
    private val baseContext: WeakReference<Context?> = WeakReference<Context?>(baseContext)

    override fun call(): List<ChannelInfo> {
        val userSubs = ArrayList<String>()

        var currentCursor: String? = ""

        if (!isLoggedIn) {
            return ArrayList()
        }

        val userId = generalTwitchUserID

        // Get all the userIds of a users follows
        do {
            val response =
                TwireApplication.helix.getFollowedChannels(null, userId, null, 100, currentCursor)
                    .execute()
            val follows = response.follows
            if (follows != null) userSubs.addAll(follows.map { it.broadcasterId }.toList())

            currentCursor = response.pagination.cursor
        } while (currentCursor != null)

        // ------- Has now loaded all the user's followed streamers ----------
        val loadedStreamerIds = ArrayList<String?>()

        SubscriptionsDbHelper(baseContext.get()).use { helper ->
            for (si in TempStorage.loadedStreamers) {
                val streamerId = si.userId
                // If the streamer was followed by the user on Twitch but is no longer followed, remove it from the database
                if (!userSubs.contains(streamerId) && Service.isUserTwitch(
                        streamerId,
                        baseContext.get()
                    )
                ) {
                    helper.writableDatabase.delete(
                        SubscriptionsDbHelper.TABLE_NAME,
                        "${SubscriptionsDbHelper.COLUMN_ID} = ?",
                        arrayOf(streamerId)
                    )
                    TempStorage.removeLoadedStreamer(si)
                    continue
                }

                // Get and save the streamerName of the already loadedStreamers
                loadedStreamerIds.add(streamerId)
            }
        }
        // Find the Twitch userIds that the app hasn't already loaded. Add it to the list of userIds that will be added to the database
        val idsToAddToDB = userSubs.filter { !loadedStreamerIds.contains(it) }

        val streamersToAddToDB = idsToAddToDB.chunked(100).flatMap {
            TwireApplication.helix.getUsers(null, it, null).execute().users
        }.map(::ChannelInfo)

        // If there are any streamers to add to the DB - Create a task and do so.
        if (!streamersToAddToDB.isEmpty()) {
            Timber.d("Starting task to add ${streamersToAddToDB.size} to the db")
            Execute.background(AddFollowsToDB(baseContext.get(), streamersToAddToDB))
        } else {
            Timber.d("Found no new streamers to add to the database")
        }

        val duration = System.currentTimeMillis() - this.timerStart
        Timber.d("Completed task in ${TimeUnit.MILLISECONDS.toSeconds(duration)} seconds")

        return streamersToAddToDB
    }
}
