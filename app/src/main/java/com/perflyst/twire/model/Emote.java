package com.perflyst.twire.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;

/**
 * Created by Sebastian Rask Jepsen on 28/07/16.
 */
public class Emote implements Comparable<Emote>, Serializable {
    private final String emoteKeyword;
    private final boolean isTextEmote;
    private String urlTemplate;
    private HashMap<Integer, String> urlMap;
    private boolean isSubscriberEmote, isCustomChannelEmote;

    public Emote(String emoteKeyword, String urlTemplate) {
        this.emoteKeyword = emoteKeyword;
        this.urlTemplate = urlTemplate;
        this.isTextEmote = false;
    }

    public Emote(String emoteKeyword, HashMap<Integer, String> urlMap) {
        this.emoteKeyword = emoteKeyword;
        this.urlMap = urlMap;
        this.isTextEmote = false;
    }

    public Emote(String textEmoteUnicode) {
        emoteKeyword = textEmoteUnicode;
        isTextEmote = true;
    }

    public static Emote Twitch(String keyword, String id) {
        return new Emote(keyword, "https://static-cdn.jtvnw.net/emoticons/v1/" + id + "/{0}.0");
    }

    public static Emote BTTV(String keyword, String id) {
        return new Emote(keyword, "https://cdn.betterttv.net/emote/" + id + "/{0}x");
    }

    public static Emote FFZ(String keyword, HashMap<Integer, String> urlMap) {
        return new Emote(keyword, urlMap);
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
        if (urlMap != null) {
            for (int i = size; i >= 1; i--) {
                if (urlMap.containsKey(i)) {
                    return urlMap.get(i);
                }
            }

            return null;
        }

        return urlTemplate == null ? null : MessageFormat.format(urlTemplate, size);
    }

    public int getBestAvailableSize(int size) {
        if (urlMap != null) {
            for (int i = size; i >= 1; i--) {
                if (urlMap.containsKey(i)) {
                    return i;
                }
            }

            return 1;
        }

        return size;
    }

    public boolean isTextEmote() {
        return isTextEmote;
    }

    public String getKeyword() {
        return emoteKeyword;
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
        if (emoteUrl != null ? !emoteUrl.equals(emote.getEmoteUrl(1)) : emote.getEmoteUrl(1) != null)
            return false;
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
