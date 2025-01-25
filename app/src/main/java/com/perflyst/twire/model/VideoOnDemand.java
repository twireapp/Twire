package com.perflyst.twire.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.github.twitch4j.helix.domain.Video;
import com.perflyst.twire.R;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

/**
 * Created by Sebastian Rask on 16-06-2016.
 */
public class VideoOnDemand implements Comparable<VideoOnDemand>, Parcelable, MainElement {
    public static final Parcelable.Creator<VideoOnDemand> CREATOR = new ClassLoaderCreator<>() {
        @Override
        public VideoOnDemand createFromParcel(Parcel source) {
            return new VideoOnDemand(source);
        }

        @Override
        public VideoOnDemand createFromParcel(Parcel source, ClassLoader loader) {
            return new VideoOnDemand(source);
        }

        @Override
        public VideoOnDemand[] newArray(int size) {
            return new VideoOnDemand[size];
        }
    };
    private final String videoTitle;
    private final String gameTitle;
    private final String previewUrl;
    private final String videoId;
    private final String channelName;
    private final String displayName;
    private final String recordedAtString;
    private final int views;
    private final long length;
    private boolean isBroadcast;
    private ZonedDateTime recordedAt;
    private ChannelInfo channelInfo;

    public VideoOnDemand(String videoTitle, String gameTitle, String previewUrl, String videoId,
                         String channelName, String displayName, int views, int length, String recordedAt) {
        this.videoTitle = videoTitle;
        this.gameTitle = gameTitle;
        this.previewUrl = previewUrl;
        this.videoId = videoId;
        this.channelName = channelName;
        this.displayName = displayName;
        this.views = views;
        this.length = length;
        this.recordedAtString = recordedAt;

        setRecordedAtDate(recordedAt);
    }

    public VideoOnDemand(Video video) {
        this.videoTitle = video.getTitle();
        this.gameTitle = "";
        this.previewUrl = video.getThumbnailUrl(320, 180);
        this.videoId = video.getId();
        this.channelName = video.getUserLogin();
        this.displayName = video.getUserName();
        this.views = video.getViewCount();
        this.length = Duration.parse("PT" + video.getDuration()).getSeconds();
        this.recordedAtString = video.getPublishedAtInstant().toString();
        this.recordedAt = video.getPublishedAtInstant().atZone(ZoneOffset.UTC);
    }

    private VideoOnDemand(Parcel in) {
        String[] data = new String[10];
        in.readStringArray(data);

        this.videoTitle = data[0];
        this.gameTitle = data[1];
        this.previewUrl = data[2];
        this.videoId = data[3];
        this.channelName = data[4];
        this.displayName = data[5];
        this.recordedAtString = data[6];
        this.views = Integer.parseInt(data[7]);
        this.length = Integer.parseInt(data[8]);
        this.isBroadcast = Boolean.parseBoolean(data[9]);
        this.channelInfo = in.readParcelable(ChannelInfo.class.getClassLoader());

        setRecordedAtDate(this.recordedAtString);
    }

    public ChannelInfo getChannelInfo() {
        return channelInfo;
    }

    public void setChannelInfo(ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
    }

    public boolean isBroadcast() {
        return isBroadcast;
    }

    public void setBroadcast(boolean broadcast) {
        isBroadcast = broadcast;
    }

    private void setRecordedAtDate(String recordedAtString) {
        try {
            this.recordedAt = ZonedDateTime.parse(recordedAtString);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String[] toSend = {
                this.videoTitle,
                this.gameTitle,
                this.previewUrl,
                this.videoId,
                this.channelName,
                this.displayName,
                this.recordedAtString,
                String.valueOf(this.views),
                String.valueOf(this.length),
                String.valueOf(this.isBroadcast)
        };

        dest.writeStringArray(toSend);
        dest.writeParcelable(channelInfo, flags);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    private String getPreviewUrl() {
        return previewUrl;
    }

    public String getVideoId() {
        return videoId;
    }

    public int getViews() {
        return views;
    }

    public long getLength() {
        return length;
    }

    public ZonedDateTime getRecordedAt() {
        return recordedAt;
    }

    @Override
    public int compareTo(@NonNull VideoOnDemand another) {
        return recordedAt.compareTo(another.recordedAt);
    }

    @Override
    public String getHighPreview() {
        return getPreviewUrl();
    }

    @Override
    public String getMediumPreview() {
        return getPreviewUrl();
    }

    @Override
    public String getLowPreview() {
        return getPreviewUrl();
    }

    @Override
    public int getPlaceHolder(Context context) {
        return R.drawable.template_stream;
    }
}
