package com.perflyst.twire.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.perflyst.twire.R;
import com.perflyst.twire.misc.SecretKeys;
import com.perflyst.twire.model.Emote;
import com.perflyst.twire.tasks.GetLiveStreamURL;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by SebastianRask on 29-04-2015.
 */
public class Settings {
    private static final Type MAP_TYPE = new TypeToken<ArrayList<Emote>>() {
    }.getType();
    private final String GENERAL_TWITCH_ACCESS_TOKEN_KEY = "genTwitchAccessToken";
    private final String GENERAL_TWITCH_NAME_KEY = "genTwitchName";
    private final String GENERAL_TWITCH_DISPLAY_NAME_KEY = "genTwitchDisplayName";
    private final String GENERAL_TWITCH_USER_BIO = "genTwitchUserBio";
    private final String GENERAL_TWITCH_LOGO_URL = "genTwitchUserLogoUrl";
    private final String GENERAL_TWITCH_USER_MAIL = "genTwitchUserEmail";
    private final String GENERAL_TWITCH_USER_CREATED = "genTwitchUserCreatedDate";
    private final String GENERAL_TWITCH_USER_UPDATED = "genTwitchUserUpdatedDate";
    private final String GENERAL_TWITCH_USER_TYPE = "genTwitchUserType";
    private final String GENERAL_TWITCH_USER_IS_PARTNER = "genTwitchUserIsPartner";
    private final String GENERAL_TWITCH_USER_ID = "genTwitchUserID";
    private final String GENERAL_FILTER_TOP_STREAMS_LANGUAGE = "genFilterTopStreamLanguage";
    private final String NOTIFICATIONS_IS_DISABLED = "notIsDisabled";
    private final String STREAM_PLAYER_SHOW_VIEWERCOUNT = "streamPlayerShowViewerCount",
            STREAM_PLAYER_REVEAL_NAVIGATION = "streamPlayerRevealNavigation",
            STREAM_PLAYER_AUTO_PLAYBACK = "streamPlayerAutoPlackbackOnReturn";
    private final String APPEARANCE_STREAM_STYLE = "appStreamStyle";
    private final String APPEARANCE_GAME_STYLE = "appGameStyle";
    private final String APPEARANCE_FOLLOW_STYLE = "appFollowStyle";
    private final String APPEARANCE_STREAM_SIZE = "appStreamSize";
    private final String APPEARANCE_GAME_SIZE = "appGameSize";
    private final String APPEARANCE_STREAMER_SIZE = "appStreamerSize";
    private final String THEME_CHOSEN = "themeColorScheme";
    private final String STREAM_PREF_QUALITY = "streamQualPref";
    private final String STREAM_VOD_PROGRESS = "streamVodProgress";
    private final String STREAM_VOD_LENGTH = "streamVodLength";
    private final String STREAM_SLEEP_HOUR = "streamSleepHour";
    private final String STREAM_SLEEP_MINUTE = "streamSleepMinute";
    private final String SETUP_IS_SETUP = "setupIsSetup";
    private final String SETUP_IS_LOGGED_IN = "setupIsLoggedIn";
    private final String TIP_IS_SHOWN = "tipsAreShown";
    private final String GENERAL_START_PAGE = "genUserStartPage";
    private final String CHAT_EMOTE_SIZE = "chatEmoteSize";
    private final String CHAT_MESSAGE_SIZE = "chatMessageSize";
    private final String CHAT_LANDSCAPE_ENABLE = "chatLandscapeEnable";
    private final String CHAT_LANDSCAPE_SWIPEABLE = "chatLandscapeSwipable";
    private final String CHAT_LANDSCAPE_WIDTH = "chatLandscapeWidth";
    private final String CHAT_RECENT_EMOTES = "chatRecentEmotes";
    private final String CHAT_KEYBOARD_HEIGHT = "chatKeyboardHeight";
    private final String NOTIFY_LIVE = "notifyUserLive";
    private final String LAST_START_UP_VERSION_CODE = "lastStartUpVersionCode";
    private final String SHOW_CHANGELOGS = "showChangelogs";
    private final Context context;

    public Settings(Context context) {
        this.context = context;
    }

    private SharedPreferences.Editor getEditor() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.edit();
    }

    private SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private <T> void setValue(String key, T value) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(key, new Gson().toJson(value));
        editor.commit();
    }

    private <T> T getValue(String key, Class<T> type, T defaultValue) {
        SharedPreferences preferences = getPreferences();
        return preferences.contains(key) ? new Gson().fromJson(preferences.getString(key, ""), type) : defaultValue;
    }

    private <T> T getValue(Type type, T defaultValue) {
        SharedPreferences preferences = getPreferences();
        return preferences.contains("lastNotificationCheckStreamList") ? (T) new Gson().fromJson(preferences.getString("lastNotificationCheckStreamList", ""), type) : defaultValue;
    }

    public int getLastVersionCode() {
        return getValue(LAST_START_UP_VERSION_CODE, Integer.class, 0);
    }

    /***
     * Get and set the version code for last startup
     */
    public void setLastVersionCode(int code) {
        setValue(LAST_START_UP_VERSION_CODE, code);
    }

    public Boolean getShowChangelogs() {
        return getValue(SHOW_CHANGELOGS, Boolean.class, true);
    }

    public void setShowChangelogs(boolean state) {
        setValue(SHOW_CHANGELOGS, state);
    }

    public ArrayList<Integer> getUsersNotToNotifyWhenLive() {
        SharedPreferences preferences = getPreferences();
        return new Gson().fromJson(preferences.getString(this.NOTIFY_LIVE, ""), new TypeToken<ArrayList<Integer>>() {
        }.getType());
    }

    /**
     * Get/set list of users that the user have disabled notifications for.
     * This is only used for when upgrading DB
     */

    void setUsersNotToNotifyWhenLive(ArrayList<Integer> emotes) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.NOTIFY_LIVE, new Gson().toJson(emotes));
        editor.commit();
    }

    public int getKeyboardHeight() {
        SharedPreferences preferences = getPreferences();
        return preferences.getInt(CHAT_KEYBOARD_HEIGHT, 0);
    }

    /**
     * Chat - Keyboard height. Save the height of users soft keyboard
     */
    public void setKeyboardHeight(int height) {
        SharedPreferences.Editor editor = getEditor();
        editor.putInt(CHAT_KEYBOARD_HEIGHT, height);
        editor.commit();
    }

    public ArrayList<Emote> getRecentEmotes() {
        SharedPreferences preferences = getPreferences();
        return new Gson().fromJson(preferences.getString(this.CHAT_RECENT_EMOTES, ""), MAP_TYPE);
    }

    /**
     * Emotes - list of recent emotes
     */

    public void setRecentEmotes(ArrayList<Emote> emotes) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.CHAT_RECENT_EMOTES, new Gson().toJson(emotes));
        editor.commit();
    }

    public String getAppearanceGameSize() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.APPEARANCE_GAME_SIZE, context.getString(R.string.card_size_large));
    }

    /**
     * Appearance - The size of the Game Card.
     */

    public void setAppearanceGameSize(String sizeName) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.APPEARANCE_GAME_SIZE, sizeName);
        editor.commit();
    }

    public String getAppearanceStreamSize() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.APPEARANCE_STREAM_SIZE, context.getString(R.string.card_size_large));
    }

    /**
     * Appearance - The size of the Stream Card.
     */

    public void setAppearanceStreamSize(String sizeName) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.APPEARANCE_STREAM_SIZE, sizeName);
        editor.commit();
    }

    public String getAppearanceChannelSize() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.APPEARANCE_STREAMER_SIZE, context.getString(R.string.card_size_large));
    }

    /**
     * Appearance - The size of the Streamer Card.
     */

    public void setAppearanceChannelSize(String sizeName) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.APPEARANCE_STREAMER_SIZE, sizeName);
        editor.commit();
    }

    public String getAppearanceChannelStyle() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.APPEARANCE_FOLLOW_STYLE, context.getString(R.string.card_style_normal));
    }

    /**
     * Appearance - The appearance of the Streamer Card.
     */

    public void setAppearanceChannelStyle(String styleName) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.APPEARANCE_FOLLOW_STYLE, styleName);
        editor.commit();
    }

    public String getAppearanceGameStyle() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.APPEARANCE_GAME_STYLE, context.getString(R.string.card_style_normal));
    }

    /**
     * Appearance - The appearance of the Game Card.
     */

    public void setAppearanceGameStyle(String styleName) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.APPEARANCE_GAME_STYLE, styleName);
        editor.commit();
    }

    public String getAppearanceStreamStyle() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.APPEARANCE_STREAM_STYLE, context.getString(R.string.card_style_expanded));
    }

    /**
     * Appearance - The appearance of the Stream Card.
     */

    public void setAppearanceStreamStyle(String styleName) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.APPEARANCE_STREAM_STYLE, styleName);
        editor.commit();
    }

    public String getStartPage() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.GENERAL_START_PAGE, getDefaultStartUpPageTitle());
    }

    /**
     * General - The users start page. This is the page that is shown when the user starts the app
     *
     * @param startPage The title of the start page. This should be the title.
     */

    public void setStartPage(String startPage) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.GENERAL_START_PAGE, startPage);
        editor.commit();
    }

    private String getDefaultStartUpPageTitle() {
        return context.getResources().getString(R.string.navigation_drawer_my_streams_title);
    }

    public String getDefaultNotLoggedInStartUpPageTitle() {
        return context.getResources().getString(R.string.navigation_drawer_top_streams_title);
    }

    /**
     * Get the chat emote size. The result is always from 1 to 3.
     */
    public int getEmoteSize() {
        SharedPreferences preferences = getPreferences();
        return preferences.getInt(this.CHAT_EMOTE_SIZE, 2);
    }

    /**
     * Chat - Get the chats emote size.
     *
     * @param emoteSize The emote size. From 1 to 3.
     */

    public void setEmoteSize(int emoteSize) {
        SharedPreferences.Editor editor = getEditor();
        editor.putInt(this.CHAT_EMOTE_SIZE, emoteSize);
        editor.commit();
    }

    /**
     * Get the chat message size. The result is always from 1 to 3.
     */
    public int getMessageSize() {
        SharedPreferences preferences = getPreferences();
        return preferences.getInt(this.CHAT_MESSAGE_SIZE, 2);
    }

    /**
     * Chat - Get the chats message size.
     *
     * @param messageSize The message size. From 1 to 3.
     */

    public void setMessageSize(int messageSize) {
        SharedPreferences.Editor editor = getEditor();
        editor.putInt(this.CHAT_MESSAGE_SIZE, messageSize);
        editor.commit();
    }

    public int getChatLandscapeWidth() {
        SharedPreferences preferences = getPreferences();
        return preferences.getInt(this.CHAT_LANDSCAPE_WIDTH, 40);
    }

    /**
     * Chat - The chat landscape width
     *
     * @param width The chat width in landscape: From 0 to 100
     */

    public void setChatLandscapeWidth(int width) {
        SharedPreferences.Editor editor = getEditor();
        editor.putInt(this.CHAT_LANDSCAPE_WIDTH, width);
        editor.commit();
    }

    /**
     * Chat - Should the chat be able to be showed in landscape
     *
     * @param enableChat True if yes, false if no.
     */

    public void setShowChatInLandscape(boolean enableChat) {
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(this.CHAT_LANDSCAPE_SWIPEABLE, enableChat);
        editor.commit();
    }

    public boolean isChatInLandscapeEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(this.CHAT_LANDSCAPE_SWIPEABLE, true);
    }

    public boolean isChatLandscapeSwipeable() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(this.CHAT_LANDSCAPE_ENABLE, true);
    }

    /**
     * Chat - Set if the chat should be showable by swiping the VideoView
     *
     * @param enableChatSwipe True if yes, false if no.
     */

    public void setChatLandscapeSwipeable(boolean enableChatSwipe) {
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(this.CHAT_LANDSCAPE_ENABLE, enableChatSwipe);
        editor.commit();
    }

    public boolean isTipsShown() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(this.TIP_IS_SHOWN, false);
    }

    /**
     * Tool tips for showing user functionality when they start the app for the first time.
     */

    public void setTipsShown(boolean shown) {
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(this.TIP_IS_SHOWN, shown);
        editor.commit();
    }

    public String getTheme() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.THEME_CHOSEN, context.getResources().getString(R.string.blue_theme_name));
    }

    /**
     * Theme
     */

    public void setTheme(String theme) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.THEME_CHOSEN, theme);
        editor.commit();
    }

    public String getPrefStreamQuality() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.STREAM_PREF_QUALITY, GetLiveStreamURL.QUALITY_AUTO);
    }

    /**
     * Stream - Used to remember the last quality the user selected
     */

    public void setPrefStreamQuality(String quality) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.STREAM_PREF_QUALITY, quality);
        editor.commit();
    }

    /**
     * Stream VOD - Used to remember the progress of a vod
     */

    public void setVodProgress(String VODid, int progress) {
        Log.d(getClass().getSimpleName(), "Saving Current Progress: " + progress);
        SharedPreferences.Editor editor = getEditor();
        editor.putInt(this.STREAM_VOD_PROGRESS + VODid, progress);
        editor.commit();
    }

    public int getVodProgress(String VODid) {
        SharedPreferences preferences = getPreferences();
        return preferences.getInt(this.STREAM_VOD_PROGRESS + VODid, 0);
    }

    /**
     * Stream VOD - Used to remember the length of a vod
     */

    public void setVodLength(String VODid, int length) {
        Log.d(getClass().getSimpleName(), "Saving Current Progress: " + length);
        SharedPreferences.Editor editor = getEditor();
        editor.putInt(this.STREAM_VOD_LENGTH + VODid, length);
        editor.commit();
    }

    public int getVodLength(String VODid) {
        SharedPreferences preferences = getPreferences();
        return preferences.getInt(this.STREAM_VOD_LENGTH + VODid, 0);
    }

    public int getStreamSleepTimerHour() {
        SharedPreferences preferences = getPreferences();
        return preferences.getInt(this.STREAM_SLEEP_HOUR, 0);
    }

    /**
     * Stream Sleep Timer - Hour
     */

    public void setStreamSleepTimerHour(int hour) {
        SharedPreferences.Editor editor = getEditor();
        editor.putInt(this.STREAM_SLEEP_HOUR, hour);
        editor.commit();
    }

    public int getStreamSleepTimerMinute() {
        SharedPreferences preferences = getPreferences();
        return preferences.getInt(this.STREAM_SLEEP_MINUTE, 15);
    }

    /**
     * Stream Sleep Timer - Minute
     */

    public void setStreamSleepTimerMinute(int minute) {
        SharedPreferences.Editor editor = getEditor();
        editor.putInt(this.STREAM_SLEEP_MINUTE, minute);
        editor.commit();
    }

    public String getGeneralTwitchAccessToken() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.GENERAL_TWITCH_ACCESS_TOKEN_KEY, SecretKeys.NO_LOG_IN_ACCESS_TOKEN);
    }

    /**
     * General - When the user first logs in we want the user's access token
     */

    public void setGeneralTwitchAccessToken(String token) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.GENERAL_TWITCH_ACCESS_TOKEN_KEY, token);
        editor.commit();
    }

    public String getGeneralTwitchName() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.GENERAL_TWITCH_NAME_KEY, "twireapp");
    }

    /**
     * General - The user specified twitch username
     * This is the name we want to use when requesting data from twitch
     */

    public void setGeneralTwitchName(String name) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.GENERAL_TWITCH_NAME_KEY, name);
        editor.commit();
    }

    public String getGeneralTwitchDisplayName() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.GENERAL_TWITCH_DISPLAY_NAME_KEY, "TwireApp");
    }

    /**
     * General - The user specified twitch display name
     * This is the name that should be shown on screen, when we want to show the user's Twitch name
     */

    public void setGeneralTwitchDisplayName(String name) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.GENERAL_TWITCH_DISPLAY_NAME_KEY, name);
        editor.commit();
    }

    public String getGeneralTwitchUserBio() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.GENERAL_TWITCH_USER_BIO, "No biography specified");
    }

    /**
     * General - The user's biography
     */

    public void setGeneralTwitchUserBio(String aBio) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.GENERAL_TWITCH_USER_BIO, aBio);
        editor.commit();
    }

    public String getGeneralTwitchUserLogo() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.GENERAL_TWITCH_LOGO_URL, "");
    }

    /**
     * General - The user twitch logo. Will often not exist
     */

    public void setGeneralTwitchUserLogo(String aLogoURLString) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.GENERAL_TWITCH_LOGO_URL, aLogoURLString);
        editor.commit();
    }

    public String getGeneralTwitchUserEmail() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.GENERAL_TWITCH_USER_MAIL, "");
    }

    /**
     * General - The Email the user used to sign into twitch
     */

    public void setGeneralTwitchUserEmail(String aEmail) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.GENERAL_TWITCH_USER_MAIL, aEmail);
        editor.commit();
    }

    public String getGeneralTwitchUserCreatedDate() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.GENERAL_TWITCH_USER_CREATED, "");
    }

    /**
     * General - The date the user joined twitch - This is the format "2013-10-09T11:51:51Z"
     */

    public void setGeneralTwitchUserCreatedDate(String aCreatedAtDate) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.GENERAL_TWITCH_USER_CREATED, aCreatedAtDate);
        editor.commit();
    }

    public String getGeneralTwitchUserUpdatedDate() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.GENERAL_TWITCH_USER_UPDATED, "");
    }

    /**
     * General - The date the user last logged into twitch - This is the format "2013-10-09T11:51:51Z"
     */

    public void setGeneralTwitchUserUpdatedDate(String aUpdatedAtDate) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.GENERAL_TWITCH_USER_UPDATED, aUpdatedAtDate);
        editor.commit();
    }

    public String getGeneralTwitchUserType() {
        SharedPreferences preferences = getPreferences();
        return preferences.getString(this.GENERAL_TWITCH_USER_TYPE, "");
    }

    /**
     * General - The user type
     */

    public void setGeneralTwitchUserType(String aType) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(this.GENERAL_TWITCH_USER_TYPE, aType);
        editor.commit();
    }

    public boolean getGeneralTwitchUserIsPartner() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(this.GENERAL_TWITCH_USER_IS_PARTNER, false);
    }

    /**
     * General - Whether or not the user is a Twitch Partner
     */

    public void setGeneralTwitchUserIsPartner(boolean aUpdatedAtDate) {
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(this.GENERAL_TWITCH_USER_IS_PARTNER, aUpdatedAtDate);
        editor.commit();
    }

    public int getGeneralTwitchUserID() {
        SharedPreferences preferences = getPreferences();
        return preferences.getInt(this.GENERAL_TWITCH_USER_ID, 0);
    }

    /**
     * General - The user's twitch ID
     */

    public void setGeneralTwitchUserID(int aID) {
        SharedPreferences.Editor editor = getEditor();
        editor.putInt(this.GENERAL_TWITCH_USER_ID, aID);
        editor.commit();
    }

    public boolean getStreamPlayerShowViewerCount() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(this.STREAM_PLAYER_SHOW_VIEWERCOUNT, true);
    }

    /**
     * Stream Player -
     */

    public void setStreamPlayerShowViewerCount(boolean showViewCount) {
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(this.STREAM_PLAYER_SHOW_VIEWERCOUNT, showViewCount);
        editor.commit();
    }

    public boolean getGeneralFilterTopStreamsByLanguage() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(this.GENERAL_FILTER_TOP_STREAMS_LANGUAGE, true);
    }

    /**
     * General - Filter top streams by language
     */

    public void setGeneralFilterTopStreamsByLanguage(boolean filterTopStreamsByLanguage) {
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(this.GENERAL_FILTER_TOP_STREAMS_LANGUAGE, filterTopStreamsByLanguage);
        editor.commit();
    }

    public boolean getStreamPlayerShowNavigationBar() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(this.STREAM_PLAYER_REVEAL_NAVIGATION, false);
    }

    /**
     * Stream Player -
     */

    public void setStreamPlayerShowNavigationBar(boolean showNavigationBar) {
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(this.STREAM_PLAYER_REVEAL_NAVIGATION, showNavigationBar);
        editor.commit();
    }

    public boolean getStreamPlayerAutoContinuePlaybackOnReturn() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(this.STREAM_PLAYER_AUTO_PLAYBACK, false);
    }

    /**
     * Stream Player -
     */

    public void setStreamPlayerAutoContinuePlaybackOnReturn(boolean autoPlayback) {
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(this.STREAM_PLAYER_AUTO_PLAYBACK, autoPlayback);
        editor.commit();
    }

    public boolean isNotificationsDisabled() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(this.NOTIFICATIONS_IS_DISABLED, false);
    }

    public boolean isSetup() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(this.SETUP_IS_SETUP, false);
    }

    /**
     * Setup
     */

    public void setSetup(boolean isSetup) {
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(this.SETUP_IS_SETUP, isSetup);
        editor.commit();
    }

    /**
     * Setup - Has the user logged in?
     */

    public void setLogin(boolean isLoggedIn) {
        if (!isLoggedIn) {
            Service.clearStreamerInfoDb(context);
        }
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(this.SETUP_IS_LOGGED_IN, isLoggedIn);
        editor.commit();
    }

    public boolean isLoggedIn() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(this.SETUP_IS_LOGGED_IN, false);
    }

    public Context getContext() {
        return context;
    }
}
