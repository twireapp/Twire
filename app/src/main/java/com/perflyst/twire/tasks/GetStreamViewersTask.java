package com.perflyst.twire.tasks;

import com.perflyst.twire.TwireApplication;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by Sebastian Rask on 17-09-2016.
 */
public class GetStreamViewersTask implements Callable<Integer> {
    private final String streamerUserId;


    public GetStreamViewersTask(String streamerUserId) {
        this.streamerUserId = streamerUserId;
    }

    public Integer call() {
        var streams = TwireApplication.helix.getStreams(null, null, null, 1, null, null, List.of(this.streamerUserId), null).execute();
        return streams.getStreams().get(0).getViewerCount();
    }
}
