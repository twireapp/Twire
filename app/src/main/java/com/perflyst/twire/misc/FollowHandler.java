package com.perflyst.twire.misc;

import android.content.Context;

import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;

/**
 * Created by Sebastian Rask on 26-02-2017.
 */

public class FollowHandler {
    private final ChannelInfo mChannelInfo;
    private final Context mContext;
    private final Delegate mDelegate;
    private boolean isStreamerFollowed,
                    isStreamerTwitch;

    public FollowHandler(ChannelInfo mChannelInfo, Context mContext, Delegate mDelegate) {
        this.mChannelInfo = mChannelInfo;
        this.mContext = mContext;
        this.mDelegate = mDelegate;
        this.isStreamerFollowed = false;
        this.isStreamerTwitch = false;

        init();
    }

    private void init() {
        if (!new Settings(mContext).isLoggedIn()) {
            //
        } else {
            boolean isUserFollowed = Service.isUserFollowingStreamer(mChannelInfo.getStreamerName(), mContext);
            boolean isUserTwitch = Service.isUserTwitch(mChannelInfo.getUserId(), mContext);
            if (isUserFollowed) {
                isStreamerFollowed = true;
            }
            if (isUserTwitch) {
                isStreamerTwitch = true;
            }
        }
    }


    // isStreamerFollowed, isStreamerTwitch need to be checked every time, because they can change fast for example when spamming the button
    public boolean isStreamerFollowed() {
        boolean isUserFollowed = Service.isUserFollowingStreamer(mChannelInfo.getStreamerName(), mContext);
        isStreamerFollowed = isUserFollowed;
        return isStreamerFollowed;
    }
    public boolean isStreamerTwitch() {
        boolean isUserTwitch = Service.isUserTwitch(mChannelInfo.getUserId(), mContext);
        isStreamerTwitch = isUserTwitch;
        return isStreamerTwitch;
    }

    public void followStreamer() {
        Service.insertStreamerInfoToDB(mContext, mChannelInfo);
    }

    public void unfollowStreamer() {
        Service.deleteStreamerInfoFromDB(mContext, mChannelInfo.getUserId());
    }

    public interface Delegate {

        void userIsNotLoggedIn();

    }
}
