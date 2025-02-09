package com.perflyst.twire;

import static org.junit.Assert.assertEquals;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.Game;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.model.UserInfo;
import com.perflyst.twire.model.VideoOnDemand;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;

public class ModelParcelableTest {
    // https://stackoverflow.com/a/56254187/8213163
    public <T extends Parcelable> T clone(@NonNull T initial) {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeParcelable(initial, 0);
            parcel.setDataPosition(0);
            return parcel.readParcelable(initial.getClass().getClassLoader());
        } finally {
            parcel.recycle();
        }
    }

    @Test
    public void channelInfo() throws MalformedURLException {
        ChannelInfo channelInfo = new ChannelInfo(
                new UserInfo("1", "login", "name"),
                "description",
                2,
                new URL("https://example.com/logo"),
                new URL("https://example.com/videoBanner"),
                new URL("https://example.com/profileBanner")
        );

        ChannelInfo newChannelInfo = clone(channelInfo);

        assertEquals(channelInfo.streamDescription, newChannelInfo.streamDescription);
        assertEquals(channelInfo.logoURL, newChannelInfo.logoURL);
        assertEquals(channelInfo.videoBannerURL, newChannelInfo.videoBannerURL);
        assertEquals(channelInfo.profileBannerURL, newChannelInfo.profileBannerURL);
        assertEquals(channelInfo.getUserId(), newChannelInfo.getUserId());
        assertEquals(channelInfo.getDisplayName(), newChannelInfo.getDisplayName());
        assertEquals(channelInfo.getLogin(), newChannelInfo.getLogin());
    }

    @Test
    public void game() {
        Game game = new Game(
                "title",
                "1",
                2,
                3,
                "https://example.com/"
        );

        Game newGame = clone(game);

        assertEquals(game.gameTitle, newGame.gameTitle);
        assertEquals(game.gameId, newGame.gameId);
        assertEquals(game.gameViewers, newGame.gameViewers);
        assertEquals(game.previewTemplate, newGame.previewTemplate);
    }

    @Test
    public void streamInfo() {
        StreamInfo streamInfo = new StreamInfo(
                new UserInfo("1", "login", "name"),
                "game",
                2,
                "preview",
                3,
                "title"
        );

        StreamInfo newStreamInfo = clone(streamInfo);

        assertEquals(streamInfo.game, newStreamInfo.game);
        assertEquals(streamInfo.currentViewers, newStreamInfo.currentViewers);
        assertEquals(streamInfo.previewTemplate, newStreamInfo.previewTemplate);
        assertEquals(streamInfo.startedAt, newStreamInfo.startedAt);
        assertEquals(streamInfo.title, newStreamInfo.title);
    }

    @Test
    public void userInfo() {
        UserInfo userInfo = new UserInfo(
                "1",
                "login",
                "name"
        );

        UserInfo newUserInfo = clone(userInfo);

        assertEquals(userInfo.getUserId(), newUserInfo.getUserId());
        assertEquals(userInfo.getLogin(), newUserInfo.getLogin());
        assertEquals(userInfo.getDisplayName(), newUserInfo.getDisplayName());
    }

    @Test
    public void videoOnDemand() {
        VideoOnDemand videoOnDemand = new VideoOnDemand(
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
        );

        VideoOnDemand newVideoOnDemand = clone(videoOnDemand);

        assertEquals(videoOnDemand.videoTitle, newVideoOnDemand.videoTitle);
        assertEquals(videoOnDemand.gameTitle, newVideoOnDemand.gameTitle);
        assertEquals(videoOnDemand.videoId, newVideoOnDemand.videoId);
        assertEquals(videoOnDemand.views, newVideoOnDemand.views);
        assertEquals(videoOnDemand.length, newVideoOnDemand.length);
        assertEquals(videoOnDemand.recordedAt, newVideoOnDemand.recordedAt);
    }
}
