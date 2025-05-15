package com.perflyst.twire.service

import com.perflyst.twire.model.ChannelInfo
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by SebastianRask on 06-07-2015.
 */
object TempStorage {
    private var loaded_streamers: CopyOnWriteArrayList<ChannelInfo>? = null

    val loadedStreamers: CopyOnWriteArrayList<ChannelInfo>
        //
        get() {
            if (loaded_streamers == null) loaded_streamers =
                CopyOnWriteArrayList<ChannelInfo>()

            return loaded_streamers!!
        }

    fun addLoadedStreamer(aStreamer: ChannelInfo) {
        loadedStreamers.addIfAbsent(aStreamer)
    }

    fun addLoadedStreamer(aStreamerList: MutableCollection<ChannelInfo>) {
        loadedStreamers.addAllAbsent(aStreamerList)
    }

    fun removeLoadedStreamer(aStreamer: ChannelInfo) {
        loadedStreamers.remove(aStreamer)
    }

    fun hasLoadedStreamers(): Boolean {
        return !loadedStreamers.isEmpty()
    }

    fun containsLoadedStreamer(aStreamer: ChannelInfo): Boolean {
        return loadedStreamers.contains(aStreamer)
    }
}
