package com.perflyst.twire.chat;

/*
 * Created by SebastianRask on 03-03-2016.
 */

import android.os.SystemClock;
import android.util.SparseArray;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.core.EventManager;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.philippheuer.events4j.simple.domain.EventSubscriber;
import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.chat.TwitchChatBuilder;
import com.github.twitch4j.chat.events.ChatConnectionStateEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.ClearChatEvent;
import com.github.twitch4j.chat.events.channel.DeleteMessageEvent;
import com.github.twitch4j.chat.events.channel.UserStateEvent;
import com.github.twitch4j.chat.events.roomstate.ChannelStatesEvent;
import com.github.twitch4j.client.websocket.domain.WebsocketConnectionState;
import com.github.twitch4j.helix.domain.ChatBadge;
import com.github.twitch4j.helix.domain.ChatBadgeSet;
import com.github.twitch4j.helix.domain.ChatBadgeSetList;
import com.github.twitch4j.helix.domain.NamedUserChatColor;
import com.google.common.collect.ImmutableSetMultimap;
import com.netflix.hystrix.HystrixCommand;
import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.model.Badge;
import com.perflyst.twire.model.ChatMessage;
import com.perflyst.twire.model.Emote;
import com.perflyst.twire.model.UserInfo;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.utils.Execute;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import timber.log.Timber;

public class ChatManager implements Runnable {
    public static ChatManager instance = null;

    public static ImmutableSetMultimap<String, Badge> ffzBadgeMap;
    private double currentProgress = -1;
    private String cursor = "";
    private boolean seek = false;
    private double previousProgress;
    private final OAuth2Credential account;
    private final UserInfo channel;
    private final String vodId;
    private final ChatCallback callback;
    private final ChatEmoteManager mEmoteManager;
    private Map<String, Map<String, Badge>> globalBadges = new HashMap<>();
    private Map<String, Map<String, Badge>> channelBadges = new HashMap<>();
    private final String twitchChatServer;

    private final Object vodLock = new Object();
    private double nextCommentOffset = 0;

    private boolean isStopping;
    // Data about the user and how to display his/hers message
    private String userDisplayName;
    private String userColor;
    private Map<String, String> userBadges;

    private TwitchChat twitchChat;

    public ChatManager(UserInfo aChannel, String aVodId, ChatCallback aCallback) {
        instance = this;
        mEmoteManager = new ChatEmoteManager(aChannel);

        Timber.d("Login with main Account: %s", Settings.getChatAccountConnect());

        if (Settings.isLoggedIn() && Settings.getChatAccountConnect()) { // if user is logged in ...
            // ... use their credentials
            Timber.d("Using user credentials for chat login.");

            account = TwireApplication.credential;
        } else {
            // ... else: use anonymous credentials
            Timber.d("Using anonymous credentials for chat login.");

            account = null;
        }

        channel = aChannel;
        vodId = aVodId;
        callback = aCallback;

        twitchChatServer = Settings.getChatEnableSSL() ? TwitchChat.TWITCH_WEB_SOCKET_SERVER : "ws://irc-ws.chat.twitch.tv:80";
        Timber.d("Use SSL Chat Server: %s", Settings.getChatEnableSSL());

        nextCommentOffset = 0;
    }

    public void updateVodProgress(long aCurrentProgress, boolean aSeek) {
        currentProgress = aCurrentProgress / 1000f;
        seek |= aSeek;

        // Only notify the thread when there's work to do.
        if (!aSeek && currentProgress < nextCommentOffset) return;

        synchronized (vodLock) {
            vodLock.notify();
        }
    }

    public void setPreviousProgress() {
        previousProgress = currentProgress;
        cursor = "";
    }

    @Override
    public void run() {
        isStopping = false;
        Timber.d("Trying to start chat %s", channel.getLogin());
        mEmoteManager.loadCustomEmotes(() ->
                Execute.ui(() ->
                        callback.onCustomEmoteIdFetched(mEmoteManager.getChannelCustomEmotes(), mEmoteManager.getGlobalCustomEmotes())
                )
        );

        globalBadges = readBadges(TwireApplication.helix.getGlobalChatBadges(null));
        channelBadges = readBadges(TwireApplication.helix.getChannelChatBadges(null, channel.getUserId()));
        readFFZBadges();

        if (vodId == null) {
            connect();
        } else {
            processVodChat();
        }
    }

    private void onMessage(ChatMessage message) {
        Execute.ui(() -> callback.onMessage(message));
    }

    private void onState(WebsocketConnectionState state) {
        Execute.ui(() -> callback.onConnectionChanged(state));
    }

    /**
     * Connect to twitch with the users twitch name and oauth key.
     * Joins the chat hashChannel.
     * Sends request to retrieve emote id and positions as well as username color
     * Handles parsing messages, pings and disconnects.
     * Inserts emotes, subscriber, turbo and mod drawables into messages. Also Colors the message username by the user specified color.
     * When a message has been parsed it is sent via the callback interface.
     */
    private void connect() {
        twitchChat = TwitchChatBuilder.builder()
                .withChatAccount(account)
                .withBaseUrl(twitchChatServer)
                .build();

        twitchChat.joinChannel(channel.getLogin());

        EventManager eventManager = twitchChat.getEventManager();
        eventManager.getEventHandler(SimpleEventHandler.class).registerListener(this);
    }

    private static class VODComment {
        public final double contentOffset;
        public final JSONObject data;

        private VODComment(double contentOffset, JSONObject data) {
            this.contentOffset = contentOffset;
            this.data = data;
        }
    }

    private void processVodChat() {
        try {
            synchronized (vodLock) {
                onState(WebsocketConnectionState.CONNECTED);
            }

            // Make sure that current progress has been set.
            synchronized (vodLock) {
                while (currentProgress == -1 && !isStopping) vodLock.wait();
            }

            Queue<VODComment> downloadedComments = new LinkedList<>();
            boolean reconnecting = false;
            boolean justSeeked = false;
            while (!isStopping) {
                if (seek) {
                    seek = false;
                    cursor = "";
                    downloadedComments.clear();
                    previousProgress = 0;
                    justSeeked = true;
                }

                VODComment comment = downloadedComments.peek();
                if (comment == null) {
                    JSONObject dataObject = Service.graphQL("VideoCommentsByOffsetOrCursor", "b70a3591ff0f4e0313d126c6a1502d79a1c02baebb288227c582044aa76adf6a", new HashMap<>() {{
                        put("videoID", vodId);
                        if (cursor.isEmpty()) put("contentOffsetSeconds", (int) currentProgress);
                        else put("cursor", cursor);
                    }});

                    if (dataObject == null) {
                        reconnecting = true;
                        onState(WebsocketConnectionState.RECONNECTING);
                        SystemClock.sleep(2500);
                        continue;
                    } else if (reconnecting) {
                        reconnecting = false;
                        onState(WebsocketConnectionState.CONNECTED);
                    }

                    if (dataObject.getJSONObject("video").isNull("comments")) {
                        cursor = "";
                        continue;
                    }

                    JSONObject commentsObject = dataObject.getJSONObject("video").getJSONObject("comments");
                    JSONArray comments = commentsObject.getJSONArray("edges");

                    for (int i = 0; i < comments.length(); i++) {
                        JSONObject commentJSON = comments.getJSONObject(i).getJSONObject("node");
                        int contentOffset = commentJSON.getInt("contentOffsetSeconds");
                        // Don't show previous comments and don't show comments that came before the current progress unless we just seeked.
                        if (contentOffset < previousProgress || contentOffset < currentProgress && !justSeeked)
                            continue;

                        // Sometimes the commenter is null, Twitch doesn't show them so we won't either.
                        if (commentJSON.isNull("commenter"))
                            continue;

                        downloadedComments.add(new VODComment(contentOffset, commentJSON));
                    }

                    justSeeked = false;

                    JSONObject pageInfo = commentsObject.getJSONObject("pageInfo");
                    boolean hasNextPage = pageInfo.getBoolean("hasNextPage");
                    // Assumption: If the VOD has no comments and no previous or next comments, there are no comments on the VOD.
                    if (comments.length() == 0 && !hasNextPage && !pageInfo.getBoolean("hasPreviousPage")) {
                        break;
                    }

                    if (hasNextPage) {
                        cursor = comments.getJSONObject(comments.length() - 1).getString("cursor");
                    } else if (downloadedComments.isEmpty()) {
                        // We've reached the end of the comments, nothing to do until the user seeks.
                        synchronized (vodLock) {
                            while (!seek && !isStopping) {
                                vodLock.wait();
                            }
                        }
                    }

                    comment = downloadedComments.peek();
                }

                if (seek || comment == null) {
                    continue;
                }

                nextCommentOffset = comment.contentOffset;
                synchronized (vodLock) {
                    while (currentProgress < nextCommentOffset && !seek && !isStopping)
                        vodLock.wait();
                }

                // If the user seeked, don't display this comment since it would now be an old comment.
                if (seek) continue;

                JSONObject commenter = comment.data.getJSONObject("commenter");
                JSONObject message = comment.data.getJSONObject("message");

                Map<String, String> badges = new HashMap<>();
                if (message.has("userBadges")) {
                    JSONArray userBadgesArray = message.getJSONArray("userBadges");
                    for (int j = 0; j < userBadgesArray.length(); j++) {
                        JSONObject userBadge = userBadgesArray.getJSONObject(j);
                        String setID = userBadge.getString("setID");
                        String version = userBadge.getString("version");
                        if (setID.isEmpty() || version.isEmpty()) continue;

                        badges.put(setID, version);
                    }
                }

                String color = !message.isNull("userColor") ? message.getString("userColor") : null;
                String displayName = commenter.getString("displayName");

                StringBuilder bodyBuilder = new StringBuilder();
                JSONArray fragments = message.getJSONArray("fragments");
                // Some messages have no fragments, no idea why. Twitch skips them so we will too.
                if (fragments.length() == 0) {
                    downloadedComments.poll();
                    continue;
                }

                Map<Integer, Emote> emotes = new HashMap<>();
                for (int i = 0; i < fragments.length(); i++) {
                    JSONObject fragment = fragments.getJSONObject(i);
                    String text = fragment.getString("text");

                    JSONObject emote = fragment.optJSONObject("emote");
                    if (emote != null) {
                        emotes.put(bodyBuilder.length(), Emote.Twitch(text, emote.getString("emoteID")));
                    }

                    bodyBuilder.append(text);
                }

                String body = bodyBuilder.toString();
                emotes.putAll(mEmoteManager.findCustomEmotes(body));

                //Pattern.compile(Pattern.quote(userDisplayName), Pattern.CASE_INSENSITIVE).matcher(message).find();

                ChatMessage chatMessage = new ChatMessage(body, displayName, color, getBadges(badges), emotes, false);
                onMessage(chatMessage);

                downloadedComments.poll();
            }
        } catch (Exception e) {
            e.printStackTrace();

            onState(WebsocketConnectionState.LOST);
            SystemClock.sleep(2500);
            processVodChat();
        }

        currentProgress = -1;
    }

    @EventSubscriber
    private void handleSocketState(ChatConnectionStateEvent state) {
        onState(state.getState());
    }

    /**
     * Handles the room state by notifying the chatfragment
     */
    @EventSubscriber
    private void handleRoomState(ChannelStatesEvent event) {
        Execute.ui(() -> callback.onRoomStateChange(event));
    }

    /**
     * Handles the event and saves data such as the users color, display name, and emotes
     */
    @EventSubscriber
    private void handleUserState(UserStateEvent event) {
        if (userDisplayName != null)
            return;

        userBadges = event.getMessageEvent().getBadges();
        userColor = event.getColor().orElse("");
        userDisplayName = event.getDisplayName().orElse("");
        callback.onEmoteSetsFetched(event.getEmoteSets());
    }

    /**
     * Parses and builds retrieved messages.
     * Sends build message back via callback.
     */
    @EventSubscriber
    private void handleMessage(ChannelMessageEvent message) {
        var messageEvent = message.getMessageEvent();
        Map<String, String> badges = messageEvent.getBadges();
        String displayName = message.getUser().getName();
        String color = messageEvent.getTagValue("color").orElse(randomColor(displayName));
        String content = message.getMessage();
        Map<Integer, Emote> emotes = mEmoteManager.findTwitchEmotes(messageEvent.getTagValue("emotes").orElse(""), content);
        emotes.putAll(mEmoteManager.findCustomEmotes(content));
        //Pattern.compile(Pattern.quote(userDisplayName), Pattern.CASE_INSENSITIVE).matcher(message).find();

        ChatMessage chatMessage = new ChatMessage(content, displayName, color, getBadges(badges), emotes, false);
        chatMessage.setID(message.getEventId());
        chatMessage.systemMessage = messageEvent.getTagValue("system-msg").orElse("");

        if (content.contains("@" + getUserDisplayName())) {
            Timber.d("Highlighting message with mention: %s", content);
            chatMessage.setHighlight(true);
        }

        onMessage(chatMessage);
    }

    private String randomColor(String username) {
        NamedUserChatColor[] colors = NamedUserChatColor.values();
        return colors[username.hashCode() % colors.length].getHexCode();
    }

    @EventSubscriber
    private void handleClearChat(ClearChatEvent event) {
        callback.onClear(null);
    }

    @EventSubscriber
    private void handleClearMessage(DeleteMessageEvent event) {
        callback.onClear(event.getMsgId());
    }

    /**
     * Makes the ChatManager stop retrieving messages.
     */
    public void stop() {
        isStopping = true;

        if (twitchChat != null)
            twitchChat.close();

        synchronized (vodLock) {
            vodLock.notify();
        }
    }

    /**
     * Send a message to a hashChannel on Twitch (Don't need to be on that hashChannel)
     *
     * @param message The message that will be sent
     */
    public void sendMessage(final String message) {
        twitchChat.sendMessage(channel.getLogin(), message);
    }

    private Map<String, Map<String, Badge>> readBadges(HystrixCommand<ChatBadgeSetList> request) {
        return new HashMap<>() {{
            for (ChatBadgeSet badgeSet : request.execute().getBadgeSets()) {
                put(badgeSet.getSetId(), new HashMap<>() {{
                    for (ChatBadge badge : badgeSet.getVersions()) {
                        put(badge.getId(), new Badge(
                                badgeSet.getSetId(),
                                new SparseArray<>() {{
                                    put(1, badge.getSmallImageUrl());
                                    put(2, badge.getMediumImageUrl());
                                    put(4, badge.getLargeImageUrl());
                                }})
                        );
                    }
                }});
            }
        }};
    }

    private void readFFZBadges() {
        ImmutableSetMultimap.Builder<String, Badge> mapBuilder = ImmutableSetMultimap.builder();

        try {
            JSONObject topObject = new JSONObject(Service.urlToJSONString("https://api.frankerfacez.com/v1/badges"));
            JSONArray badges = topObject.getJSONArray("badges");
            JSONObject users = topObject.getJSONObject("users");
            for (int badgeIndex = 0; badgeIndex < badges.length(); badgeIndex++) {
                JSONObject badgeJSON = badges.getJSONObject(badgeIndex);

                SparseArray<String> urls = new SparseArray<>();
                JSONObject urlsObject = badgeJSON.getJSONObject("urls");
                for (Iterator<String> iterator = urlsObject.keys(); iterator.hasNext(); ) {
                    String size = iterator.next();
                    urls.put(Integer.parseInt(size), urlsObject.getString(size));
                }

                Badge badge = new Badge(badgeJSON.getString("name"), urls, badgeJSON.getString("color"), badgeJSON.isNull("replaces") ? null : badgeJSON.getString("replaces"));
                JSONArray badgeUsers = users.getJSONArray(badgeJSON.getString("id"));
                for (int userIndex = 0; userIndex < badgeUsers.length(); userIndex++) {
                    mapBuilder.put(badgeUsers.getString(userIndex), badge);
                }
            }

            ffzBadgeMap = mapBuilder.build();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public String getUserColor() {
        return userColor;
    }

    public Map<String, String> getUserBadges() {
        return userBadges;
    }

    private Badge getBadge(String badgeSet, String version) {
        Map<String, Badge> channelSet = channelBadges.get(badgeSet);
        if (channelSet != null && channelSet.get(version) != null)
            return channelSet.get(version);

        Map<String, Badge> globalSet = globalBadges.get(badgeSet);
        if (globalSet != null && globalSet.get(version) != null)
            return globalSet.get(version);

        Timber.e("Badge failed to load: \"" + badgeSet + "\" \"" + version + "\"");
        return null;
    }

    public List<Badge> getBadges(Map<String, String> badges) {
        List<Badge> badgeObjects = new ArrayList<>();
        for (Map.Entry<String, String> entry : badges.entrySet()) {
            badgeObjects.add(getBadge(entry.getKey(), entry.getValue()));
        }

        return badgeObjects;
    }

    public interface ChatCallback {
        void onMessage(ChatMessage message);

        void onClear(String target);

        void onConnectionChanged(WebsocketConnectionState state);

        void onRoomStateChange(ChannelStatesEvent state);

        void onCustomEmoteIdFetched(List<Emote> channel, List<Emote> global);

        void onEmoteSetsFetched(List<String> emoteSets);
    }
}

