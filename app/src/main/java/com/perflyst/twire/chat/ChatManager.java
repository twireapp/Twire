package com.perflyst.twire.chat;

/*
 * Created by SebastianRask on 03-03-2016.
 */

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import com.perflyst.twire.TwireApplication;
import com.google.common.collect.ImmutableSetMultimap;
import com.perflyst.twire.model.Badge;
import com.perflyst.twire.model.ChatEmote;
import com.perflyst.twire.model.ChatMessage;
import com.perflyst.twire.model.Emote;
import com.perflyst.twire.model.IRCMessage;
import com.perflyst.twire.model.UserInfo;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ChatManager implements Runnable {
    public static ChatManager instance = null;

    public static ImmutableSetMultimap<String, Badge> ffzBadgeMap;
    private static double currentProgress;
    private static String cursor = "";
    private static boolean seek = false;
    private static double previousProgress;
    private final String LOG_TAG = getClass().getSimpleName();
    private final String user;
    private final String password;
    private final UserInfo channel;
    private final String vodId;
    private final ChatCallback callback;
    private final ChatEmoteManager mEmoteManager;
    private final Map<String, Map<String, Badge>> globalBadges = new HashMap<>();
    private final Map<String, Map<String, Badge>> channelBadges = new HashMap<>();
    // Default Twitch Chat connect IP/domain and port
    private String twitchChatServer = "irc.chat.twitch.tv";
    // Port 6667 for unsecure connection | 6697 for SSL
    private int twitchChatPortunsecure = 6667;
    private int twitchChatPortsecure = 6697;
    private int twitchChatPort;

    private final Object vodLock = new Object();
    private double nextCommentOffset = 0;

    private BufferedWriter writer;
    private boolean isStopping;
    // Data about the user and how to display his/hers message
    private String userDisplayName;
    private String userColor;
    private Map<String, String> userBadges;
    // Data about room state
    private boolean chatIsR9kmode;
    private boolean chatIsSlowmode;
    private boolean chatIsSubsonlymode;

    public ChatManager(Context aContext, UserInfo aChannel, String aVodId, ChatCallback aCallback) {
        instance = this;

        Settings appSettings = new Settings(aContext);
        mEmoteManager = new ChatEmoteManager(aChannel, appSettings);

        Log.d(LOG_TAG, "Login with main Account: " + appSettings.getChatAccountConnect());

        if (appSettings.isLoggedIn() && appSettings.getChatAccountConnect()) { // if user is logged in ...
            // ... use their credentials
            Log.d(LOG_TAG, "Using user credentials for chat login.");

            user = appSettings.getGeneralTwitchName();
            password = "oauth:" + appSettings.getGeneralTwitchAccessToken();
        } else {
            // ... else: use anonymous credentials
            Log.d(LOG_TAG, "Using anonymous credentials for chat login.");

            user = "justinfan" + getRandomNumber(10000, 99999);
            password = "SCHMOOPIIE";
        }

        channel = aChannel;
        vodId = aVodId;
        callback = aCallback;

        //Set the Port Setting
        if (appSettings.getChatEnableSSL())
            twitchChatPort = twitchChatPortsecure;
        else {
            twitchChatPort = twitchChatPortunsecure;
        }
        Log.d(LOG_TAG, "Use SSL Chat Server: " + appSettings.getChatEnableSSL());
    }

    public static void updateVodProgress(int aCurrentProgress, boolean aSeek) {
        currentProgress = aCurrentProgress / 1000f;
        seek |= aSeek;

        if (instance == null) return;

        // Only notify the thread when there's work to do.
        if (!aSeek && currentProgress < instance.nextCommentOffset) return;

        synchronized (instance.vodLock) {
            instance.vodLock.notify();
        }
    }

    public static void setPreviousProgress() {
        previousProgress = currentProgress;
        cursor = "";
    }

    @Override
    public void run() {
        Log.d(LOG_TAG, "Trying to start chat " + channel.getLogin() + " for user " + user);
        mEmoteManager.loadCustomEmotes(() -> onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_CUSTOM_EMOTES_FETCHED)));

        readBadges("https://badges.twitch.tv/v1/badges/global/display", globalBadges);
        readBadges("https://badges.twitch.tv/v1/badges/channels/" + channel.getUserId() + "/display", channelBadges);
        readFFZBadges();

        if (vodId == null) {
            connect(twitchChatServer, twitchChatPort);
        } else {
            processVodChat();
        }
    }

    protected void onProgressUpdate(ProgressUpdate... values) {
        final ProgressUpdate update = values[0];
        final ProgressUpdate.UpdateType type = update.getUpdateType();
        TwireApplication.uiThreadPoster.post(() -> {
            switch (type) {
                case ON_MESSAGE:
                    callback.onMessage(update.getMessage());
                    break;
                case ON_CONNECTED:
                    callback.onConnected();
                    break;
                case ON_CONNECTING:
                    callback.onConnecting();
                    break;
                case ON_CONNECTION_FAILED:
                    callback.onConnectionFailed();
                    break;
                case ON_RECONNECTING:
                    callback.onReconnecting();
                    break;
                case ON_ROOMSTATE_CHANGE:
                    callback.onRoomstateChange(chatIsR9kmode, chatIsSlowmode, chatIsSubsonlymode);
                    break;
                case ON_CUSTOM_EMOTES_FETCHED:
                    callback.onCustomEmoteIdFetched(
                            mEmoteManager.getChannelCustomEmotes(), mEmoteManager.getGlobalCustomEmotes()
                    );
                    break;
            }
        });
    }


    /**
     * Connect to twitch with the users twitch name and oauth key.
     * Joins the chat hashChannel.
     * Sends request to retrieve emote id and positions as well as username color
     * Handles parsing messages, pings and disconnects.
     * Inserts emotes, subscriber, turbo and mod drawables into messages. Also Colors the message username by the user specified color.
     * When a message has been parsed it is sent via the callback interface.
     */
    private void connect(String address, int port) {
        try {
            Log.d("Chat connecting to", address + ":" + port);
            @SuppressWarnings("resource")
            Socket socket;
            // if we don`t use the SSL Port then create a default socket
            if (port != twitchChatPortsecure) {
                socket = new Socket(address, port);
            } else {
                // if we use the SSL Port then create a SSL Socket
                // https://stackoverflow.com/questions/13874387/create-app-with-sslsocket-java
                SSLSocketFactory factory=(SSLSocketFactory) SSLSocketFactory.getDefault();
                socket=(SSLSocket) factory.createSocket(address, port);
            }

            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.write("PASS " + password + "\r\n");
            writer.write("NICK " + user + "\r\n");
            writer.write("USER " + user + " \r\n");
            writer.flush();

            String line;
            while ((line = reader.readLine()) != null) {
                if (isStopping) {
                    leaveChannel();
                    Log.d(LOG_TAG, "Stopping chat for " + channel.getLogin());
                    break;
                }

                IRCMessage ircMessage = IRCMessage.parse(line);
                if (ircMessage != null) {
                    handleIRC(ircMessage);
                } else if (line.contains("004 " + user + " :")) {
                    Log.d(LOG_TAG, "<" + line);
                    Log.d(LOG_TAG, "Connected >> " + user + " ~ irc.twitch.tv");
                    onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_CONNECTED));
                    sendRawMessage("CAP REQ :twitch.tv/tags twitch.tv/commands");
                    sendRawMessage("JOIN #" + channel.getLogin() + "\r\n");
                } else if (line.startsWith("PING")) { // Twitch wants to know if we are still here. Send PONG and Server info back
                    handlePing(line);
                } else if (line.toLowerCase().contains("disconnected")) {
                    Log.e(LOG_TAG, "Disconnected - trying to reconnect");
                    onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_RECONNECTING));
                    connect(address, port); //ToDo: Test if chat keeps playing if connection is lost
                } else if (line.contains("NOTICE * :Error logging in")) {
                    onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_CONNECTION_FAILED));
                } else {
                    Log.d(LOG_TAG, "<" + line);
                }
            }

            // If we reach this line then the socket closed but chat wasn't stopped, so reconnect.
            connect(address, port);
        } catch (IOException e) {
            e.printStackTrace();

            onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_CONNECTION_FAILED));
            SystemClock.sleep(2500);
            onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_RECONNECTING));
            connect(address, port);
        }
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
                onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_CONNECTED));

                // Make sure that current progress has been set.
                vodLock.wait();

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
                        String result = Service.urlToJSONString("https://api.twitch.tv/v5/videos/" + vodId + "/comments?cursor=" + cursor + "&content_offset_seconds=" + currentProgress, false);

                        if (result.isEmpty()) {
                            reconnecting = true;
                            onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_RECONNECTING));
                            SystemClock.sleep(2500);
                            continue;
                        } else if (reconnecting) {
                            reconnecting = false;
                            onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_CONNECTED));
                        }

                        JSONObject commentsObject = new JSONObject(result);
                        JSONArray comments = commentsObject.getJSONArray("comments");

                        for (int i = 0; i < comments.length(); i++) {
                            JSONObject commentJSON = comments.getJSONObject(i);
                            double contentOffset = commentJSON.getDouble("content_offset_seconds");
                            // Don't show previous comments and don't show comments that came before the current progress unless we just seeked.
                            if (contentOffset < previousProgress || contentOffset < currentProgress && !justSeeked)
                                continue;

                            downloadedComments.add(new VODComment(contentOffset, commentJSON));
                        }

                        justSeeked = false;

                        // Assumption: If the VOD has no comments and no previous or next comments, there are no comments on the VOD.
                        if (comments.length() == 0 && !commentsObject.has("_next") && !commentsObject.has("_prev")) {
                            break;
                        }

                        if (commentsObject.has("_next"))
                            cursor = commentsObject.getString("_next");

                        comment = downloadedComments.peek();
                    }

                    if (seek || comment == null) {
                        continue;
                    }

                    nextCommentOffset = comment.contentOffset;
                    if (currentProgress < nextCommentOffset) vodLock.wait();

                    JSONObject commenter = comment.data.getJSONObject("commenter");
                    JSONObject message = comment.data.getJSONObject("message");

                    Map<String, String> badges = new HashMap<>();
                    if (message.has("user_badges")) {
                        JSONArray userBadgesArray = message.getJSONArray("user_badges");
                        for (int j = 0; j < userBadgesArray.length(); j++) {
                            JSONObject userBadge = userBadgesArray.getJSONObject(j);
                            badges.put(userBadge.getString("_id"), userBadge.getString("version"));
                        }
                    }

                    String color = message.has("user_color") ? message.getString("user_color") : null;
                    String displayName = commenter.getString("display_name");
                    String body = message.getString("body");

                    List<ChatEmote> emotes = new ArrayList<>();
                    if (message.has("emoticons")) {
                        JSONArray emoticonsArray = message.getJSONArray("emoticons");
                        for (int j = 0; j < emoticonsArray.length(); j++) {
                            JSONObject emoticon = emoticonsArray.getJSONObject(j);
                            int begin = emoticon.getInt("begin");
                            int end = emoticon.getInt("end") + 1;
                            // In some cases, Twitch gets the indexes of emotes wrong so we have to ignore any emotes that go over the length of the message.
                            if (end > body.length())
                                continue;

                            String keyword = body.substring(begin, end);
                            emotes.add(new ChatEmote(Emote.Twitch(keyword, emoticon.getString("_id")), new int[]{begin}));
                        }
                    }
                    emotes.addAll(mEmoteManager.findCustomEmotes(body));

                    //Pattern.compile(Pattern.quote(userDisplayName), Pattern.CASE_INSENSITIVE).matcher(message).find();

                    ChatMessage chatMessage = new ChatMessage(body, displayName, color, getBadges(badges), emotes, false);
                    onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_MESSAGE, chatMessage));

                    downloadedComments.poll();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

            onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_CONNECTION_FAILED));
            SystemClock.sleep(2500);
            onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_RECONNECTING));
            processVodChat();
        }
    }

    public void handleIRC(IRCMessage message) {
        switch (message.command) {
            case "PRIVMSG":
            case "USERNOTICE":
                handleMessage(message);
                break;
            case "USERSTATE":
                if (userDisplayName == null)
                    handleUserstate(message);
                break;
            case "ROOMSTATE":
                handleRoomstate(message);
                break;
            case "NOTICE":
                handleNotice(message);
                break;
            case "JOIN":
                break;
            default:
                Log.e(LOG_TAG, "Unhandled command type: " + message.command);
                break;
        }
    }

    private void handleNotice(IRCMessage message) {
        String msgId = message.tags.get("msg-id");
        switch (msgId) {
            case "subs_on":
                chatIsSubsonlymode = true;
                break;
            case "subs_off":
                chatIsSubsonlymode = false;
                break;
            case "slow_on":
                chatIsSlowmode = true;
                break;
            case "slow_off":
                chatIsSlowmode = false;
                break;
            case "r9k_on":
                chatIsR9kmode = true;
                break;
            case "r9k_off":
                chatIsR9kmode = false;
                break;
        }

        onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_ROOMSTATE_CHANGE));
    }

    /**
     * Parses the received line and gets the roomstate.
     * If the roomstate has changed since last check variables are changed and the chatfragment is notified
     */
    private void handleRoomstate(IRCMessage message) {
        boolean roomstateChanged = false;

        if( message.tags.get("r9k") != null) {
            chatIsR9kmode = message.tags.get("r9k").equals("1");
            roomstateChanged = true;
        }
        if( message.tags.get("slow") != null) {
            chatIsSlowmode = !message.tags.get("slow").equals("0");
            roomstateChanged = true;
        }
        if( message.tags.get("subs-only") != null) {
            chatIsSubsonlymode = message.tags.get("subs-only").equals("1");
            roomstateChanged = true;
        }

        // If the one of the roomstate types have changed notify the chatfragment
        if (roomstateChanged) {
            onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_ROOMSTATE_CHANGE));
        }
    }

    /**
     * Parses the received line and saves data such as the users color, if the user is mod, subscriber or turbouser
     */
    private void handleUserstate(IRCMessage message) {
        userBadges = new HashMap<>();
        String badgeString = message.tags.get("badges");
        if (badgeString != null && !badgeString.isEmpty()) {
            for (String badge : badgeString.split(",")) {
                String[] parts = badge.split("/");
                userBadges.put(parts[0], parts[1]);
            }
        }

        userColor = message.tags.get("color");
        userDisplayName = message.tags.get("display-name");
        callback.onEmoteSetsFetched(message.tags.get("emote-sets").split(","));
    }

    /**
     * Parses and builds retrieved messages.
     * Sends build message back via callback.
     */
    private void handleMessage(IRCMessage message) {
        Map<String, String> tags = message.tags;
        Map<String, String> badges = new HashMap<>();
        String badgesString = tags.get("badges");
        if (badgesString != null) {
            for (String badge : badgesString.split(",")) {
                String[] parts = badge.split("/");
                badges.put(parts[0], parts[1]);
            }
        }
        String color = tags.get("color");
        String displayName = tags.get("display-name");
        String content = message.content;
        List<ChatEmote> emotes = new ArrayList<>(mEmoteManager.findTwitchEmotes(message.tags.get("emotes"), content));
        emotes.addAll(mEmoteManager.findCustomEmotes(content));
        //Pattern.compile(Pattern.quote(userDisplayName), Pattern.CASE_INSENSITIVE).matcher(message).find();

        ChatMessage chatMessage = new ChatMessage(content, displayName, color, getBadges(badges), emotes, false);

        if (content.contains("@" + getUserDisplayName())) {
            Log.d(LOG_TAG, "Highlighting message with mention: " + content);
            chatMessage.setHighlight(true);
        }

        onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_MESSAGE, chatMessage));
    }

    /**
     * Sends a PONG with the connected twitch server, as specified by Twitch IRC API.
     */
    private void handlePing(String line) throws IOException {
        writer.write("PONG " + line.substring(5) + "\r\n");
        writer.flush();
    }

    /**
     * Sends an non manipulated String message to Twitch.
     */
    private void sendRawMessage(String message) {
        try {
            writer.write(message + " \r\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Makes the ChatManager stop retrieving messages.
     */
    public void stop() {
        isStopping = true;
    }

    /**
     * Send a message to a hashChannel on Twitch (Don't need to be on that hashChannel)
     *
     * @param message The message that will be sent
     */
    public void sendMessage(final String message) {
        try {
            if (writer != null) {
                writer.write("PRIVMSG #" + channel.getLogin() + " :" + message + "\r\n");
                writer.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Leaves the current hashChannel
     */
    private void leaveChannel() {
        sendRawMessage("PART #" + channel.getLogin());
    }

    private void readBadges(String url, Map<String, Map<String, Badge>> badgeMap) {
        try {
            JSONObject globalBadgeSets = new JSONObject(Service.urlToJSONString(url)).getJSONObject("badge_sets");
            for (Iterator<String> it = globalBadgeSets.keys(); it.hasNext(); ) {
                String badgeSet = it.next();
                Map<String, Badge> versionMap = new HashMap<>();

                badgeMap.put(badgeSet, versionMap);

                JSONObject versions = globalBadgeSets.getJSONObject(badgeSet).getJSONObject("versions");
                for (Iterator<String> iter = versions.keys(); iter.hasNext(); ) {
                    String version = iter.next();
                    JSONObject versionObject = versions.getJSONObject(version);
                    SparseArray<String> urls = new SparseArray<>();
                    urls.put(1, versionObject.getString("image_url_1x"));
                    urls.put(2, versionObject.getString("image_url_2x"));
                    urls.put(4, versionObject.getString("image_url_4x"));

                    versionMap.put(version, new Badge(badgeSet, urls));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
                    urls.put(Integer.parseInt(size), "https:" + urlsObject.getString(size));
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

        return null;
    }

    public List<Badge> getBadges(Map<String, String> badges) {
        List<Badge> badgeObjects = new ArrayList<>();
        for (Map.Entry<String, String> entry : badges.entrySet()) {
            badgeObjects.add(getBadge(entry.getKey(), entry.getValue()));
        }

        return badgeObjects;
    }

    private int getRandomNumber(int min, int max) {
        return new Random().nextInt(max - min + 1) + min;
    }

    public interface ChatCallback {
        void onMessage(ChatMessage message);

        void onConnecting();

        void onReconnecting();

        void onConnected();

        void onConnectionFailed();

        void onRoomstateChange(boolean isR9K, boolean isSlow, boolean isSubsOnly);

        void onCustomEmoteIdFetched(List<Emote> channel, List<Emote> global);

        void onEmoteSetsFetched(String[] emoteSets);
    }

    /**
     * Class used for determining which callback to make in the AsyncTasks OnProgressUpdate
     */
    protected static class ProgressUpdate {
        private final UpdateType updateType;
        private ChatMessage message;

        ProgressUpdate(UpdateType type) {
            updateType = type;
        }

        ProgressUpdate(UpdateType type, ChatMessage aMessage) {
            updateType = type;
            message = aMessage;
        }

        UpdateType getUpdateType() {
            return updateType;
        }

        public ChatMessage getMessage() {
            return message;
        }

        public enum UpdateType {
            ON_MESSAGE,
            ON_CONNECTING,
            ON_RECONNECTING,
            ON_CONNECTED,
            ON_CONNECTION_FAILED,
            ON_ROOMSTATE_CHANGE,
            ON_CUSTOM_EMOTES_FETCHED
        }
    }
}

