package com.perflyst.twire.tasks

import com.perflyst.twire.TwireApplication
import java.util.concurrent.Callable

/**
 * Created by Sebastian Rask on 17-09-2016.
 */
class GetStreamViewersTask(private val streamerUserId: String) : Callable<Int?> {
    override fun call(): Int? {
        val streams = TwireApplication.helix.getStreams(
            null,
            null,
            null,
            1,
            null,
            null,
            listOf<String?>(this.streamerUserId),
            null
        ).execute()
        return streams.streams[0].viewerCount
    }
}
