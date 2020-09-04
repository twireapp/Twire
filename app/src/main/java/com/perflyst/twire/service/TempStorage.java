package com.perflyst.twire.service;

import com.perflyst.twire.model.ChannelInfo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by SebastianRask on 06-07-2015.
 */
public class TempStorage {
    private static CopyOnWriteArrayList<ChannelInfo> loaded_streamers;

    //
    // For StreamerInfo Objects
    //

    public static CopyOnWriteArrayList<ChannelInfo> getLoadedStreamers() {
        if (loaded_streamers == null)
            loaded_streamers = new CopyOnWriteArrayList<>();

        return loaded_streamers;
    }

    public static void addLoadedStreamer(ChannelInfo aStreamer) {
        getLoadedStreamers().add(aStreamer);
    }

    public static void addLoadedStreamer(List<ChannelInfo> aStreamerList) {
        getLoadedStreamers().addAll(aStreamerList);
    }

    public static void removeLoadedStreamer(ChannelInfo aStreamer) {
        getLoadedStreamers().remove(aStreamer);
    }

    public static boolean hasLoadedStreamers() {
        return getLoadedStreamers().size() > 0;
    }

    public static boolean containsLoadedStreamer(ChannelInfo aStreamer) {
        return getLoadedStreamers().contains(aStreamer);
    }

}
