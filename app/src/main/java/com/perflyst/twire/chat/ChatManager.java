package com.perflyst.twire.chat;

/**
 * Created by SebastianRask on 03-03-2016.
 */

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import com.perflyst.twire.model.Badge;
import com.perflyst.twire.model.ChatEmote;
import com.perflyst.twire.model.ChatMessage;
import com.perflyst.twire.model.Emote;
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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatManager extends AsyncTask<Void, ChatManager.ProgressUpdate, Void> {
    public static final int VOD_LOADING = -1;
    private static double currentProgress;
    private static String cursor = "";
    private static boolean seek = false;
    private final String LOG_TAG = getClass().getSimpleName();
    private Pattern roomstatePattern = Pattern.compile("@.*r9k=(0|1);.*slow=(0|\\d+);subs-only=(0|1)"),
            userStatePattern = Pattern.compile("badges=(.*);color=(#?\\w*);display-name=(.+);emote-sets=(.+);mod="),
            stdVarPattern = Pattern.compile("badges=(.*);color=(#?\\w*);display-name=(\\w+).* PRIVMSG #\\S* :(.*)"),
            noticePattern = Pattern.compile("@.*msg-id=(\\w*)");
    // Default Twitch Chat connect IP/domain and port
    private String twitchChatServer = "irc.twitch.tv";
    private int twitchChatPort = 6667;
    private BufferedWriter writer;
    private Handler callbackHandler;
    private boolean isStopping;
    private String user;
    private String password;
    private String channelName;
    private String hashChannel;
    private int channelUserId;
    private String vodId;
    private ChatCallback callback;
    // Data about the user and how to display his/hers message
    private String userDisplayName;
    private String userColor;
    private Map<String, String> userBadges;
    // Data about room state
    private boolean chatIsR9kmode;
    private boolean chatIsSlowmode;
    private boolean chatIsSubsonlymode;
    private ChatEmoteManager mEmoteManager;

    private Map<String, Map<String, Badge>> globalBadges = new HashMap<>();
    private Map<String, Map<String, Badge>> channelBadges = new HashMap<>();
    public static List<Badge> ffzBadges = new ArrayList<>();

    public ChatManager(Context aContext, String aChannel, int aChannelUserId, String aVodId, ChatCallback aCallback) {
        mEmoteManager = new ChatEmoteManager(aChannel);
        Settings appSettings = new Settings(aContext);

        if(appSettings.isLoggedIn()) { // if user is logged in ...
            // ... use their credentials
            Log.d(LOG_TAG, "Using user credentials for chat login.");

            user = appSettings.getGeneralTwitchName();
            password = "oauth:" + appSettings.getGeneralTwitchAccessToken();
        }
        else
        {
            // ... else: use anonymous credentials
            Log.d(LOG_TAG, "Using anonymous credentials for chat login.");

            user = "justinfan" + getRandomNumber(10000, 99999);
            password = "SCHMOOPIIE";
        }

        hashChannel = "#" + aChannel;
        channelName = aChannel;
        channelUserId = aChannelUserId;
        vodId = aVodId;
        callback = aCallback;

        executeOnExecutor(THREAD_POOL_EXECUTOR);
    }

    public static void updateVodProgress(int aCurrentProgress, boolean aSeek) {
        currentProgress = aCurrentProgress / 1000;
        seek = aSeek;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        callbackHandler = new Handler();
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.d(LOG_TAG, "Trying to start chat " + hashChannel + " for user " + user);
        mEmoteManager.loadCustomEmotes(() -> onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_CUSTOM_EMOTES_FETCHED)));

        readBadges("https://badges.twitch.tv/v1/badges/global/display", globalBadges);
        readBadges("https://badges.twitch.tv/v1/badges/channels/" + channelUserId + "/display", channelBadges);
        readFFZBadges();

        if (vodId == null) {
            ChatProperties properties = fetchChatProperties();
            if (properties != null) {
                String ipAndPort = properties.getChatIp();
                String[] ipAndPortArr = ipAndPort.split(":");
                twitchChatServer = ipAndPortArr[0];
                twitchChatPort = Integer.parseInt(ipAndPortArr[1]);
            }

            connect(twitchChatServer, twitchChatPort);
        } else {
            processVodChat();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(ProgressUpdate... values) {
        super.onProgressUpdate(values);
        final ProgressUpdate update = values[0];
        final ProgressUpdate.UpdateType type = update.getUpdateType();
        callbackHandler.post(() -> {
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

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.d(LOG_TAG, "Finished executing - Ending chat");
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
            @SuppressWarnings("resource")
            Socket socket = new Socket(address, port);
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
                    Log.d(LOG_TAG, "Stopping chat for " + channelName);
                    break;
                }

                if (line.contains("004 " + user + " :")) {
                    Log.d(LOG_TAG, "<" + line);
                    Log.d(LOG_TAG, "Connected >> " + user + " ~ irc.twitch.tv");
                    onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_CONNECTED));
                    sendRawMessage("CAP REQ :twitch.tv/tags twitch.tv/commands");
                    sendRawMessage("JOIN " + hashChannel + "\r\n");
                } else if (userDisplayName == null && line.contains("USERSTATE " + hashChannel)) {
                    handleUserstate(line);
                } else if (line.contains("ROOMSTATE " + hashChannel)) {
                    handleRoomstate(line);
                } else if (line.contains("NOTICE " + hashChannel)) {
                    handleNotice(line);
                } else if (line.startsWith("PING")) { // Twitch wants to know if we are still here. Send PONG and Server info back
                    handlePing(line);
                } else if (line.contains("PRIVMSG")) {
                    handleMessage(line);
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

        } catch (IOException e) {
            e.printStackTrace();
            onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_CONNECTION_FAILED));
        }
    }

    private void processVodChat() {
        try {
            onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_CONNECTED));

            List<JSONObject> downloadedComments = new ArrayList<>();
            boolean reconnecting = false;
            while (!isStopping) {
                if (currentProgress == VOD_LOADING) {
                    continue;
                }

                if (seek) {
                    seek = false;
                    cursor = "";
                    downloadedComments.clear();
                }

                if (downloadedComments.size() == 0) {
                    String result = Service.urlToJSONString("https://api.twitch.tv/v5/videos/" + vodId.substring(1) + "/comments?cursor=" + cursor + "&content_offset_seconds=" + currentProgress);

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
                        JSONObject comment = comments.getJSONObject(i);
                        downloadedComments.add(comment);
                    }

                    // Assumption: If the VOD has no comments and no previous or next comments, there are no comments on the VOD.
                    if (comments.length() == 0 && !commentsObject.has("_next") && !commentsObject.has("_prev")) {
                        break;
                    }

                    if (commentsObject.has("_next"))
                        cursor = commentsObject.getString("_next");
                }

                if (seek) {
                    seek = false;
                    cursor = "";
                    downloadedComments.clear();
                    continue;
                }

                for (int i = 0; i < downloadedComments.size(); i++) {
                    JSONObject comment = downloadedComments.get(i);
                    if (currentProgress >= comment.getDouble("content_offset_seconds")) {
                        JSONObject commenter = comment.getJSONObject("commenter");
                        JSONObject message = comment.getJSONObject("message");

                        Map<String, String> badges = new HashMap<>();
                        if (message.has("user_badges")) {
                            JSONArray userBadgesArray = message.getJSONArray("user_badges");
                            for (int j = 0; j < userBadgesArray.length(); j++) {
                                JSONObject userBadge = userBadgesArray.getJSONObject(j);
                                badges.put(userBadge.getString("_id"), userBadge.getString("version"));
                            }
                        }

                        String color = message.has("user_color") ? message.getString("user_color") : "";
                        String displayName = commenter.getString("display_name");
                        String body = message.getString("body");

                        List<ChatEmote> emotes = new ArrayList<>();
                        if (message.has("emoticons")) {
                            JSONArray emoticonsArray = message.getJSONArray("emoticons");
                            for (int j = 0; j < emoticonsArray.length(); j++) {
                                JSONObject emoticon = emoticonsArray.getJSONObject(j);
                                int begin = emoticon.getInt("begin");
                                String keyword = body.substring(begin, emoticon.getInt("end") + 1);
                                emotes.add(new ChatEmote(Emote.Twitch(keyword, emoticon.getString("_id")), new int[] { begin }));
                            }
                        }
                        emotes.addAll(mEmoteManager.findCustomEmotes(body));

                        //Pattern.compile(Pattern.quote(userDisplayName), Pattern.CASE_INSENSITIVE).matcher(message).find();

                        ChatMessage chatMessage = new ChatMessage(body, displayName, color, getBadges(badges), emotes, false);
                        publishProgress(new ProgressUpdate(ProgressUpdate.UpdateType.ON_MESSAGE, chatMessage));

                        downloadedComments.remove(i);
                        i--;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_CONNECTION_FAILED));
        }
    }

    private void handleNotice(String line) {
        Log.d(LOG_TAG, "Notice: " + line);
        Matcher noticeMatcher = noticePattern.matcher(line);
        if (noticeMatcher.find()) {
            String msgId = noticeMatcher.group(1);
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
        } else {
            Log.d(LOG_TAG, "Failed to find notice pattern in: \n" + line);
        }
    }

    /**
     * Parses the received line and gets the roomstate.
     * If the roomstate has changed since last check variables are changed and the chatfragment is notified
     *
     * @param line
     */
    private void handleRoomstate(String line) {
        Matcher roomstateMatcher = roomstatePattern.matcher(line);
        if (roomstateMatcher.find()) {
            boolean newR9k = roomstateMatcher.group(1).equals("1");
            boolean newSlow = !roomstateMatcher.group(2).equals("0");
            boolean newSub = roomstateMatcher.group(3).equals("1");
            // If the one of the roomstate types have changed notify the chatfragment
            if (chatIsR9kmode != newR9k || chatIsSlowmode != newSlow || chatIsSubsonlymode != newSub) {
                chatIsR9kmode = newR9k;
                chatIsSlowmode = newSlow;
                chatIsSubsonlymode = newSub;

                onProgressUpdate(new ProgressUpdate(ProgressUpdate.UpdateType.ON_ROOMSTATE_CHANGE));
            }
        } else {
            Log.d(LOG_TAG, "Failed to find roomstate pattern in: \n" + line);
        }
    }

    /**
     * Parses the received line and saves data such as the users color, if the user is mod, subscriber or turbouser
     *
     * @param line
     */
    private void handleUserstate(String line) {
        Matcher userstateMatcher = userStatePattern.matcher(line);
        if (userstateMatcher.find()) {
            userBadges = new HashMap<>();
            if (!userstateMatcher.group(1).isEmpty()) {
                for (String badge : userstateMatcher.group(1).split(",")) {
                    String[] parts = badge.split("/");
                    userBadges.put(parts[0], parts[1]);
                }
            }

            userColor = userstateMatcher.group(2);
            userDisplayName = userstateMatcher.group(3);
            String emoteSets = userstateMatcher.group(4);
        } else {
            Log.e(LOG_TAG, "Failed to find userstate pattern in: \n" + line);
        }
    }

    /**
     * Parses and builds retrieved messages.
     * Sends build message back via callback.
     *
     * @param line
     */
    private void handleMessage(String line) {
        Matcher stdVarMatcher = stdVarPattern.matcher(line);

        if (stdVarMatcher.find()) {
            Map<String, String> badges = new HashMap<>();
            if (!stdVarMatcher.group(1).isEmpty()) {
                for (String badge : stdVarMatcher.group(1).split(",")) {
                    String[] parts = badge.split("/");
                    badges.put(parts[0], parts[1]);
                }
            }
            String color = stdVarMatcher.group(2);
            String displayName = stdVarMatcher.group(3);
            String message = stdVarMatcher.group(4);
            List<ChatEmote> emotes = new ArrayList<>(mEmoteManager.findTwitchEmotes(line, message));
            emotes.addAll(mEmoteManager.findCustomEmotes(message));
            //Pattern.compile(Pattern.quote(userDisplayName), Pattern.CASE_INSENSITIVE).matcher(message).find();

            ChatMessage chatMessage = new ChatMessage(message, displayName, color, getBadges(badges), emotes, false);
            publishProgress(new ProgressUpdate(ProgressUpdate.UpdateType.ON_MESSAGE, chatMessage));
        } else {
            Log.e(LOG_TAG, "Failed to find message pattern in: \n" + line);
        }
    }

    /**
     * Sends a PONG with the connected twitch server, as specified by Twitch IRC API.
     *
     * @param line
     * @throws IOException
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
                writer.write("PRIVMSG " + hashChannel + " :" + message + "\r\n");
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
        sendRawMessage("PART " + hashChannel);
    }

    /**
     * Fetches the chat properties from Twitch.
     * Should never be called on the UI thread.
     *
     * @return
     */
    private ChatProperties fetchChatProperties() {
        final String URL = "https://api.twitch.tv/api/channels/" + channelName + "/chat_properties";
        final String HIDE_LINKS_BOOL = "hide_chat_links";
        final String REQUIRE_VERIFIED_ACC_BOOL = "require_verified_account";
        final String SUBS_ONLY_BOOL = "subsonly";
        final String EVENT_BOOL = "devchat";
        final String CLUSTER_STRING = "cluster";
        final String CHAT_SERVERS_ARRAY = "chat_servers";

        try {
            JSONObject dataObject = new JSONObject(Service.urlToJSONString(URL));
            boolean hideLinks = dataObject.getBoolean(HIDE_LINKS_BOOL);
            boolean requireVerifiedAccount = dataObject.getBoolean(REQUIRE_VERIFIED_ACC_BOOL);
            boolean subsOnly = dataObject.getBoolean(SUBS_ONLY_BOOL);
            boolean isEvent = dataObject.getBoolean(EVENT_BOOL);
            String cluster = "";//dataObject.getString(CLUSTER_STRING);
            JSONArray chatServers = dataObject.getJSONArray(CHAT_SERVERS_ARRAY);

            ArrayList<String> chatServersResult = new ArrayList<>();
            for (int i = 0; i < chatServers.length(); i++) {
                chatServersResult.add(chatServers.getString(i));
            }

            return new ChatProperties(hideLinks, requireVerifiedAccount, subsOnly, isEvent, cluster, chatServersResult);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    void readBadges(String url, Map<String, Map<String, Badge>> badgeMap) {
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

    void readFFZBadges() {
        ffzBadges.clear();

        try {
            JSONObject topObject = new JSONObject(Service.urlToJSONString("https://api.frankerfacez.com/v1/badges"));
            JSONArray badges = topObject.getJSONArray("badges");
            JSONObject users = topObject.getJSONObject("users");
            for (int badgeIndex = 0; badgeIndex < badges.length(); badgeIndex++) {
                JSONObject badge = badges.getJSONObject(badgeIndex);

                SparseArray<String> urls = new SparseArray<>();
                JSONObject urlsObject = badge.getJSONObject("urls");
                for (Iterator<String> iterator = urlsObject.keys(); iterator.hasNext(); ) {
                    String size = iterator.next();
                    urls.put(Integer.parseInt(size), "https:" + urlsObject.getString(size));
                }

                List<String> emoteUsers = new ArrayList<>();
                JSONArray badgeUsers = users.getJSONArray(Integer.toString(badgeIndex + 1));
                for (int userIndex = 0; userIndex < badgeUsers.length(); userIndex++) {
                    emoteUsers.add(badgeUsers.getString(userIndex));
                }

                ffzBadges.add(new Badge(badge.getString("name"), urls, badge.getString("color"), badge.isNull("replaces") ? null : badge.getString("replaces"), emoteUsers));
            }
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

    public Badge getBadge(String badgeSet, String version) {
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
        for (String badgeSet : badges.keySet()) {
            badgeObjects.add(getBadge(badgeSet, badges.get(badgeSet)));
        }

        return badgeObjects;
    }

    public interface ChatCallback {
        void onMessage(ChatMessage message);

        void onConnecting();

        void onReconnecting();

        void onConnected();

        void onConnectionFailed();

        void onRoomstateChange(boolean isR9K, boolean isSlow, boolean isSubsOnly);

        void onCustomEmoteIdFetched(List<Emote> channel, List<Emote> global);
    }

    /**
     * Class used for determining which callback to make in the AsyncTasks OnProgressUpdate
     */
    protected static class ProgressUpdate {
        private UpdateType updateType;
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

        public void setUpdateType(UpdateType updateType) {
            this.updateType = updateType;
        }

        public ChatMessage getMessage() {
            return message;
        }

        public void setMessage(ChatMessage message) {
            this.message = message;
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

    private int getRandomNumber(int min,int max) {
        return (new Random()).nextInt((max - min) + 1) + min;
    }
}

