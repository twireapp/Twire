package com.perflyst.twire.service

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.customview.widget.ViewDragHelper
import androidx.drawerlayout.widget.DrawerLayout
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.perflyst.twire.R
import com.perflyst.twire.TwireApplication
import com.perflyst.twire.activities.main.MyChannelsActivity
import com.perflyst.twire.activities.main.MyStreamsActivity
import com.perflyst.twire.activities.main.TopGamesActivity
import com.perflyst.twire.activities.main.TopStreamsActivity
import com.perflyst.twire.misc.SecretKeys
import com.perflyst.twire.model.ChannelInfo
import com.perflyst.twire.model.UserInfo
import com.perflyst.twire.service.Settings.startPage
import com.perflyst.twire.service.Settings.usersNotToNotifyWhenLive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.Random
import java.util.TreeMap
import java.util.concurrent.TimeUnit

/**
 * Created by Sebastian Rask on 12-02-2015.
 * Class made purely for adding utility methods for other classes
 */
// TODO: Split this service out to multiple more cohesive service classes
object Service {
    val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(5, TimeUnit.SECONDS)
        .connectTimeout(3, TimeUnit.SECONDS)
        .build()

    val errorEmote: String
        get() {
            val emotes = arrayOf(
                "('.')",
                "('x')",
                "(>_<)",
                "(>.<)",
                "(;-;)",
                "\\(o_o)/",
                "(O_o)",
                "(o_0)",
                "(≥o≤)",
                "(≥o≤)",
                "(·.·)",
                "(·_·)"
            )
            val rnd = Random()
            return emotes[rnd.nextInt(emotes.size - 1)]
        }

    /**
     * Returns an intent with the right destination activity.
     *
     * @param context The context from which the method is called
     * @return The intent
     */
    fun getStartPageIntent(context: Context): Intent {
        val title = startPage

        var activityClass: Class<*> = MyStreamsActivity::class.java
        when (title) {
            context.getString(R.string.navigation_drawer_follows_title) -> {
                activityClass = MyChannelsActivity::class.java
            }

            context.getString(R.string.navigation_drawer_top_streams_title) -> {
                activityClass = TopStreamsActivity::class.java
            }

            context.getString(R.string.navigation_drawer_top_games_title) -> {
                activityClass = TopGamesActivity::class.java
            }
        }

        val intent = Intent(context, activityClass)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return intent
    }

    /**
     * Animates the background color of a view from one color to another color.
     *
     * @param v         The view to animate
     * @param toColor   The To Color
     * @param fromColor The From Color
     * @param duration  The Duration of the animation
     */
    fun animateBackgroundColorChange(v: View?, toColor: Int, fromColor: Int, duration: Int) {
        val colorFade =
            ObjectAnimator.ofObject(v, "backgroundColor", ArgbEvaluator(), fromColor, toColor)
        colorFade.setDuration(duration.toLong())
        colorFade.start()
    }

    /**
     * Finds and returns an attribute color. If it was not found the method returns the default color
     */
    @JvmStatic
    fun getColorAttribute(
        @AttrRes attribute: Int,
        @ColorRes defaultColor: Int,
        context: Context
    ): Int {
        val a = TypedValue()
        context.theme.resolveAttribute(attribute, a, true)
        return if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            a.data
        } else {
            ContextCompat.getColor(context, defaultColor)
        }
    }

    /**
     * @param view         The view to get the color from
     * @param defaultColor The color to return if the view's background isn't a ColorDrawable
     * @return The color
     */
    fun getBackgroundColorFromView(view: View?, defaultColor: Int): Int {
        var color = defaultColor
        val background = view?.background
        if (background is ColorDrawable) {
            color = background.color
        }

        return color
    }

    /**
     * Gets the accent color from the current theme
     */
    fun getAccentColor(mContext: Context): Int {
        val typedValue = TypedValue()

        val a = mContext.obtainStyledAttributes(
            typedValue.data,
            intArrayOf(androidx.appcompat.R.attr.colorAccent)
        )
        val color = a.getColor(0, 0)

        a.recycle()

        return color
    }

    /**
     * Method for increasing a Navigation Drawer's edge size.
     */
    fun increaseNavigationDrawerEdge(aDrawerLayout: DrawerLayout) {
        // Increase the area from which you can open the navigation drawer.
        try {
            val mDragger = aDrawerLayout.javaClass.getDeclaredField("mLeftDragger")
            mDragger.isAccessible = true
            val draggerObj = mDragger.get(aDrawerLayout) as ViewDragHelper?

            val mEdgeSize = draggerObj!!.javaClass.getDeclaredField("mEdgeSize")
            mEdgeSize.isAccessible = true
            val edgeSize = mEdgeSize.getInt(draggerObj) * 3

            mEdgeSize.setInt(
                draggerObj,
                edgeSize
            ) //optimal value as for me, you may set any constant in dp
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startNotifications(context: Context?) {
    }

    /**
     * Does the opposite of the View.bringToFront() method
     *
     * @param v the view you want to send to the back
     */
    fun bringToBack(v: View) {
        val parent = v.parent as ViewGroup?
        if (null != parent) {
            parent.removeView(v)
            parent.addView(v, 0)
        }
    }

    /**
     * Gets the navigation drawer toggle view from a toolbar
     *
     * @param toolbar The toolbar containing the navigation button
     * @return The ImageButton
     */
    fun getNavButtonView(toolbar: Toolbar?): ImageButton? {
        try {
            val toolbarClass: Class<*> = Toolbar::class.java
            val navButtonField = toolbarClass.getDeclaredField("mNavButtonView")
            navButtonField.isAccessible = true

            return navButtonField.get(toolbar) as ImageButton?
        } catch (e: IllegalAccessException) {
            Timber.e(e)
        } catch (e: NoSuchFieldException) {
            Timber.e(e)
        }

        return null
    }

    /**
     * Returns the height of the device screen
     */
    fun getScreenHeight(context: Context): Int {
        val wm = ContextCompat.getSystemService(context, WindowManager::class.java)
        val displayMetrics = DisplayMetrics()
        wm?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    fun urlToJSONString(urlToRead: String): String {
        // Alright, so sometimes Twitch decides that our client ID should be blocked. Currently only happens for the hidden /api endpoints.
        // IF we are being blocked, then retry the request with Twitch web ClientID. They are typically not blocking this.
        var result = urlToJSONString(
            urlToRead,
            true
        ) // "{\"error\":\"Gone\",\"status\":410,\"message\":\"this API has been removed.\"}";
        var retryWithWebClientId = false
        if (result == null || result.isEmpty()) {
            retryWithWebClientId = true
        } else {
            try {
                val resultJson = JSONObject(result)
                val status = resultJson.getInt("status")
                val error = resultJson.getString("error")
                retryWithWebClientId = status == 410 || error == "Gone"
            } catch (_: Exception) {
            }
        }

        if (retryWithWebClientId) {
            result = urlToJSONString(urlToRead, false)
        }

        return result ?: ""
    }

    fun urlToJSONString(urlToRead: String, useOurClientId: Boolean): String? {
        val clientId: String = if (useOurClientId) {
            SecretKeys.APPLICATION_CLIENT_ID
        } else {
            SecretKeys.TWITCH_WEB_CLIENT_ID
        }

        val request = Request.Builder()
            .url(urlToRead)
            .header("Client-ID", clientId)
            .header("Accept", "application/vnd.twitchtv.v5+json")
            .build()

        return urlToJSONString(request)
    }

    fun graphQL(
        operation: String,
        hash: String,
        variables: MutableMap<String, Any?>
    ): JSONObject? {
        val query =
            "[{\"operationName\":\"$operation\",\"variables\":${JSONObject(variables)},\"extensions\":{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"$hash\"}}}]"

        val request = Request.Builder()
            .url("https://gql.twitch.tv/gql")
            .header("Client-ID", SecretKeys.TWITCH_GRAPHQL_ID)
            .post(query.toRequestBody("application/json".toMediaType()))
            .build()

        val result = urlToJSONString(request)
        if (result == null) return null

        try {
            val array = JSONArray(result)
            return array.getJSONObject(0).getJSONObject("data")
        } catch (e: JSONException) {
            Timber.e(e)
            return null
        }
    }

    fun urlToJSONString(request: Request): String? {
        val response = makeRequest(request)
        if (response == null) return null

        val result = response.body

        if (result == null || result.isEmpty() || result[0] != '{' && result[0] != '[') {
            Timber.tag("URL TO JSON STRING").v("%s did not successfully get read", request.url)
            Timber.tag("URL TO JSON STRING").v("Result of reading - %s", result)
        }

        return result
    }

    fun makeRequest(request: Request): SimpleResponse? {
        val response: Response?
        try {
            response = client.newCall(request).execute()
            return SimpleResponse(response)
        } catch (_: IOException) {
            return null
        }
    }

    fun getStreamerInfoFromUserId(streamerId: String): ChannelInfo? {
        val users =
            TwireApplication.helix.getUsers(null, listOf(streamerId), null).execute()
                .users
        if (users.isEmpty()) return null

        return ChannelInfo(users[0]!!)
    }

    /**
     * Connects to the database containing data of user follows. Loops through every record of in the database and creates a StreamerInfo object for these
     */
    fun getStreamerInfoFromDB(context: Context?): MutableMap<String, ChannelInfo> {
        val subscriptions: MutableMap<String, ChannelInfo> = TreeMap<String, ChannelInfo>()
        val mDbHelper = SubscriptionsDbHelper(context)
        val DISTINCT = true
        val allColumns = arrayOf<String?>(
            SubscriptionsDbHelper.Companion.COLUMN_ID,
            SubscriptionsDbHelper.Companion.COLUMN_STREAMER_NAME,
            SubscriptionsDbHelper.Companion.COLUMN_DISPLAY_NAME,
            SubscriptionsDbHelper.Companion.COLUMN_DESCRIPTION,
            SubscriptionsDbHelper.Companion.COLUMN_FOLLOWERS,
            SubscriptionsDbHelper.Companion.COLUMN_UNIQUE_VIEWS,
            SubscriptionsDbHelper.Companion.COLUMN_LOGO_URL,
            SubscriptionsDbHelper.Companion.COLUMN_VIDEO_BANNER_URL,
            SubscriptionsDbHelper.Companion.COLUMN_PROFILE_BANNER_URL,
            SubscriptionsDbHelper.Companion.COLUMN_NOTIFY_WHEN_LIVE
        )

        // Get the data repository in read mode
        val db = mDbHelper.readableDatabase

        val cursor = db.query(
            DISTINCT,
            SubscriptionsDbHelper.Companion.TABLE_NAME,
            allColumns,
            null, null, null, null, null, null
        )

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast) {
                val streamerId = cursor.getString(0)
                val streamerName = cursor.getString(1)
                val displayName = cursor.getString(2)
                val streamDescription = cursor.getString(3)
                val followers = cursor.getInt(4)
                var logo: URL? = null
                var videoBanner: URL? = null
                var profileBanner: URL? = null
                val notifyWhenLive = cursor.getInt(9) == 1

                // Make sure the streamer has uploaded pictures
                try {
                    if (!cursor.isNull(6)) logo = URL(cursor.getString(6))

                    if (!cursor.isNull(7)) videoBanner = URL(cursor.getString(7))

                    if (!cursor.isNull(8)) profileBanner = URL(cursor.getString(8))
                } catch (e: MalformedURLException) {
                    Timber.e(e)
                }

                // Create new StreamerInfo object from data fetched from database
                val mChannelInfo = ChannelInfo(
                    UserInfo(streamerId, streamerName, displayName),
                    streamDescription, followers, logo, videoBanner, profileBanner
                )
                mChannelInfo.isNotifyWhenLive = notifyWhenLive
                subscriptions.put(mChannelInfo.displayName, mChannelInfo)

                // Move to the next record in the database
                cursor.moveToNext()
            }
        }

        cursor.close()
        db.close()

        return subscriptions
    }

    /**
     * Determines whether or not the user is currently following a streamer.
     * This is done by looking in the SQLite database
     */
    fun isUserFollowingStreamer(streamername: String?, context: Context?): Boolean {
        val mDbHelper = SubscriptionsDbHelper(context)
        val db = mDbHelper.readableDatabase
        val query =
            "SELECT * FROM ${SubscriptionsDbHelper.Companion.TABLE_NAME} WHERE ${SubscriptionsDbHelper.Companion.COLUMN_STREAMER_NAME}='$streamername';"
        var result = false
        val cursor = db.rawQuery(query, null)
        if (cursor.count > 0) {
            result = true
        }
        cursor.close()
        db.close()
        return result
    }

    fun isUserTwitch(streamerId: String?, context: Context?): Boolean {
        val mDbHelper = SubscriptionsDbHelper(context)
        val db = mDbHelper.readableDatabase
        val query =
            "SELECT * FROM ${SubscriptionsDbHelper.Companion.TABLE_NAME} WHERE ${SubscriptionsDbHelper.Companion.COLUMN_ID}='$streamerId';"
        var result = false
        val cursor = db.rawQuery(query, null)
        val columnIndex =
            cursor.getColumnIndex(SubscriptionsDbHelper.Companion.COLUMN_IS_TWITCH_FOLLOW)
        if (cursor.moveToFirst()) {
            result = cursor.getInt(columnIndex) > 0
        }
        cursor.close()
        db.close()
        return result
    }

    fun updateStreamerInfoDbWithValues(values: ContentValues?, context: Context?, id: String?) {
        updateStreamerInfoDbWithValues(
            values,
            context,
            "${SubscriptionsDbHelper.Companion.COLUMN_ID}=?",
            arrayOf(id)
        )
    }

    private fun updateStreamerInfoDbWithValues(
        values: ContentValues?,
        context: Context?,
        whereClause: String?,
        whereArgs: Array<String?>?
    ) {
        val helper = SubscriptionsDbHelper(context)

        try {
            val db = helper.writableDatabase

            if (isDbSafe(db)) {
                db.update(
                    SubscriptionsDbHelper.Companion.TABLE_NAME,
                    values,
                    whereClause,
                    whereArgs
                )
            }

            db.close()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun isDbSafe(db: SQLiteDatabase): Boolean {
        return db.isOpen && !db.isReadOnly && !db.isDbLockedByCurrentThread()
    }

    fun deleteStreamerInfoFromDB(context: Context?, channelInfo: ChannelInfo): Boolean {
        val mDbHelper = SubscriptionsDbHelper(context)
        val db = mDbHelper.writableDatabase // Get the data repository in write mode

        var result = false
        val streamerId = channelInfo.userId
        if (!isUserTwitch(streamerId, context)) {
            result = db.delete(
                SubscriptionsDbHelper.Companion.TABLE_NAME,
                "${SubscriptionsDbHelper.Companion.COLUMN_ID} = '$streamerId'",
                null
            ) > 0

            if (result) TempStorage.removeLoadedStreamer(channelInfo)
        }

        db.close()

        return result
    }

    fun insertStreamerInfoToDB(context: Context?, streamer: ChannelInfo) {
        val usersNotToNotifyWhenLive: ArrayList<String>? = usersNotToNotifyWhenLive
        val disableForStreamer =
            usersNotToNotifyWhenLive != null && usersNotToNotifyWhenLive.contains(streamer.userId)

        // Create a new map of values where column names are the keys
        val values = ContentValues()
        values.put(SubscriptionsDbHelper.Companion.COLUMN_ID, streamer.userId)
        values.put(SubscriptionsDbHelper.Companion.COLUMN_STREAMER_NAME, streamer.login)
        values.put(SubscriptionsDbHelper.Companion.COLUMN_DISPLAY_NAME, streamer.displayName)
        values.put(SubscriptionsDbHelper.Companion.COLUMN_DESCRIPTION, streamer.streamDescription)
        values.put(SubscriptionsDbHelper.Companion.COLUMN_UNIQUE_VIEWS, 0)
        values.put(
            SubscriptionsDbHelper.Companion.COLUMN_NOTIFY_WHEN_LIVE,
            if (disableForStreamer) 0 else 1
        ) // Enable by default
        values.put(SubscriptionsDbHelper.Companion.COLUMN_IS_TWITCH_FOLLOW, 0)


        // Test if the URL strings are null, to make sure we don't call toString on a null.
        if (streamer.logoURL != null) values.put(
            SubscriptionsDbHelper.Companion.COLUMN_LOGO_URL,
            streamer.logoURL.toString()
        )

        if (streamer.videoBannerURL != null) values.put(
            SubscriptionsDbHelper.Companion.COLUMN_VIDEO_BANNER_URL,
            streamer.videoBannerURL.toString()
        )

        if (streamer.profileBannerURL != null) values.put(
            SubscriptionsDbHelper.Companion.COLUMN_PROFILE_BANNER_URL,
            streamer.profileBannerURL.toString()
        )


        streamer.getFollowers({ followers: Int? ->
            values.put(SubscriptionsDbHelper.Companion.COLUMN_FOLLOWERS, followers)
            val helper = SubscriptionsDbHelper(context)
            val db = helper.writableDatabase
            db.insert(SubscriptionsDbHelper.Companion.TABLE_NAME, null, values)
            db.close()
        }, 0)

        TempStorage.addLoadedStreamer(streamer)
    }

    fun clearStreamerInfoDb(context: Context?) {
        Timber.i("CLEARING STREAMERINFO DATABASE")
        TempStorage.loadedStreamers.clear()
        val helper = SubscriptionsDbHelper(context)
        helper.onUpgrade(
            helper.writableDatabase,
            SubscriptionsDbHelper.Companion.DATABASE_VERSION,
            SubscriptionsDbHelper.Companion.DATABASE_VERSION + 1
        )
    }

    fun dpToPixels(context: Context, dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    var gameNameCache: LoadingCache<String?, String?> = CacheBuilder.newBuilder()
        .maximumSize(100)
        .build<String?, String?>(object : CacheLoader<String, String>() {
            override fun load(gameId: String): String {
                return TwireApplication.helix.getGames(null, listOf(gameId), null, null)
                    .execute()
                    .games[0]
                    .name
            }
        })

    class SimpleResponse(response: Response) {
        var code: Int
        var body: String? = null
        var response: Response?

        init {
            checkNotNull(response.body)

            code = response.code
            this.response = response

            try {
                body = response.body!!.string()
            } catch (_: IOException) {
            }
        }
    }
}
