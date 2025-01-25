package com.perflyst.twire.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.github.twitch4j.helix.domain.Stream;
import com.perflyst.twire.R;


public class StreamInfo implements Comparable<StreamInfo>, MainElement, Parcelable {
    public static final Parcelable.Creator<StreamInfo> CREATOR = new ClassLoaderCreator<>() {
        @Override
        public StreamInfo createFromParcel(Parcel parcel, ClassLoader classLoader) {
            return new StreamInfo(parcel);
        }

        @Override
        public StreamInfo createFromParcel(Parcel parcel) {
            return new StreamInfo(parcel);
        }

        @Override
        public StreamInfo[] newArray(int i) {
            return new StreamInfo[0];
        }
    };
    private final UserInfo userInfo;
    private final String[] previews;
    private final long startedAt;
    private String game;
    private int currentViewers;
    private String title;

    public StreamInfo(UserInfo userInfo, String game, int currentViewers,
                      String[] previews, long startedAt, String title) {
        this.userInfo = userInfo;
        this.game = game;
        this.currentViewers = currentViewers;
        this.previews = previews;
        this.startedAt = startedAt;
        this.title = title;
    }

    public StreamInfo(Stream stream) {
        this(
                new UserInfo(stream.getUserId(), stream.getUserLogin(), stream.getUserName()),
                stream.getGameName(),
                stream.getViewerCount(),
                new String[] {
                        stream.getThumbnailUrl(80, 45),
                        stream.getThumbnailUrl(320, 180),
                        stream.getThumbnailUrl(640, 360),
                },
                stream.getStartedAtInstant().toEpochMilli(),
                stream.getTitle()
        );
    }

    public StreamInfo(Parcel in) {
        int[] intData = in.createIntArray();
        String[] stringsData = in.createStringArray();

        if (stringsData != null && stringsData.length == 2) {
            this.game = stringsData[0];
            this.title = stringsData[1];
        }

        if (intData != null && intData.length == 1) {
            this.currentViewers = intData[0];
        }

        this.startedAt = in.readLong();
        this.previews = in.createStringArray();
        this.userInfo = in.readParcelable(StreamInfo.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String[] stringsToSend = {
                game,
                title
        };

        int[] intsToSend = {
                currentViewers,
        };

        dest.writeIntArray(intsToSend);
        dest.writeStringArray(stringsToSend);
        dest.writeLong(startedAt);
        dest.writeStringArray(previews);
        dest.writeParcelable(userInfo, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }


    public String getTitle() {
        return title;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public String getGame() {
        return game;
    }

    public int getCurrentViewers() {
        return currentViewers;
    }

    public String[] getPreviews() {
        return previews;
    }

    public long getStartedAt() {
        return startedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StreamInfo)) {
            return false;
        }

        String thisId = getUserInfo().getUserId();
        String otherId = ((StreamInfo) obj).getUserInfo().getUserId();
        return thisId.equals(otherId);
    }

    /**
     * For a Comparator that also takes priority into account, check out the private comparator in the OnlineStreamsCardAdapter
     */
    @Override
    public int compareTo(StreamInfo another) {
        return getCurrentViewers() - another.getCurrentViewers();
    }

    @Override
    @NonNull
    public String toString() {
        return this.getUserInfo().getDisplayName();
    }

    @Override
    public String getHighPreview() {
        return previews[1];
    }

    @Override
    public String getMediumPreview() {
        return previews[2];
    }

    @Override
    public String getLowPreview() {
        return previews[3];
    }

    @Override
    public int getPlaceHolder(Context context) {
        return R.drawable.template_stream;
    }
}
