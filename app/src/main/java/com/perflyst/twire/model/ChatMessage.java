package com.perflyst.twire.model;


import androidx.annotation.NonNull;

import com.perflyst.twire.chat.ChatManager;

import java.util.List;

public class ChatMessage {
    private final String message;
    private final String name;
    private final String color;
    private final List<Badge> badges;
    private final List<ChatEmote> emotes;
    private boolean highlight;

    public ChatMessage(String message, String name, String color, List<Badge> badges, List<ChatEmote> emotes, boolean highlight) {
        this.message = message;
        this.name = name;
        this.color = color;
        this.badges = badges;
        this.emotes = emotes;
        this.highlight = highlight;

        // Load any special FFZ badges the user has
        for (Badge badge : ChatManager.ffzBadges) {
            if (badge.users.contains(name.toLowerCase())) {
                if (badge.replaces != null) {
                    for (int i = 0; i < badges.size(); i++) {
                        if (badges.get(i).name.equals(badge.replaces)) {
                            badges.set(i, badge);
                            break;
                        }
                    }
                } else {
                    badges.add(badge);
                }
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
