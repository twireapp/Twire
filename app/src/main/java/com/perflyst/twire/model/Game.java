package com.perflyst.twire.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.perflyst.twire.R;

/**
 * Created by Sebastian Rask Jepsen on 11-08-2015.
 */
public class Game implements Comparable<Game>, MainElement, Parcelable {
    public static final Parcelable.Creator<Game> CREATOR = new ClassLoaderCreator<Game>() {
        @Override
        public Game createFromParcel(Parcel source) {
            return new Game(source);
        }

        @Override
        public Game createFromParcel(Parcel source, ClassLoader loader) {
            return new Game(source);
        }

        @Override
        public Game[] newArray(int size) {
            return new Game[size];
        }

    };
    private int gameId;
    private int gameViewers;
    private int gameStreamers;
    private final String gameTitle;
    private final String gamePreviewSmallURL;
    private final String gamePreviewMediumURL;
    private final String gamePreviewLargeURL;

    public Game(String gameTitle, int gameId, String gamePreviewSmallURL, String gamePreviewMediumURL, String gamePreviewLargeURL) {
        this(gameTitle, gameId, -1, -1, gamePreviewSmallURL, gamePreviewMediumURL, gamePreviewLargeURL);
    }

    public Game(String gameTitle, int gameId, int gameViewers, int gameStreamers, String gamePreviewSmallURL, String gamePreviewMediumURL, String gamePreviewLargeURL) {
        this.gameId = gameId;
        this.gameViewers = gameViewers;
        this.gameStreamers = gameStreamers;
        this.gameTitle = gameTitle;
        this.gamePreviewSmallURL = gamePreviewSmallURL;
        this.gamePreviewMediumURL = gamePreviewMediumURL;
        this.gamePreviewLargeURL = gamePreviewLargeURL;
    }

    public Game(Parcel parcel) {
        String[] stringData = new String[4];
        int[] intData = new int[3];
        parcel.readStringArray(stringData);
        parcel.readIntArray(intData);

        gameId = intData[0];
        gameViewers = intData[1];
        gameStreamers = intData[2];

        gameTitle = stringData[0];
        gamePreviewSmallURL = stringData[1];
        gamePreviewMediumURL = stringData[2];
        gamePreviewLargeURL = stringData[3];
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String[] toSend = {
                gameTitle,
                gamePreviewSmallURL,
                gamePreviewMediumURL,
                gamePreviewLargeURL
        };

        int[] integers = {
                gameId,
                gameViewers,
                gameStreamers
        };

        dest.writeStringArray(toSend);
        dest.writeIntArray(integers);
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public int getGameViewers() {
        return gameViewers;
    }

    public void setGameViewers(int gameViewers) {
        this.gameViewers = gameViewers;
    }

    public void setGameStreamers(int gameStreamers) {
        this.gameStreamers = gameStreamers;
    }

    private String getGamePreviewSmallURL() {
        return gamePreviewSmallURL;
    }

    private String getGamePreviewMediumURL() {
        return gamePreviewMediumURL;
    }

    private String getGamePreviewLargeURL() {
        return gamePreviewLargeURL;
    }

    public int getGameId() { return gameId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Game game = (Game) o;

        return !(gameTitle != null ? !gameTitle.equals(game.gameTitle) : game.gameTitle != null);

    }

    @Override
    public int hashCode() {
        return gameTitle != null ? gameTitle.hashCode() : 0;
    }

    @Override
    public int compareTo(Game aGame) {
        return this.getGameViewers() - aGame.getGameViewers();
    }

    @Override
    public String getHighPreview() {
        return getGamePreviewLargeURL();
    }

    @Override
    public String getMediumPreview() {
        return getGamePreviewMediumURL();
    }

    @Override
    public String getLowPreview() {
        return getGamePreviewSmallURL();
    }

    @Override
    public int getPlaceHolder(Context context) {
        return R.drawable.template_game;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
