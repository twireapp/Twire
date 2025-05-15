package com.perflyst.twire

import android.os.Parcel
import android.os.Parcelable
import com.perflyst.twire.model.ChannelInfo
import com.perflyst.twire.model.Game
import com.perflyst.twire.model.StreamInfo
import com.perflyst.twire.model.UserInfo
import com.perflyst.twire.model.VideoOnDemand
import org.junit.Assert
import org.junit.Test
import java.net.MalformedURLException
import java.net.URL
import java.time.ZonedDateTime

class ModelParcelableTest {
    // https://stackoverflow.com/a/56254187/8213163
    fun <T : Parcelable?> clone(initial: T): T? {
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(initial, 0)
            parcel.setDataPosition(0)
            return parcel.readParcelable<T?>((initial as Any).javaClass.classLoader)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    @Throws(MalformedURLException::class)
    fun channelInfo() {
        val channelInfo = ChannelInfo(
            UserInfo("1", "login", "name"),
            "description",
            2,
            URL("https://example.com/logo"),
            URL("https://example.com/videoBanner"),
            URL("https://example.com/profileBanner")
        )

        val newChannelInfo = clone(channelInfo)!!

        Assert.assertEquals(channelInfo.streamDescription, newChannelInfo.streamDescription)
        Assert.assertEquals(channelInfo.logoURL, newChannelInfo.logoURL)
        Assert.assertEquals(channelInfo.videoBannerURL, newChannelInfo.videoBannerURL)
        Assert.assertEquals(channelInfo.profileBannerURL, newChannelInfo.profileBannerURL)
        Assert.assertEquals(channelInfo.userId, newChannelInfo.userId)
        Assert.assertEquals(channelInfo.displayName, newChannelInfo.displayName)
        Assert.assertEquals(channelInfo.login, newChannelInfo.login)
    }

    @Test
    fun game() {
        val game = Game(
            "title",
            "1",
            2,
            3,
            "https://example.com/"
        )

        val newGame = clone(game)!!

        Assert.assertEquals(game.gameTitle, newGame.gameTitle)
        Assert.assertEquals(game.gameId, newGame.gameId)
        Assert.assertEquals(game.gameViewers.toLong(), newGame.gameViewers.toLong())
        Assert.assertEquals(game.previewTemplate, newGame.previewTemplate)
    }

    @Test
    fun streamInfo() {
        val streamInfo = StreamInfo(
            UserInfo("1", "login", "name"),
            "game",
            2,
            "preview",
            3,
            "title"
        )

        val newStreamInfo = clone(streamInfo)!!

        Assert.assertEquals(streamInfo.game, newStreamInfo.game)
        Assert.assertEquals(
            streamInfo.currentViewers.toLong(),
            newStreamInfo.currentViewers.toLong()
        )
        Assert.assertEquals(streamInfo.previewTemplate, newStreamInfo.previewTemplate)
        Assert.assertEquals(streamInfo.startedAt, newStreamInfo.startedAt)
        Assert.assertEquals(streamInfo.title, newStreamInfo.title)
    }

    @Test
    fun userInfo() {
        val userInfo = UserInfo(
            "1",
            "login",
            "name"
        )

        val newUserInfo = clone(userInfo)!!

        Assert.assertEquals(userInfo.userId, newUserInfo.userId)
        Assert.assertEquals(userInfo.login, newUserInfo.login)
        Assert.assertEquals(userInfo.displayName, newUserInfo.displayName)
    }

    @Test
    fun videoOnDemand() {
        val videoOnDemand = VideoOnDemand(
            "videoTitle",
            "gameTitle",
            "https://example.com/small",
            "id",
            "channelName",
            "displayName",
            ZonedDateTime.parse("1970-01-01T00:00:00.000Z"),
            1,
            2,
            false,
            null
        )

        val newVideoOnDemand = clone(videoOnDemand)!!

        Assert.assertEquals(videoOnDemand.videoTitle, newVideoOnDemand.videoTitle)
        Assert.assertEquals(videoOnDemand.gameTitle, newVideoOnDemand.gameTitle)
        Assert.assertEquals(videoOnDemand.videoId, newVideoOnDemand.videoId)
        Assert.assertEquals(videoOnDemand.views.toLong(), newVideoOnDemand.views.toLong())
        Assert.assertEquals(videoOnDemand.length, newVideoOnDemand.length)
        Assert.assertEquals(videoOnDemand.recordedAt, newVideoOnDemand.recordedAt)
    }
}
