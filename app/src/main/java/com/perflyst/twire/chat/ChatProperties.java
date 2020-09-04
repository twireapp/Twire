package com.perflyst.twire.chat;

/*
 * Created by sebastian on 26/07/2017.
 */

import java.util.List;
import java.util.Random;

/**
 * Stores The properties of the chat. i.e. server ip's and port, if the stream is an event, if the chat is for subs only.
 */
class ChatProperties {
    private final boolean hideLinks, requireVerifiedAccount, isSubsOnly, isEvent;
    private final List<String> chatServers;
    private final Random random = new Random();

    ChatProperties(boolean hideLinks, boolean requireVerifiedAccount, boolean isSubsOnly, boolean isEvent, List<String> chatServers) {
        this.hideLinks = hideLinks;
        this.requireVerifiedAccount = requireVerifiedAccount;
        this.isSubsOnly = isSubsOnly;
        this.isEvent = isEvent;
        this.chatServers = chatServers;
    }

    String getChatIp() {
        int positions = random.nextInt(chatServers.size());
        return chatServers.get(positions);
    }

    public boolean isHideLinks() {
        return hideLinks;
    }

    public boolean isRequireVerifiedAccount() {
        return requireVerifiedAccount;
    }

    public boolean isSubsOnly() {
        return isSubsOnly;
    }

    public boolean isEvent() {
        return isEvent;
    }

    public List<String> getChatServers() {
        return chatServers;
    }
}
