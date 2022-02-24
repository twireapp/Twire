package com.perflyst.twire;

import static org.junit.Assert.assertArrayEquals;
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
                new UserInfo(1, "login", "name"),
                "description",
                2,
                3,
                new URL("https://example.com/logo"),
                new URL("https://example.com/videoBanner"),
                new URL("https://example.com/profileBanner")
        );

        ChannelInfo newChannelInfo = clone(channelInfo);

        assertEquals(channelInfo.getStreamDescription(), newChannelInfo.getStreamDescription());
        assertEquals(channelInfo.getViews(), newChannelInfo.getViews());
        assertEquals(channelInfo.getLogoURL(), newChannelInfo.getLogoURL());
        assertEquals(channelInfo.getVideoBannerURL(), newChannelInfo.getVideoBannerURL());
        assertEquals(channelInfo.getProfileBannerURL(), newChannelInfo.getProfileBannerURL());
    }

    @Test
    public void game() {
        Game game = new Game(
                "title",
                1,
                2,
                3,
                "https://example.com/small",
                "https://example.com/medium",
                "https://example.com/large"
        );

        Game newGame = clone(game);

        assertEquals(game.getGameTitle(), newGame.getGameTitle());
        assertEquals(game.getGameId(), newGame.getGameId());
        assertEquals(game.getGameViewers(), newGame.getGameViewers());
        assertEquals(game.getLowPreview(), newGame.getLowPreview());
        assertEquals(game.getMediumPreview(), newGame.getMediumPreview());
        assertEquals(game.getHighPreview(), newGame.getHighPreview());
    }

    @Test
    public void streamInfo() {
        StreamInfo streamInfo = new StreamInfo(
                new UserInfo(1, "login", "name"),
                "game",
                2,
                new String[] { "previews" },
                3,
                "title"
        );

        StreamInfo newStreamInfo = clone(streamInfo);

        assertEquals(streamInfo.getGame(), newStreamInfo.getGame());
        assertEquals(streamInfo.getCurrentViewers(), newStreamInfo.getCurrentViewers());
        assertArrayEquals(streamInfo.getPreviews(), newStreamInfo.getPreviews());
        assertEquals(streamInfo.getStartedAt(), newStreamInfo.getStartedAt());
        assertEquals(streamInfo.getTitle(), newStreamInfo.getTitle());
        assertEquals(streamInfo.getPriority(), newStreamInfo.getPriority());
    }

    @Test
    public void userInfo() {
        UserInfo userInfo = new UserInfo(
                1,
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
                1,
                2,
                "1997-01-01"
        );

        VideoOnDemand newVideoOnDemand = clone(videoOnDemand);

        assertEquals(videoOnDemand.getVideoTitle(), newVideoOnDemand.getVideoTitle());
        assertEquals(videoOnDemand.getGameTitle(), newVideoOnDemand.getGameTitle());
        assertEquals(videoOnDemand.getVideoId(), newVideoOnDemand.getVideoId());
        assertEquals(videoOnDemand.getViews(), newVideoOnDemand.getViews());
        assertEquals(videoOnDemand.getLength(), newVideoOnDemand.getLength());
        assertEquals(videoOnDemand.getRecordedAt(), newVideoOnDemand.getRecordedAt());
    }
}
