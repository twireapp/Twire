package com.sebastianrask.bettersubscription.activities;

import com.sebastianrask.bettersubscription.model.ChannelInfo;

import java.util.List;

/**
 * Created by Sebastian Rask Jepsen (SRJ@Idealdev.dk) on 10/12/16.
 */

public interface FollowingFetcher {
    void addStreamer(ChannelInfo streamer);
    void addStreamers(List<ChannelInfo> streamers);
    void showErrorView();
    boolean isEmpty();
    void notifyFinishedAdding();
}
