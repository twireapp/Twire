package com.perflyst.twire.model;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRCMessage {
    static Pattern ircPattern = Pattern.compile("(?:@(.+) )?:.+ ([A-Z]+) #\\S*(?: :(.+))?"),
            tagPattern = Pattern.compile("([^=]+)=?(.+)?");

    public Map<String, String> tags;
    public String command;
    public String content;

    public static IRCMessage parse(String message) {
        Matcher ircMatcher = ircPattern.matcher(message);
        if (!ircMatcher.matches())
            return null;

        IRCMessage ircMessage = new IRCMessage();
        ircMessage.tags = parseTags(ircMatcher.group(1));
        ircMessage.command = ircMatcher.group(2);
        ircMessage.content = ircMatcher.group(3);

        if (ircMessage.content == null) {
            ircMessage.content = "";
        }

        return ircMessage;
    }

    private static Map<String, String> parseTags(@Nullable String tagString) {
        if (tagString == null)
            return Collections.emptyMap();

        Map<String, String> replacements = new HashMap<>();
        replacements.put("\\:", ";");
        replacements.put("\\s", " ");
        replacements.put("\\\\", "\\");
        replacements.put("\\r", "\r");
        replacements.put("\\n", "\n");

        Map<String, String> tags = new HashMap<>();
        for (String tag : tagString.split(";")) {
            Matcher tagMatcher = tagPattern.matcher(tag);
            if (tagMatcher.find()) {
                String value = tagMatcher.group(2);
                if (value == null)
                    continue;

                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    value = value.replace(entry.getKey(), entry.getValue());
                }

                tags.put(tagMatcher.group(1), value);
            }
        }

        return tags;
    }
}
