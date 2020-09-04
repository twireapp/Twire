package com.perflyst.twire.misc;

import android.content.Context;
import android.os.AsyncTask;

import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.tasks.FollowTask;
import com.perflyst.twire.tasks.UnfollowTask;

/**
 * Created by Sebastian Rask on 26-02-2017.
 */

public class FollowHandler {
    private final ChannelInfo mChannelInfo;
    private final Context mContext;
    private final Delegate mDelegate;
    private boolean isStreamerFollowed;

    public FollowHandler(ChannelInfo mChannelInfo, Context mContext, Delegate mDelegate) {
        this.mChannelInfo = mChannelInfo;
        this.mContext = mContext;
        this.mDelegate = mDelegate;
        this.isStreamerFollowed = false;

        init();
    }

    private void init() {
        if (!new Settings(mContext).isLoggedIn()) {
            mDelegate.userIsNotLoggedIn();
        } else {
            boolean isUserFollowed = Service.isUserFollowingStreamer(mChannelInfo.getStreamerName(), mContext);
            if (isUserFollowed) {
                isStreamerFollowed = true;
            }
        }
    }

    public boolean isStreamerFollowed() {
        return isStreamerFollowed;
    }

    public void followStreamer() {
        String urlString = getBaseFollowString();

        FollowTask followTask = new FollowTask(result -> {
            isStreamerFollowed = result;
            if (result) {
                Service.insertStreamerInfoToDB(mContext, mChannelInfo);
            }
        });
        followTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, urlString);
    }

    public void unfollowStreamer() {
        String url = getBaseFollowString();

        UnfollowTask unfollowTask = new UnfollowTask(result -> {
            isStreamerFollowed = !result;

            if (result) {
                Service.deleteStreamerInfoFromDB(mContext, mChannelInfo.getUserId());
            }
        });
        unfollowTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, url);
    }

    /**
     * Returns the URL string we need to connect to.
     * Both if the user wants to follow AND unfollow the current streamer
     */
    private String getBaseFollowString() {
        Settings settings = new Settings(mContext);
        return "https://api.twitch.tv/kraken/users/" + settings.getGeneralTwitchUserID() + "/follows/channels/" + mChannelInfo.getUserId() + "?oauth_token=" + settings.getGeneralTwitchAccessToken();
    }

    public interface Delegate {

        void userIsNotLoggedIn();

    }
}
