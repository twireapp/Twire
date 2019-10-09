package com.perflyst.twire.model;

/**
 * Created by SebastianRask on 03-03-2016.
 */
public class ChatEmote {
    private String[] emotePositions;
    private String emoteUrl;

    private boolean isGif = false;

    public ChatEmote(String[] emotePositions, String emoteUrl) {
        this.emotePositions = emotePositions;
        this.emoteUrl = emoteUrl;
    }

    public String getEmoteUrl() {
        return emoteUrl;
    }

    public String[] getEmotePositions() {
        return emotePositions;
    }

    public boolean isGif() {
        return isGif;
    }

    public void setGif(boolean gif) {
        isGif = gif;
    }


}
