package com.perflyst.twire.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.perflyst.twire.R;


public class StreamInfo implements Comparable<StreamInfo>, MainElement, Parcelable {
    public static final Parcelable.Creator<StreamInfo> CREATOR = new ClassLoaderCreator<StreamInfo>() {
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
    private ChannelInfo channelInfo;
    private String game;
    private int currentViewers;
    private String[] previews;
    private long startedAt;
    private String title;
    private int priority; // Variable only used for featured streams

    public StreamInfo(ChannelInfo channelInfo, String game, int currentViewers,
                      String[] previews, long startedAt, String title) {
        this.channelInfo = channelInfo;
        this.game = game;
        this.currentViewers = currentViewers;
        this.previews = previews;
        this.startedAt = startedAt;
        this.title = title;
        this.priority = -1;
    }

    public StreamInfo(Parcel in) {
        String[] stringsData = in.createStringArray();
        int[] intData = in.createIntArray();

        if (stringsData.length == 2) {
            this.game = stringsData[0];
            this.title = stringsData[1];
        }

        if (intData.length == 2) {
            this.currentViewers = intData[0];
            this.priority = intData[1];
        }

        this.startedAt = in.readLong();
        this.previews = in.createStringArray();
        this.channelInfo = in.readParcelable(StreamInfo.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String[] stringsToSend = new String[]{
                game,
                title
        };

        int[] intsToSend = new int[]{
                currentViewers,
                priority
        };

        dest.writeIntArray(intsToSend);
        dest.writeStringArray(stringsToSend);
        dest.writeLong(startedAt);
        dest.writeStringArray(previews);
        dest.writeParcelable(channelInfo, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ChannelInfo getChannelInfo() {
        return channelInfo;
    }

    public void setChannelInfo(ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public int getCurrentViewers() {
        return currentViewers;
    }

    public void setCurrentViewers(int currentViewers) {
        this.currentViewers = currentViewers;
    }

    public String[] getPreviews() {
        return previews;
    }

    public void setPreviews(String[] previews) {
        this.previews = previews;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public boolean isFeaturedStream() {
        return getPriority() > -1;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StreamInfo)) {
            return false;
        }

        String thisStreamerName = getChannelInfo().getStreamerName();
        String otherStreamerName = ((StreamInfo) obj).getChannelInfo().getStreamerName();
        return thisStreamerName.equals(otherStreamerName);
    }

    /**
     * For a Comparator that also takes priority into account, check out the private comparator in the OnlineStreamsCardAdapter
     */
    @Override
    public int compareTo(StreamInfo another) {
        return getCurrentViewers() - another.getCurrentViewers();
    }

    @Override
    public String toString() {
        return this.getChannelInfo().getDisplayName();
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
