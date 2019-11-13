package com.perflyst.twire.model;


import java.util.List;

public class ChatMessage {
    private String message;
    private String name;
    private String color;
    private List<String> badgeUrls;
    private List<ChatEmote> emotes;
    private boolean highlight;

    public ChatMessage(String message, String name, String color, List<String> badgeUrls, List<ChatEmote> emotes, boolean highlight) {
        this.message = message;
        this.name = name;
        this.color = color;
        this.badgeUrls = badgeUrls;
        this.emotes = emotes;
        this.highlight = highlight;
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

    public List<String> getBadges() {
        return badgeUrls;
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
                ", badges=" + badgeUrls +
                ", emotes=" + emotes +
                '}';
    }
}
