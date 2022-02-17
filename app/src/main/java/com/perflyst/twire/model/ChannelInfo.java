package com.perflyst.twire.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.google.common.base.Optional;
import com.perflyst.twire.R;
import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.service.Service;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Sebastian Rask on 30-01-2015.
 * This class is designed to hold all relevant information about a twitch user/streamer
 */
public class ChannelInfo extends UserInfo implements Comparable<ChannelInfo>, Parcelable, MainElement {
    public static final Parcelable.Creator<ChannelInfo> CREATOR = new ClassLoaderCreator<ChannelInfo>() {
        @Override
        public ChannelInfo createFromParcel(Parcel source) {
            String[] data = new String[9];

            source.readStringArray(data);
            return new ChannelInfo(
                    new UserInfo(Integer.parseInt(data[0]), data[1], data[2]),
                    data[3],
                    Integer.parseInt(data[4]),
                    Integer.parseInt(data[5]),
                    findUrl(data[6]),
                    findUrl(data[7]),
                    findUrl(data[8])
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
    private Optional<Integer> followers;
    private final int views;
    private String streamDescription;
    private URL logoURL;
    private URL videoBannerURL;
    private URL profileBannerURL;
    private Bitmap logoImage;
    private Bitmap videoBannerImage;
    private Bitmap profileBannerImage;
    private boolean notifyWhenLive;

    public ChannelInfo(UserInfo userInfo, String streamDescription, int followers, int views, URL logoURL, URL videoBannerURL, URL profileBannerURL, Boolean loadBitmap) {
        super(userInfo.getUserId(), userInfo.getLogin(), userInfo.getDisplayName());
        this.streamDescription = streamDescription;
        this.followers = followers == -1 ? Optional.absent() : Optional.of(followers);
        this.views = views;
        this.logoURL = logoURL;
        this.videoBannerURL = videoBannerURL;
        this.profileBannerURL = profileBannerURL;

        if (loadBitmap) {
            LoadBitmapTask imageTask = new LoadBitmapTask();
            imageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.logoURL, this.videoBannerURL, this.profileBannerURL);
        }
    }

    public ChannelInfo(UserInfo userInfo, String streamDescription, int followers, int views, URL logoURL, URL videoBannerURL, URL profileBannerURL) {
        super(userInfo.getUserId(), userInfo.getLogin(), userInfo.getDisplayName());
        this.streamDescription = streamDescription;
        this.followers = followers == -1 ? Optional.absent() : Optional.of(followers);
        this.views = views;
        this.logoURL = logoURL;
        this.videoBannerURL = videoBannerURL;
        this.profileBannerURL = profileBannerURL;
    }

    // Parcel Part
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Create array with values to send with intent - I think
        String[] toSend = {
                String.valueOf(this.getUserId()),
                this.getLogin(),
                this.getDisplayName(),
                this.streamDescription,
                String.valueOf(this.followers.or(-1)),
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

    public boolean equals(Object o) {
        if (getClass() != o.getClass())
            return false;

        ChannelInfo other = (ChannelInfo) o;
        return this.getUserId() == other.getUserId();

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

    public String getStreamDescription() {
        return this.streamDescription;
    }

    public void setStreamDescription(String streamDescription) {
        this.streamDescription = streamDescription;
    }

    public void getFollowers(Context context, Consumer<Optional<Integer>> callback) {
        TwireApplication.backgroundPoster.post(() -> {
            fetchFollowers(context);
            TwireApplication.uiThreadPoster.post(() -> callback.accept(followers));
        });
    }

    public Optional<Integer> fetchFollowers(Context context) {
        if (followers.isPresent()) {
            return followers;
        }

        String userFollows = Service.urlToJSONStringHelix("https://api.twitch.tv/helix/users/follows?first=1&to_id=" + getUserId(), context);

        try {
            JSONObject fullDataObject = new JSONObject(userFollows);
            followers = Optional.of(fullDataObject.getInt("total"));
            return followers;
        } catch (JSONException e) {
            e.printStackTrace();
            return Optional.absent();
        }
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
