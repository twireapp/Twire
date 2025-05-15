package com.perflyst.twire.tasks

import android.content.Context
import com.perflyst.twire.TwireApplication
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.Callable

/**
 * Created by Sebastian Rask on 26-06-2016.
 */
class GetStreamsCountTask(context: Context?) : Callable<Int> {
    private val context: WeakReference<Context?> = WeakReference<Context?>(context)

    override fun call(): Int {
        try {
            val subscriptionsTask = GetFollowsFromDB(context.get())
            val follows = subscriptionsTask.call().values.map { it.userId }

            return follows.chunked(100).flatMap {
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
            }.size
        } catch (e: Exception) {
            Timber.e(e)
        }
        return -1
    }
}
