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
        if (new Settings(mContext).isLoggedIn()) {
            isStreamerFollowed = Service.isUserFollowingStreamer(mChannelInfo.getLogin(), mContext);
            isStreamerTwitch = Service.isUserTwitch(mChannelInfo.getUserId(), mContext);
        } else {
            mDelegate.userIsNotLoggedIn();
        }
    }

    // isStreamerFollowed, isStreamerTwitch need to be checked every time, because they can change fast for example when spamming the button
    public boolean isStreamerFollowed() {
        isStreamerFollowed = Service.isUserFollowingStreamer(mChannelInfo.getLogin(), mContext);
        return isStreamerFollowed;
    }
    public boolean isStreamerTwitch() {
        isStreamerTwitch = Service.isUserTwitch(mChannelInfo.getUserId(), mContext);
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
