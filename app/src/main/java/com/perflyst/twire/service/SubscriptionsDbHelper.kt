package com.perflyst.twire.service

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import com.perflyst.twire.service.Settings.usersNotToNotifyWhenLive
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by SebastianRask on 27-01-2015.
 * Helper for creating and maintaining our database for StreamerInfo objects
 */
class SubscriptionsDbHelper(private val mContext: Context?) : SQLiteOpenHelper(
    mContext, DATABASE_NAME, null, DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            if (newVersion == 7) {
                usersNotToNotifyWhenLive = getUsersNotToNotify(db)
            }
        } catch (e: SQLiteException) {
            Timber.e(e)
        }


        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
        //db.close();
    }

    @Throws(SQLiteException::class)
    private fun getUsersNotToNotify(db: SQLiteDatabase): ArrayList<String> {
        val query =
            "SELECT * FROM $TABLE_NAME WHERE $COLUMN_NOTIFY_WHEN_LIVE=0;"
        val cursor = db.rawQuery(query, null)

        val usersToNotify = ArrayList<String>()

        while (cursor.moveToNext()) {
            val idPosition = cursor.getColumnIndex(COLUMN_ID)
            val userId = cursor.getString(idPosition)

            usersToNotify.add(userId)
        }

        cursor.close()

        return usersToNotify
    }

    // Wipe
    fun onWipe(db: SQLiteDatabase, isLoggedin: Boolean) {
        if (!db.isOpen) {
            return
        }
        val query = if (isLoggedin) {
            "DELETE FROM $TABLE_NAME WHERE $COLUMN_IS_TWITCH_FOLLOW == 0;"
        } else {
            "DELETE FROM $TABLE_NAME;"
        }
        Timber.tag("query").d(query)
        db.execSQL(query)
    }

    // Export import below
    fun onImport(db: SQLiteDatabase): Int {
        if (!db.isOpen) {
            return 0
        }
        try {
            val filedata = read(mContext, EXPORT_NAME)
            if (filedata == null) {
                return 0
            }
            val channelsfile = JSONObject(filedata)
            val channels = channelsfile.getJSONArray("Channels")
            for (i in 0..<channels.length()) {
                val tempchannel = channels.getJSONObject(i)

                val values = ContentValues()
                // these can´t be empty
                values.put(COLUMN_ID, tempchannel.getInt("ID"))
                values.put(COLUMN_STREAMER_NAME, tempchannel.getString("NAME"))
                values.put(COLUMN_DISPLAY_NAME, tempchannel.getString("DISPLAY_NAME"))
                values.put(COLUMN_DESCRIPTION, tempchannel.getString("DESCRIPTION"))
                values.put(COLUMN_FOLLOWERS, tempchannel.getInt("FOLLOWERS"))
                values.put(COLUMN_NOTIFY_WHEN_LIVE, tempchannel.getInt("NOTIFY"))
                values.put(COLUMN_IS_TWITCH_FOLLOW, tempchannel.getInt("IS_TWITCH"))

                // these could be empty
                if (tempchannel.has("LOGO")) values.put(
                    COLUMN_LOGO_URL,
                    tempchannel.getString("LOGO")
                )

                if (tempchannel.has("BANNER")) values.put(
                    COLUMN_VIDEO_BANNER_URL,
                    tempchannel.getString("BANNER")
                )

                if (tempchannel.has("PROFILE_BANNER")) values.put(
                    COLUMN_PROFILE_BANNER_URL,
                    tempchannel.getString("PROFILE_BANNER")
                )

                // check what methode should be used
                val sql = "SELECT * FROM $TABLE_NAME WHERE _ID=${tempchannel.getInt("ID")}"
                val cursor = db.rawQuery(sql, null)

                if (cursor.count > 0) {
                    db.replace(TABLE_NAME, null, values)
                } else {
                    db.insert(TABLE_NAME, null, values)
                }
                cursor.close()
            }
            return channels.length()
        } catch (e: JSONException) {
            Timber.e(e)
        }
        return 0
    }

    fun onExport(db: SQLiteDatabase): Int {
        val query = "SELECT * FROM $TABLE_NAME;"
        val cursor = db.rawQuery(query, null)

        val channelstoExport = JSONArray()

        while (cursor.moveToNext()) {
            val idPosition = cursor.getColumnIndex(COLUMN_ID)
            val streamerNamePosition = cursor.getColumnIndex(COLUMN_STREAMER_NAME)
            val displayNamePosition = cursor.getColumnIndex(COLUMN_DISPLAY_NAME)
            val bioPosition = cursor.getColumnIndex(COLUMN_DESCRIPTION)
            val followersPosition = cursor.getColumnIndex(COLUMN_FOLLOWERS)
            val logoPosition = cursor.getColumnIndex(COLUMN_LOGO_URL)
            val bannerPosition = cursor.getColumnIndex(COLUMN_VIDEO_BANNER_URL)
            val banner_profilePosition = cursor.getColumnIndex(COLUMN_PROFILE_BANNER_URL)
            val notifyPosition = cursor.getColumnIndex(COLUMN_NOTIFY_WHEN_LIVE)
            val istwitchPosition = cursor.getColumnIndex(COLUMN_IS_TWITCH_FOLLOW)

            val userId = cursor.getInt(idPosition)
            val name = cursor.getString(streamerNamePosition)
            val displayname = cursor.getString(displayNamePosition)
            val description = cursor.getString(bioPosition)
            val followers = cursor.getInt(followersPosition)
            val logo = cursor.getString(logoPosition)
            val banner = cursor.getString(bannerPosition)
            val profilebanner = cursor.getString(banner_profilePosition)
            val notify = cursor.getInt(notifyPosition)
            val isTwitch = cursor.getInt(istwitchPosition)

            try {
                val tempchannel = JSONObject()
                tempchannel.put("ID", userId)
                tempchannel.put("NAME", name)
                tempchannel.put("DISPLAY_NAME", displayname)
                tempchannel.put("DESCRIPTION", description)
                tempchannel.put("FOLLOWERS", followers)
                tempchannel.put("LOGO", logo)
                tempchannel.put("BANNER", banner)
                tempchannel.put("PROFILE_BANNER", profilebanner)
                tempchannel.put("NOTIFY", notify)
                tempchannel.put("IS_TWITCH", isTwitch)
                channelstoExport.put(tempchannel)
            } catch (e: JSONException) {
                Timber.e(e)
            }
        }
        cursor.close()

        if (channelstoExport.length() > 0) {
            try {
                val channels = JSONObject()
                channels.put("Channels", channelstoExport)
                val jsonStr = channels.toString()
                Timber.tag("Export String").d(jsonStr)

                create(mContext, EXPORT_NAME, jsonStr)
                return channelstoExport.length()
            } catch (e: JSONException) {
                Timber.e(e)
            }
        }
        return 0
    }

    private fun read(context: Context?, fileName: String): String? {
        try {
            val cw = ContextWrapper(context)
            val directory = cw.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val importfile = File(directory, fileName)
            val fis = FileInputStream(importfile)
            val isr = InputStreamReader(fis)
            val bufferedReader = BufferedReader(isr)
            val sb = StringBuilder()
            var line: String?
            while ((bufferedReader.readLine().also { line = it }) != null) {
                sb.append(line)
            }
            return sb.toString()
        } catch (_: IOException) {
            return null
        }
    }

    private fun create(context: Context?, fileName: String, jsonString: String) {
        try {
            val cw = ContextWrapper(context)
            val directory = cw.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val exportfile = File(directory, fileName)
            val fos = FileOutputStream(exportfile)
            fos.write(jsonString.toByteArray())
            fos.close()
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    companion object {
        const val TABLE_NAME: String = "Subscriptions"
        const val COLUMN_ID: String = "_id"
        const val COLUMN_STREAMER_NAME: String = "streamerName"
        const val COLUMN_DISPLAY_NAME: String = "displayName"
        const val COLUMN_DESCRIPTION: String = "biography"
        const val COLUMN_FOLLOWERS: String = "followers"
        const val COLUMN_UNIQUE_VIEWS: String = "views"
        const val COLUMN_LOGO_URL: String = "logoURL"
        const val COLUMN_VIDEO_BANNER_URL: String = "videoBannerURL"
        const val COLUMN_PROFILE_BANNER_URL: String = "profileBannerURL"
        const val COLUMN_NOTIFY_WHEN_LIVE: String = "notifityWhenLive"
        const val COLUMN_IS_TWITCH_FOLLOW: String = "isTwitchFollow"
        const val DATABASE_VERSION: Int = 8
        private const val DATABASE_NAME = "SubscriptionsDb_09.db"
        private const val EXPORT_NAME = "export.json"
        private const val SQL_CREATE_ENTRIES = """CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_STREAMER_NAME TEXT NOT NULL UNIQUE,
                $COLUMN_DISPLAY_NAME TEXT,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_FOLLOWERS INTEGER,
                $COLUMN_UNIQUE_VIEWS INTEGER,
                $COLUMN_LOGO_URL TEXT,
                $COLUMN_VIDEO_BANNER_URL TEXT,
                $COLUMN_PROFILE_BANNER_URL TEXT,
                $COLUMN_NOTIFY_WHEN_LIVE INTEGER,
                $COLUMN_IS_TWITCH_FOLLOW INTEGER
                );"""

        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}
