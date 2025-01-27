package com.perflyst.twire.model;

import android.content.ContentValues;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.github.twitch4j.helix.domain.ChannelSearchResult;
import com.github.twitch4j.helix.domain.User;
import com.perflyst.twire.R;
import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.misc.Utils;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.SubscriptionsDbHelper;
import com.perflyst.twire.utils.Execute;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * Created by Sebastian Rask on 30-01-2015.
 * This class is designed to hold all relevant information about a twitch user/streamer
 */
public class ChannelInfo extends UserInfo implements Comparable<ChannelInfo>, Parcelable, MainElement {
    public static final Parcelable.Creator<ChannelInfo> CREATOR = new ClassLoaderCreator<ChannelInfo>() {
        @Override
        public ChannelInfo createFromParcel(Parcel source) {
            String[] data = new String[8];

            source.readStringArray(data);
            return new ChannelInfo(
                    new UserInfo(data[0], data[1], data[2]),
                    data[3],
                    Integer.parseInt(data[4]),
                    findUrl(data[5]),
                    findUrl(data[6]),
                    findUrl(data[7])
            );
        }

        @Override
        public ChannelInfo createFromParcel(Parcel source, ClassLoader loader) {
            return createFromParcel(source);
        }

        @Override
        public ChannelInfo[] newArray(int size) {
            return new ChannelInfo[size];
        }

        public URL findUrl(String text) {
            if (text == null) return null;

            try {
                return new URL(text);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
        }
    };
    @Nullable
    private final Integer followers;
    private String streamDescription;
    private URL logoURL;
    private final URL videoBannerURL;
    private final URL profileBannerURL;
    private boolean notifyWhenLive;

    public ChannelInfo(UserInfo userInfo, String streamDescription, int followers, URL logoURL, URL videoBannerURL, URL profileBannerURL) {
        super(userInfo.getUserId(), userInfo.getLogin(), userInfo.getDisplayName());
        this.streamDescription = streamDescription;
        this.followers = followers == -1 ? null : followers;
        this.logoURL = logoURL;
        this.videoBannerURL = videoBannerURL;
        this.profileBannerURL = profileBannerURL;
    }

    public ChannelInfo(User user) {
        super(user.getId(), user.getLogin(), user.getDisplayName());
        this.streamDescription = user.getDescription();
        this.followers = null;
        this.logoURL = Utils.safeUrl(user.getProfileImageUrl());
        this.videoBannerURL = Utils.safeUrl(user.getOfflineImageUrl());
        this.profileBannerURL = null;
    }

    public ChannelInfo(ChannelSearchResult channel) {
        super(channel.getId(), channel.getBroadcasterLogin(), channel.getDisplayName());
        this.streamDescription = "";
        this.followers = null;
        this.logoURL = null;
        this.videoBannerURL = null;
        this.profileBannerURL = null;
    }

    // Parcel Part
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // Create array with values to send with intent - I think
        String[] toSend = {
                String.valueOf(this.getUserId()),
                this.getLogin(),
                this.getDisplayName(),
                this.streamDescription,
                String.valueOf(Objects.requireNonNullElse(this.followers, -1)),
                null, //this.logoURL.toString(),
                null, //this.videoBannerURL.toString(),
                null, //this.profileBannerURL.toString()
        };

        // Only send URLS with if they are not null
        if (this.logoURL != null) {
            toSend[5] = String.valueOf(this.logoURL);
        }

        if (this.videoBannerURL != null) {
            toSend[6] = String.valueOf(this.videoBannerURL);
        }

        if (this.profileBannerURL != null) {
            toSend[7] = String.valueOf(this.profileBannerURL);
        }

        dest.writeStringArray(toSend);
    }

    // Parcel Part End


    public boolean isNotifyWhenLive() {
        return notifyWhenLive;
    }

    public void setNotifyWhenLive(boolean notifyWhenLive) {
        this.notifyWhenLive = notifyWhenLive;
    }

    public boolean equals(Object o) {
        if (getClass() != o.getClass())
            return false;

        ChannelInfo other = (ChannelInfo) o;
        return this.getUserId().equals(other.getUserId());

    }

    @Override
    public int compareTo(@NonNull ChannelInfo another) {
        return String.CASE_INSENSITIVE_ORDER.compare(another.getDisplayName(), getDisplayName());
    }

    public String getStreamDescription() {
        return this.streamDescription;
    }

    public void setStreamDescription(String streamDescription) {
        this.streamDescription = streamDescription;
    }

    public void getFollowers(Consumer<Integer> callback, int defaultValue) {
        Execute.background(this::fetchFollowers, followers -> callback.accept(Objects.requireNonNullElse(followers, defaultValue)));
    }

    @Nullable
    public Integer fetchFollowers() {
        if (followers != null) {
            return followers;
        }

        var followers = TwireApplication.helix.getChannelFollowers(null, getUserId(), null, 1, null).execute();
        return followers.getTotal();
    }

    public URL getVideoBannerURL() {
        return videoBannerURL;
    }

    public URL getLogoURL() {
        return logoURL;
    }

    public URL getProfileBannerURL() {
        return profileBannerURL;
    }

    @Override
    public String getHighPreview() {
        return getMediumPreview();
    }

    @Override
    public String getMediumPreview() {
        if (getLogoURL() == null) return null;
        return getLogoURL().toString();
    }

    @Override
    public String getLowPreview() {
        return getMediumPreview();
    }

    @Override
    public int getPlaceHolder(Context context) {
        return R.drawable.ic_profile_template_300p;
    }

    @Override
    public void refreshPreview(Context context, Runnable callback) {
        Execute.background(() -> {
            ChannelInfo mChannelInfo = Service.getStreamerInfoFromUserId(getUserId());

            if (mChannelInfo != null && logoURL != mChannelInfo.getLogoURL()) {
                logoURL = mChannelInfo.getLogoURL();
                callback.run();

                var values = new ContentValues();
                values.put(SubscriptionsDbHelper.COLUMN_LOGO_URL, logoURL.toString());
                Service.updateStreamerInfoDbWithValues(values, context, getUserId());
            }
        });
    }
}
