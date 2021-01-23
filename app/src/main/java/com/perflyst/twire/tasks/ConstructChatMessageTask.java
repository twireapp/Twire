package com.perflyst.twire.tasks;

import android.os.AsyncTask;

import com.perflyst.twire.chat.ChatManager;
import com.perflyst.twire.model.ChatEmote;
import com.perflyst.twire.model.ChatMessage;
import com.perflyst.twire.model.Emote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Sebastian Rask Jepsen on 07/08/16.
 */
public class ConstructChatMessageTask extends AsyncTask<Void, Void, ChatMessage> {
    private final Callback callback;
    private final List<Emote> customEmotes, twitchEmotes, subscriberEmotes;
    private final ChatManager chatManager;
    private final String message;
    private final HashMap<String, Integer> wordOccurrences;

    public ConstructChatMessageTask(Callback callback, List<Emote> customEmotes, List<Emote> twitchEmotes, List<Emote> subscriberEmotes, ChatManager chatManager, String message) {
        this.callback = callback;
        this.customEmotes = customEmotes;
        this.twitchEmotes = twitchEmotes;
        this.subscriberEmotes = subscriberEmotes;
        this.chatManager = chatManager;
        this.message = message;
        this.wordOccurrences = new HashMap<>();
    }

    @Override
    protected ChatMessage doInBackground(Void... voids) {
        try {

            return new ChatMessage(
                    message,
                    chatManager.getUserDisplayName(),
                    chatManager.getUserColor(),
                    chatManager.getBadges(chatManager.getUserBadges()),
                    getMessageChatEmotes(),
                    false
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(ChatMessage chatMessage) {
        super.onPostExecute(chatMessage);
        callback.onMessageConstructed(chatMessage);
    }

    private ArrayList<ChatEmote> getMessageChatEmotes() {
        ArrayList<ChatEmote> result = new ArrayList<>();
        List<String> words = Arrays.asList(message.split(" "));

        if (subscriberEmotes != null) {
            result.addAll(getEmotesFromList(words, subscriberEmotes));
        }

        if (twitchEmotes != null) {
            result.addAll(getEmotesFromList(words, twitchEmotes));
        }

        if (customEmotes != null) {
            result.addAll(getEmotesFromList(words, customEmotes));
        }

        return result;
    }

    private List<ChatEmote> getEmotesFromList(List<String> words, List<Emote> emotesToCheck) {
        List<ChatEmote> result = new ArrayList<>();

        for (String word : words) {
            if (emotesToCheck != null) {
                for (Emote emote : emotesToCheck) {
                    if (word.equals(emote.getKeyword())) {
                        int fromIndex = wordOccurrences.containsKey(word) ? wordOccurrences.get(word) : 0;
                        int wordIndex = message.indexOf(word, fromIndex);

                        wordOccurrences.put(word, wordIndex + word.length() - 1);

                        result.add(new ChatEmote(emote, new int[]{wordIndex}));
                    }
                }
            }
        }

        return result;
    }

    public interface Callback {
        void onMessageConstructed(ChatMessage chatMessage);
    }
}
