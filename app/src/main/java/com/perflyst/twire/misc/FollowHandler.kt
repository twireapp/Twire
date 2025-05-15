package com.perflyst.twire.misc

import android.content.Context
import com.perflyst.twire.model.ChannelInfo
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.isLoggedIn

/**
 * Created by Sebastian Rask on 26-02-2017.
 */
class FollowHandler(
    private val mChannelInfo: ChannelInfo,
    private val mContext: Context?,
    private val mDelegate: Delegate
) {
    private var isStreamerFollowed = false
    private var isStreamerTwitch = false

    init {
        init()
    }

    private fun init() {
        if (isLoggedIn) {
            isStreamerFollowed = Service.isUserFollowingStreamer(mChannelInfo.login, mContext)
            isStreamerTwitch = Service.isUserTwitch(mChannelInfo.userId, mContext)
        } else {
            mDelegate.userIsNotLoggedIn()
        }
    }

    // isStreamerFollowed, isStreamerTwitch need to be checked every time, because they can change fast for example when spamming the button
    fun isStreamerFollowed(): Boolean {
        isStreamerFollowed = Service.isUserFollowingStreamer(mChannelInfo.login, mContext)
        return isStreamerFollowed
    }

    fun isStreamerTwitch(): Boolean {
        isStreamerTwitch = Service.isUserTwitch(mChannelInfo.userId, mContext)
        return isStreamerTwitch
    }

    fun followStreamer() {
        Service.insertStreamerInfoToDB(mContext, mChannelInfo)
    }

    fun unfollowStreamer() {
        Service.deleteStreamerInfoFromDB(mContext, mChannelInfo)
    }

    fun interface Delegate {
        fun userIsNotLoggedIn()
    }
}
