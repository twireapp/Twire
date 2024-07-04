package com.perflyst.twire.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.perflyst.twire.R
import com.perflyst.twire.TwireApplication
import com.perflyst.twire.misc.SecretKeys
import com.perflyst.twire.model.Emote
import com.perflyst.twire.model.Theme
import com.perflyst.twire.tasks.GetStreamURL
import timber.log.Timber
import java.lang.reflect.Type
import java.util.Arrays

/**
 * Created by SebastianRask on 29-04-2015.
 */
@SuppressLint("StaticFieldLeak")
object Settings {
    private val MAP_TYPE: Type = object : TypeToken<ArrayList<Emote>>() {
    }.type
    private const val GENERAL_TWITCH_ACCESS_TOKEN_KEY = "genTwitchAccessToken"
    private const val GENERAL_TWITCH_NAME_KEY = "genTwitchName"
    private const val GENERAL_TWITCH_DISPLAY_NAME_KEY = "genTwitchDisplayName"
    private const val GENERAL_TWITCH_USER_BIO = "genTwitchUserBio"
    private const val GENERAL_TWITCH_LOGO_URL = "genTwitchUserLogoUrl"
    private const val GENERAL_TWITCH_USER_MAIL = "genTwitchUserEmail"
    private const val GENERAL_TWITCH_USER_CREATED = "genTwitchUserCreatedDate"
    private const val GENERAL_TWITCH_USER_UPDATED = "genTwitchUserUpdatedDate"
    private const val GENERAL_TWITCH_USER_TYPE = "genTwitchUserType"
    private const val GENERAL_TWITCH_USER_IS_PARTNER = "genTwitchUserIsPartner"
    private const val GENERAL_TWITCH_USER_ID = "genTwitchUserID"
    private const val GENERAL_FILTER_TOP_STREAMS_LANGUAGE = "genFilterTopStreamLanguage"
    private const val GENERAL_IMAGE_PROXY = "genImageProxy"
    private const val GENERAL_IMAGE_PROXY_URL = "genImageProxyUrl"
    private const val NOTIFICATIONS_IS_DISABLED = "notIsDisabled"
    private const val STREAM_PLAYER_SHOW_VIEWERCOUNT = "streamPlayerShowViewerCount"
    private const val STREAM_PLAYER_SHOW_RUNTIME = "streamPlayerShowRuntime"
    private const val STREAM_PLAYER_REVEAL_NAVIGATION = "streamPlayerRevealNavigation"
    private const val STREAM_PLAYER_AUTO_PLACKBACK = "streamPlayerAutoPlackbackOnReturn"
    private const val STREAM_PLAYER_AUTO_PLAYBACK = "streamPlayerAutoPlaybackOnReturn"
    private const val STREAM_PLAYER_LOCKED_PLAYBACK = "streamPlayerLockedPlayback"
    private const val STREAM_PLAYER_TYPE = "streamPlayerType"
    private const val STREAM_PLAYER_PROXY = "streamPlayerProxy"
    private const val APPEARANCE_STREAM_STYLE = "appStreamStyle"
    private const val APPEARANCE_GAME_STYLE = "appGameStyle"
    private const val APPEARANCE_FOLLOW_STYLE = "appFollowStyle"
    private const val APPEARANCE_STREAM_SIZE = "appStreamSize"
    private const val APPEARANCE_GAME_SIZE = "appGameSize"
    private const val APPEARANCE_STREAMER_SIZE = "appStreamerSize"
    private const val THEME_CHOSEN = "themeColorScheme"
    private const val STREAM_PREF_QUALITY = "streamQualPref"
    private const val STREAM_VOD_PROGRESS = "streamVodProgress"
    private const val STREAM_VOD_LENGTH = "streamVodLength"
    private const val STREAM_SLEEP_HOUR = "streamSleepHour"
    private const val STREAM_SLEEP_MINUTE = "streamSleepMinute"
    private const val PLAYBACK_SPEED = "playbackSpeed"
    private const val SKIP_SILENCE = "playbackSkipSilence"
    private const val SETUP_IS_SETUP = "setupIsSetup"
    private const val SETUP_IS_LOGGED_IN = "setupIsLoggedIn"
    private const val TIP_IS_SHOWN = "tipsAreShown"
    private const val GENERAL_START_PAGE = "genUserStartPage"
    private const val CHAT_EMOTE_SIZE = "chatEmoteSize"
    private const val CHAT_MESSAGE_SIZE = "chatMessageSize"
    private const val CHAT_LANDSCAPE_ENABLE = "chatLandscapeEnable"
    private const val CHAT_LANDSCAPE_SWIPEABLE = "chatLandscapeSwipable"
    private const val CHAT_LANDSCAPE_WIDTH = "chatLandscapeWidth"
    private const val CHAT_ENABLE_SSL = "chatEnableSSL"
    private const val CHAT_ACCOUNT_CONNECT = "chatAccountConnect"
    private const val CHAT_RECENT_EMOTES = "chatRecentEmotes"
    private const val CHAT_KEYBOARD_HEIGHT = "chatKeyboardHeight"
    private const val CHAT_EMOTE_BTTV = "chatEmoteBTTV"
    private const val CHAT_EMOTE_FFZ = "chatEmoteFFZ"
    private const val CHAT_EMOTE_SEVENTV = "chatEmoteSEVENTV"
    private const val NOTIFY_LIVE = "notifyUserLive"
    private const val LAST_START_UP_VERSION_CODE = "lastStartUpVersionCode"
    private const val SHOW_CHANGELOGS = "showChangelogs"

    @SuppressLint("StaticFieldLeak")
    @JvmStatic
    lateinit var context: Context
    private lateinit var preferences: SharedPreferences

    @JvmStatic
    fun init(app: TwireApplication) {
        context = app.applicationContext
        preferences = PreferenceManager.getDefaultSharedPreferences(app.applicationContext)

        preferences.edit {
            for (key in preferences.all.keys) {
                if (key.startsWith(STREAM_VOD_PROGRESS + "v")) {
                    putInt(
                        STREAM_VOD_PROGRESS + key.substring(
                            STREAM_VOD_PROGRESS.length + 1
                        ), preferences.getInt(key, 0)
                    )
                    remove(key)
                } else if (key.startsWith(STREAM_VOD_LENGTH)) {
                    remove(key)
                }

                if (key == STREAM_PLAYER_AUTO_PLACKBACK) {
                    putBoolean(
                        STREAM_PLAYER_AUTO_PLAYBACK,
                        preferences.getBoolean(key, true)
                    )
                    remove(key)
                }
            }
            // Migrate theme to use enums instead of the translated theme name
            val oldTheme = preferences.getString(THEME_CHOSEN, Theme.BLUE.toString())!!
            try {
                Theme.valueOf(oldTheme)
            } catch (e: IllegalArgumentException) {
                val newTheme = Arrays.stream(Theme.entries.toTypedArray<Theme>())
                    .filter { theme: Theme -> context.getString(theme.nameRes) == oldTheme }
                    .findFirst()
                    .orElse(Theme.BLUE)
                theme = newTheme
            }
            // Migrate int user ids to string
            try {
                val userId = preferences.getInt(GENERAL_TWITCH_USER_ID, 0)
                if (userId != 0) {
                    generalTwitchUserID = userId.toString()
                }
            } catch (ignored: ClassCastException) {
            }
            apply()
        }
    }

    private fun <T> setValue(key: String, value: T) {
        preferences.edit {
            putString(key, Gson().toJson(value))
        }
    }

    private fun <T> getValue(key: String, type: Class<T>, defaultValue: T): T {
        return if (preferences.contains(key)) Gson().fromJson(
            preferences.getString(key, ""),
            type
        ) else defaultValue
    }

    /**
     * Get and set the version code for last startup
     */
    @JvmStatic
    var lastVersionCode: Int
        get() = getValue(LAST_START_UP_VERSION_CODE, Int::class.java, 0)
        set(code) = setValue(LAST_START_UP_VERSION_CODE, code)

    @JvmStatic
    var showChangelogs: Boolean
        get() = getValue(SHOW_CHANGELOGS, Boolean::class.java, true)
        set(state) = setValue(SHOW_CHANGELOGS, state)

    /**
     * Get/set list of users that the user have disabled notifications for.
     * This is only used for when upgrading DB
     */
    @JvmStatic
    var usersNotToNotifyWhenLive: ArrayList<String>?
        get() = Gson().fromJson(
            preferences.getString(
                NOTIFY_LIVE,
                ""
            ),
            object :
                TypeToken<ArrayList<String>?>() {
            }.type
        )
        set(value) = preferences.edit { putString(NOTIFY_LIVE, Gson().toJson(value)) }

    /**
     * Chat - Keyboard height. Save the height of users soft keyboard
     */
    @JvmStatic
    var keyboardHeight by Pref(CHAT_KEYBOARD_HEIGHT, 0)

    /**
     * Emotes - list of recent emotes
     */
    @JvmStatic
    var recentEmotes: ArrayList<Emote>?
        get() = Gson().fromJson(
            preferences.getString(
                CHAT_RECENT_EMOTES,
                ""
            ), MAP_TYPE
        )
        set(value) = preferences.edit { putString(CHAT_RECENT_EMOTES, Gson().toJson(value)) }

    /**
     * Appearance - The size of the Game Card.
     */
    @JvmStatic
    var appearanceGameSize by Pref(
        APPEARANCE_GAME_SIZE,
        { context.getString(R.string.card_size_large) }
    )

    /**
     * Appearance - The size of the Stream Card.
     */
    @JvmStatic
    var appearanceStreamSize by Pref(
        APPEARANCE_STREAM_SIZE,
        { context.getString(R.string.card_size_large) }
    )

    /**
     * Appearance - The size of the Streamer Card.
     */
    @JvmStatic
    var appearanceChannelSize by Pref(
        APPEARANCE_STREAMER_SIZE,
        { context.getString(R.string.card_size_large) }
    )

    /**
     * Appearance - The appearance of the Streamer Card.
     */
    @JvmStatic
    var appearanceChannelStyle by Pref(
        APPEARANCE_FOLLOW_STYLE,
        { context.getString(R.string.card_style_normal) }
    )

    /**
     * Appearance - The appearance of the Game Card.
     */
    @JvmStatic
    var appearanceGameStyle by Pref(
        APPEARANCE_GAME_STYLE,
        { context.getString(R.string.card_style_expanded) }
    )

    /**
     * Appearance - The appearance of the Stream Card.
     */
    @JvmStatic
    var appearanceStreamStyle by Pref(
        APPEARANCE_STREAM_STYLE,
        { context.getString(R.string.card_style_expanded) }
    )

    /**
     * General - The users start page. This is the page that is shown when the user starts the app
     */
    @JvmStatic
    var startPage by Pref(
        GENERAL_START_PAGE,
        { defaultStartUpPageTitle }
    )

    private val defaultStartUpPageTitle: String
        get() = context.getString(R.string.navigation_drawer_my_streams_title)

    val defaultNotLoggedInStartUpPageTitle: String
        get() = context.getString(R.string.navigation_drawer_top_streams_title)

    /**
     * The chat emote size. The result is always from 1 to 3.
     */
    @JvmStatic
    var emoteSize by Pref(CHAT_EMOTE_SIZE, 2)

    /**
     * The chat message size. The result is always from 1 to 3.
     */
    @JvmStatic
    var messageSize by Pref(CHAT_MESSAGE_SIZE, 2)

    /**
     * Chat - The chat landscape width: From 0 to 100
     */
    @JvmStatic
    var chatLandscapeWidth by Pref(CHAT_LANDSCAPE_WIDTH, 40)

    /**
     * Chat - Should the chat be able to be showed in landscape
     */
    @JvmStatic
    var isChatInLandscapeEnabled by Pref(CHAT_LANDSCAPE_SWIPEABLE, true)

    /**
     * Chat - Set if the chat should be showable by swiping the VideoView
     */
    @JvmStatic
    var isChatLandscapeSwipeable by Pref(CHAT_LANDSCAPE_ENABLE, true)

    /**
     * Tool tips for showing user functionality when they start the app for the first time.
     */
    @JvmStatic
    var isTipsShown by Pref(TIP_IS_SHOWN, false)

    /**
     * Theme
     */
    @JvmStatic
    var theme: Theme
        get() {
            val themeId = preferences.getString(
                THEME_CHOSEN,
                Theme.BLUE.toString()
            )!!
            return Theme.valueOf(themeId)
        }
        set(theme) = preferences.edit { putString(THEME_CHOSEN, theme.toString()) }

    @JvmStatic
    val isDarkTheme: Boolean
        get() = theme == Theme.NIGHT || theme == Theme.TRUE_NIGHT

    /**
     * Stream - Used to remember the last quality the user selected
     */
    @JvmStatic
    var prefStreamQuality by Pref(
        STREAM_PREF_QUALITY,
        GetStreamURL.QUALITY_AUTO
    )

    /**
     * Stream VOD - Used to remember the progress of a vod
     */
    // TODO: This should probably be stored in a database.
    @JvmStatic
    fun setVodProgress(VODid: String, currentPosition: Long) {
        val progress = (currentPosition / 1000).toInt()

        Timber.d("Saving Current Progress: %s", progress)
        preferences.edit {
            putInt(STREAM_VOD_PROGRESS + VODid, progress)
        }
    }

    @JvmStatic
    fun getVodProgress(VODid: String): Int =
        preferences.getInt(STREAM_VOD_PROGRESS + VODid, 0)

    /**
     * Stream Sleep Timer - Hour
     */
    @JvmStatic
    var streamSleepTimerHour by Pref(STREAM_SLEEP_HOUR, 0)

    /**
     * Stream Sleep Timer - Minute
     */
    @JvmStatic
    var streamSleepTimerMinute by Pref(STREAM_SLEEP_MINUTE, 15)

    @JvmStatic
    var playbackSpeed by Pref(PLAYBACK_SPEED, 1f)

    @JvmStatic
    var skipSilence by Pref(SKIP_SILENCE, false)

    /**
     * General - When the user first logs in we want the user's access token
     */
    @JvmStatic
    var generalTwitchAccessToken by Pref(
        GENERAL_TWITCH_ACCESS_TOKEN_KEY,
        SecretKeys.NO_LOG_IN_ACCESS_TOKEN
    )

    /**
     * General - The user specified twitch username
     * This is the name we want to use when requesting data from twitch
     */
    @JvmStatic
    var generalTwitchName by Pref(GENERAL_TWITCH_NAME_KEY, "twireapp")

    /**
     * General - The user specified twitch display name
     * This is the name that should be shown on screen, when we want to show the user's Twitch name
     */
    @JvmStatic
    var generalTwitchDisplayName by Pref(
        GENERAL_TWITCH_DISPLAY_NAME_KEY,
        "TwireApp"
    )

    /**
     * General - The user's biography
     */
    @JvmStatic
    var generalTwitchUserBio by Pref(
        GENERAL_TWITCH_USER_BIO,
        "No biography specified"
    )

    /**
     * General - The user twitch logo. Will often not exist
     */
    @JvmStatic
    var generalTwitchUserLogo by Pref(GENERAL_TWITCH_LOGO_URL, "")

    /**
     * General - The Email the user used to sign into twitch
     */
    @JvmStatic
    var generalTwitchUserEmail by Pref(GENERAL_TWITCH_USER_MAIL, "")

    /**
     * General - The date the user joined twitch - This is the format "2013-10-09T11:51:51Z"
     */
    @JvmStatic
    var generalTwitchUserCreatedDate by Pref(GENERAL_TWITCH_USER_CREATED, "")

    /**
     * General - The date the user last logged into twitch - This is the format "2013-10-09T11:51:51Z"
     */
    @JvmStatic
    var generalTwitchUserUpdatedDate by Pref(GENERAL_TWITCH_USER_UPDATED, "")

    /**
     * General - The user type
     */
    @JvmStatic
    var generalTwitchUserType by Pref(GENERAL_TWITCH_USER_TYPE, "")

    /**
     * General - Whether or not the user is a Twitch Partner
     */
    @JvmStatic
    var generalTwitchUserIsPartner by Pref(GENERAL_TWITCH_USER_IS_PARTNER, false)

    /**
     * General - The user's twitch ID
     */
    @JvmStatic
    var generalTwitchUserID by Pref(GENERAL_TWITCH_USER_ID, "0")

    /**
     * Stream Player -
     */
    @JvmStatic
    var streamPlayerShowViewerCount by Pref(STREAM_PLAYER_SHOW_VIEWERCOUNT, true)

    @JvmStatic
    var streamPlayerRuntime by Pref(STREAM_PLAYER_SHOW_RUNTIME, true)

    /**
     * General - Filter top streams by language
     */
    @JvmStatic
    var generalFilterTopStreamsByLanguage by Pref(
        GENERAL_FILTER_TOP_STREAMS_LANGUAGE,
        true
    )

    /**
     * Stream Player -
     */
    @JvmStatic
    var streamPlayerShowNavigationBar by Pref(
        STREAM_PLAYER_REVEAL_NAVIGATION,
        false
    )

    /**
     * Stream Player -
     */
    @JvmStatic
    var streamPlayerAutoContinuePlaybackOnReturn by Pref(STREAM_PLAYER_AUTO_PLAYBACK, false)

    @JvmStatic
    var streamPlayerLockedPlayback by Pref(STREAM_PLAYER_LOCKED_PLAYBACK, true)

    @JvmStatic
    var streamPlayerType by Pref(STREAM_PLAYER_TYPE, 0)

    /**
     * Stream Player - Type
     */
    @JvmStatic
    var streamPlayerProxy by Pref(STREAM_PLAYER_PROXY, "")

    /**
     * Stream Player - Proxy
     */
    @JvmStatic
    val isNotificationsDisabled by Pref(NOTIFICATIONS_IS_DISABLED, false)

    /**
     * Setup
     */
    @JvmStatic
    var isSetup by Pref(SETUP_IS_SETUP, false)

    /**
     * Setup - Has the user logged in?
     */
    @JvmStatic
    var isLoggedIn by Pref(SETUP_IS_LOGGED_IN, false) { TwireApplication.updateCredential() }

    /**
     * Chat - Enable SSL?
     */
    @JvmStatic
    var chatEnableSSL by Pref(CHAT_ENABLE_SSL, true)

    /**
     * Chat - Emote Settings
     */
    @JvmStatic
    var chatEmoteBTTV by Pref(CHAT_EMOTE_BTTV, true)

    @JvmStatic
    var chatEmoteFFZ by Pref(CHAT_EMOTE_FFZ, true)

    @JvmStatic
    var chatEmoteSEVENTV by Pref(CHAT_EMOTE_SEVENTV, false)

    /**
     * Chat - Connect with Account
     */
    @JvmStatic
    var chatAccountConnect by Pref(CHAT_ACCOUNT_CONNECT, true)

    /**
     * General - Use Image Proxy
     */
    @JvmStatic
    var generalUseImageProxy by Pref(GENERAL_IMAGE_PROXY, false)

    /**
     * General - Use Image Proxy Url
     */
    @JvmStatic
    var imageProxyUrl by Pref(
        GENERAL_IMAGE_PROXY_URL,
        "https://external-content.duckduckgo.com/iu/?u="
    )

    @JvmStatic
    var reportErrors by Pref(
        "reportErrors",
        ReportErrors.ASK
    )

    private class Pref<T>(
        val key: String,
        val default: () -> T,
        val onChange: (() -> Unit)? = null
    ) {
        constructor(key: String, default: T, onChange: (() -> Unit)? = null) : this(
            key,
            { default },
            onChange
        )

        @Suppress("UNCHECKED_CAST")
        operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
            return when (val defaultValue = default()) {
                is Boolean -> preferences.getBoolean(key, defaultValue) as T
                is Int -> preferences.getInt(key, defaultValue) as T
                is Long -> preferences.getLong(key, defaultValue) as T
                is Float -> preferences.getFloat(key, defaultValue) as T
                is String -> preferences.getString(key, defaultValue) as T
                is Enum<*> -> {
                    val stringValue =
                        preferences.getString(key, defaultValue.name) ?: defaultValue.name
                    return (defaultValue.javaClass.enumConstants?.first { it.name == stringValue }
                        ?: defaultValue) as T
                }

                else -> throw IllegalArgumentException("Unsupported type")
            }
        }

        operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
            when (value) {
                is Boolean -> preferences.edit { putBoolean(key, value) }
                is Int -> preferences.edit { putInt(key, value) }
                is Long -> preferences.edit { putLong(key, value) }
                is Float -> preferences.edit { putFloat(key, value) }
                is String -> preferences.edit { putString(key, value) }
                is Enum<*> -> preferences.edit { putString(key, value.name) }
                else -> throw IllegalArgumentException("Unsupported type")
            }
            onChange?.invoke()
        }
    }
}

enum class ReportErrors(@StringRes val stringRes: Int) {
    ALWAYS(R.string.report_error_always),
    NEVER(R.string.report_error_never),
    ASK(R.string.report_error_ask)
}
