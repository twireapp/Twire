package com.perflyst.twire.tasks;

import com.perflyst.twire.chat.ChatManager;
import com.perflyst.twire.model.ChatEmote;
import com.perflyst.twire.model.ChatMessage;
import com.perflyst.twire.model.Emote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by Sebastian Rask Jepsen on 07/08/16.
 */
public class ConstructChatMessageTask implements Callable<ChatMessage> {
    private final List<Emote> customEmotes, twitchEmotes, subscriberEmotes;
    private final ChatManager chatManager;
    private final String message;
    private final HashMap<String, Integer> wordOccurrences;

    public ConstructChatMessageTask(List<Emote> customEmotes, List<Emote> twitchEmotes, List<Emote> subscriberEmotes, ChatManager chatManager, String message) {
        this.customEmotes = customEmotes;
        this.twitchEmotes = twitchEmotes;
        this.subscriberEmotes = subscriberEmotes;
        this.chatManager = chatManager;
        this.message = message;
        this.wordOccurrences = new HashMap<>();
    }

    public ChatMessage call() {
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
}
