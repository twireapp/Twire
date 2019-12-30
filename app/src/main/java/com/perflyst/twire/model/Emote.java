package com.perflyst.twire.model;

import android.util.SparseArray;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * Created by Sebastian Rask Jepsen on 28/07/16.
 */
public class Emote implements Comparable<Emote>, Serializable {
    private String emoteKeyword;
    private GetEmoteURL getEmoteUrl;
    private GetBestAvailableSize getBestAvailableSize = size -> size;
    private boolean isTextEmote, isSubscriberEmote, isCustomChannelEmote;

    public Emote(String emoteKeyword, GetEmoteURL getEmoteUrl) {
        this.emoteKeyword = emoteKeyword;
        this.getEmoteUrl = getEmoteUrl;
        this.isTextEmote = false;
    }

    public Emote(String textEmoteUnicode) {
        emoteKeyword = textEmoteUnicode;
        isTextEmote = true;
    }

    public boolean isCustomChannelEmote() {
        return isCustomChannelEmote;
    }

    public void setCustomChannelEmote(boolean customChannelEmote) {
        isCustomChannelEmote = customChannelEmote;
    }

    public boolean isSubscriberEmote() {
        return isSubscriberEmote;
    }

    public void setSubscriberEmote(boolean subscriberEmote) {
        isSubscriberEmote = subscriberEmote;
    }

    public String getEmoteUrl(int size) {
        return getEmoteUrl.execute(size);
    }
    public int getBestAvailableSize(int size) {
        return getBestAvailableSize.execute(size);
    }

    public boolean isTextEmote() {
        return isTextEmote;
    }

    public String getKeyword() {
        return emoteKeyword;
    }

    public interface GetEmoteURL
    {
        String execute(int size);
    }

    public interface GetBestAvailableSize
    {
        int execute(int size);
    }

    public static Emote Twitch(String keyword, String id)
    {
        return new Emote(keyword, size -> "https://static-cdn.jtvnw.net/emoticons/v1/" + id + "/" + size + ".0");
    }

    public static Emote BTTV(String keyword, String id)
    {
        return new Emote(keyword, size -> "https://cdn.betterttv.net/emote/" + id + "/" + size + "x");
    }

    public static Emote FFZ(String keyword, SparseArray<String> urlMap)
    {
        Emote emote = new Emote(keyword, size -> {
            for (int i = size; i >= 1; i--) {
                if (urlMap.indexOfKey(i) >= 0) {
                    return urlMap.get(i);
                }
            }

            return null;
        });

        emote.getBestAvailableSize = size -> {
            for (int i = size; i >= 1; i--) {
                if (urlMap.indexOfKey(i) >= 0) {
                    return i;
                }
            }

            return 1;
        };

        return emote;
    }

    @Override
    public int compareTo(@NonNull Emote emote) {
        if (this.isCustomChannelEmote() && !emote.isCustomChannelEmote()) {
            return -1;
        } else if (emote.isCustomChannelEmote() && !this.isCustomChannelEmote()) {
            return 1;
        } else {
            return this.emoteKeyword.compareTo(emote.emoteKeyword);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Emote emote = (Emote) o;
        String emoteUrl = getEmoteUrl(1);

        if (isTextEmote != emote.isTextEmote) return false;
        if (emoteUrl != null ? !emoteUrl.equals(emote.getEmoteUrl(1)) : emote.getEmoteUrl(1) != null) return false;
        return emoteKeyword != null ? emoteKeyword.equals(emote.emoteKeyword) : emote.emoteKeyword == null;
    }

    @Override
    public int hashCode() {
        String emoteUrl = getEmoteUrl(1);
        int result = emoteUrl != null ? emoteUrl.hashCode() : 0;
        result = 31 * result + (emoteKeyword != null ? emoteKeyword.hashCode() : 0);
        result = 31 * result + (isTextEmote ? 1 : 0);
        return result;
    }
}
