package com.perflyst.twire.model;


import androidx.annotation.NonNull;

import com.perflyst.twire.chat.ChatManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatMessage {
    private final String message;
    private final String name;
    private final String color;
    private final List<Badge> badges;
    private final Map<Integer, Emote> emotes;
    private boolean highlight;
    private String id = null;

    public String systemMessage = "";

    public ChatMessage(String message, String name, String color, List<Badge> badges, Map<Integer, Emote> emotes, boolean highlight) {
        this.message = message;
        this.name = name;
        this.color = color;
        this.badges = badges;
        this.emotes = emotes;
        this.highlight = highlight;

        if (ChatManager.ffzBadgeMap == null) return;

        // Load any special FFZ badges the user has
        for (Badge badge : ChatManager.ffzBadgeMap.get(name.toLowerCase())) {
            if (badge.replaces != null) {
                for (int i = 0; i < badges.size(); i++) {
                    if (badges.get(i) != null && badges.get(i).name.equals(badge.replaces)) {
                        badges.set(i, badge);
                        break;
                    }
                }
            } else {
                badges.add(badge);
            }
        }
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

    public List<Badge> getBadges() {
        return badges;
    }

    public Map<Integer, Emote> getEmotes() {
        return emotes;
    }

    public boolean isHighlight() {
        return highlight;
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    @Override
    @NonNull
    public String toString() {
        return "ChatMessage{" +
                "message='" + message + '\'' +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", badges=" + badges +
                ", emotes=" + emotes +
                '}';
    }
}
