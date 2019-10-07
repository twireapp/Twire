package com.perflyst.twire.model;


import android.graphics.Bitmap;

import java.util.List;

public class ChatMessage {
    private String message;
    private String name;
    private String color = "";
    private boolean mod;
    private boolean turbo;
    private boolean subscriber;
    private List<ChatEmote> emotes;
    private Bitmap subscriberIcon;
    private boolean highlight;

    public ChatMessage(String message, String name, String color, boolean mod, boolean turbo, boolean subscriber, List<ChatEmote> emotes, Bitmap subscriberIcon, boolean highlight) {
        this.message = message;
        this.name = name;
        this.color = color;
        this.mod = mod;
        this.turbo = turbo;
        this.subscriber = subscriber;
        this.emotes = emotes;
        this.subscriberIcon = subscriberIcon;
        this.highlight = highlight;
    }

    public Bitmap getSubscriberIcon() {
        return subscriberIcon;
    }

    public String getMessage() {
        return message;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public boolean isMod() {
        return mod;
    }

    public boolean isTurbo() {
        return turbo;
    }

    public boolean isSubscriber() {
        return subscriber;
    }

    public List<ChatEmote> getEmotes() {
        return emotes;
    }

    public boolean isHighlight() {
        return highlight;
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "message='" + message + '\'' +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", mod=" + mod +
                ", turbo=" + turbo +
                ", subscriber=" + subscriber +
                ", emotes=" + emotes +
                ", subscriberIcon=" + subscriberIcon +
                '}';
    }
}
