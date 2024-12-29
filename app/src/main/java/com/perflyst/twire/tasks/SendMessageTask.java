package com.perflyst.twire.tasks;

import com.perflyst.twire.chat.ChatManager;

/**
 * Created by Sebastian Rask Jepsen on 21/07/16.
 */
public class SendMessageTask implements Runnable {
    private final ChatManager mBot;
    private final String message;

    public SendMessageTask(ChatManager mBot, String message) {
        this.mBot = mBot;
        this.message = message;
    }

    public void run() {
        if (mBot != null && message != null) {
            mBot.sendMessage(message);
        }
    }
}
