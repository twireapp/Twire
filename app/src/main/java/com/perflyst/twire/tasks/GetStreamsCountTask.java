package com.perflyst.twire.tasks;

import android.content.Context;

import com.google.common.collect.Lists;
import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.model.UserInfo;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

/**
 * Created by Sebastian Rask on 26-06-2016.
 */
public class GetStreamsCountTask implements Callable<Integer> {
    private final WeakReference<Context> context;

    public GetStreamsCountTask(Context context) {
        this.context = new WeakReference<>(context);
    }

    public Integer call() {
        try {
            GetFollowsFromDB subscriptionsTask = new GetFollowsFromDB(context.get());
            var follows = subscriptionsTask.call().values().stream().map(UserInfo::getUserId).toList();

            int total = 0;
            for (var chunk : Lists.partition(follows, 100)) {
                var streams = TwireApplication.helix.getStreams(null, null, null, 100, null, null, chunk, null).execute().getStreams();
                total += streams.size();
            }

            return total;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}
