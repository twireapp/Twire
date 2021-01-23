package com.perflyst.twire.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.perflyst.twire.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Sebastian Rask on 30-01-2015.
 * This class is designed to hold all relevant information about a twitch user/streamer
 */
public class ChannelInfo implements Comparable<ChannelInfo>, Parcelable, MainElement {
    public static final Parcelable.Creator<ChannelInfo> CREATOR = new ClassLoaderCreator<ChannelInfo>() {
        @Override
        public ChannelInfo createFromParcel(Parcel source) {
            return new ChannelInfo(source);
        }

        @Override
        public ChannelInfo createFromParcel(Parcel source, ClassLoader loader) {
            return new ChannelInfo(source);
        }

        @Override
        public ChannelInfo[] newArray(int size) {
            return new ChannelInfo[size];
        }
    };
    private final int userId;
    private final String streamerName;
    private final String displayName;
    private final int followers;
    private final int views;
    private String streamDescription;
    private URL logoURL;
    private URL videoBannerURL;
    private URL profileBannerURL;
    private Bitmap logoImage;
    private Bitmap videoBannerImage;
    private Bitmap profileBannerImage;
    private boolean notifyWhenLive;

    public ChannelInfo(int userId, String streamerName, String displayName, String streamDescription, int followers, int views, URL logoURL, URL videoBannerURL, URL profileBannerURL, Boolean loadBitmap) {
        this.streamerName = streamerName;
        this.displayName = displayName;
        this.streamDescription = streamDescription;
        this.followers = followers;
        this.views = views;
        this.logoURL = logoURL;
        this.videoBannerURL = videoBannerURL;
        this.profileBannerURL = profileBannerURL;
        this.userId = userId;

        if (loadBitmap) {
            LoadBitmapTask imageTask = new LoadBitmapTask();
            imageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.logoURL, this.videoBannerURL, this.profileBannerURL);
        }
    }

    public ChannelInfo(int userId, String streamerName, String displayName, String streamDescription, int followers, int views, URL logoURL, URL videoBannerURL, URL profileBannerURL) {
        this.displayName = displayName;
        this.streamerName = streamerName;
        this.streamDescription = streamDescription;
        this.followers = followers;
        this.views = views;
        this.logoURL = logoURL;
        this.videoBannerURL = videoBannerURL;
        this.profileBannerURL = profileBannerURL;
        this.userId = userId;
    }

    // Parcel Part
    // Constructor to recreate the streamerInfo object when an activity receives it. - I think
    public ChannelInfo(Parcel in) {
        String[] data = new String[9];

        in.readStringArray(data);
        this.userId = Integer.parseInt(data[0]);
        this.streamerName = data[1];
        this.displayName = data[2];
        this.streamDescription = data[3];
        this.followers = Integer.parseInt(data[4]);
        this.views = Integer.parseInt(data[5]);
        // Test for null in URL and make sure the URLs are viable
        try {
            if (data[6] != null) {
                this.logoURL = new URL(data[6]);
            }

            if (data[7] != null) {
                this.videoBannerURL = new URL(data[7]);
            }

            if (data[8] != null) {
                this.profileBannerURL = new URL(data[8]);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Create array with values to send with intent - I think
        String[] toSend = {
                String.valueOf(this.userId),
                this.streamerName,
                this.displayName,
                this.streamDescription,
                String.valueOf(this.followers),
                String.valueOf(this.views),
                null, //this.logoURL.toString(),
                null, //this.videoBannerURL.toString(),
                null, //this.profileBannerURL.toString()
        };

        // Only send URLS with if they are not null
        if (this.logoURL != null) {
            toSend[6] = String.valueOf(this.logoURL);
        }

        if (this.videoBannerURL != null) {
            toSend[7] = String.valueOf(this.videoBannerURL);
        }

        if (this.profileBannerURL != null) {
            toSend[8] = String.valueOf(this.profileBannerURL);
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

    @NonNull
    public String toString() {
        return this.displayName;
    }

    public boolean equals(Object o) {
        if (getClass() != o.getClass())
            return false;

        ChannelInfo other = (ChannelInfo) o;
        return this.streamerName.equals(other.getStreamerName());

    }

    @Override
    public int compareTo(@NonNull ChannelInfo another) {
        return String.CASE_INSENSITIVE_ORDER.compare(another.getDisplayName(), getDisplayName());
    }

    private void setLogoImage(Bitmap logoImage) {
        this.logoImage = logoImage;
    }

    private void setVideoBannerImage(Bitmap videoBannerImage) {
        this.videoBannerImage = videoBannerImage;
    }

    private void setProfileBannerImage(Bitmap profileBannerImage) {
        this.profileBannerImage = profileBannerImage;
    }

    public int getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getStreamerName() {
        return this.streamerName;
    }

    public String getStreamDescription() {
        return this.streamDescription;
    }

    public void setStreamDescription(String streamDescription) {
        this.streamDescription = streamDescription;
    }

    public int getFollowers() {
        return followers;
    }

    public int getViews() {
        return views;
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

    /**
     * Requires 3 URLs for images. Also if one of the URLs is null.
     * This Async class loads and creates bitmaps for a StreamerInfo object if a boolean value for the constructor is true
     */
    public class LoadBitmapTask extends AsyncTask<URL, Void, ArrayList<Bitmap>> {

        @Override
        protected ArrayList<Bitmap> doInBackground(URL... params) {
            URL[] urls = {params[0], params[1], params[2]};
            ArrayList<Bitmap> bitmaps = new ArrayList<>();

            for (URL url : urls) {
                Bitmap bitmap = null;
                if (url != null) {
                    try {
                        InputStream is = (InputStream) url.getContent();
                        bitmap = BitmapFactory.decodeStream(is);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                bitmaps.add(bitmap);
            }

            return bitmaps;

        }

        protected void onPostExecute(ArrayList<Bitmap> results) {

            if (results.get(0) != null) {
                setLogoImage(results.get(0));
            }

            if (results.get(1) != null) {
                setVideoBannerImage(results.get(1));
            }

            if (results.get(2) != null) {
                setProfileBannerImage(results.get(2));
            }
        }
    }
}
