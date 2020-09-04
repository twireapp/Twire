package com.perflyst.twire.tasks;

import android.os.AsyncTask;

import com.perflyst.twire.chat.ChatManager;

/**
 * Created by Sebastian Rask Jepsen on 21/07/16.
 */
public class SendMessageTask extends AsyncTask<Void, Void, Void> {
    private final ChatManager mBot;
    private final String message;

    public SendMessageTask(ChatManager mBot, String message) {
        this.mBot = mBot;
        this.message = message;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (mBot != null && message != null) {
            mBot.sendMessage(message);
        }
        return null;
    }
}
