package com.perflyst.twire.tasks

import android.content.ContentValues
import android.content.Context
import com.perflyst.twire.model.ChannelInfo
import com.perflyst.twire.service.Settings.usersNotToNotifyWhenLive
import com.perflyst.twire.service.SubscriptionsDbHelper
import com.perflyst.twire.service.TempStorage
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.Objects
import java.util.TreeMap

/**
 * Precondition: Current user subscriptions should already have been loaded from database
 * Adds a subscription to the internal database. The input should be a SubscriptionStreamerInfo object
 * Precondition: This task requires two inputs. First input should be an ArrayList of streamerInfo objects you want to add to the database.
 * The second should be the BaseContext
 */
class AddFollowsToDB(baseContext: Context?, private val subsToAdd: List<ChannelInfo>) :
    Runnable {
    private val baseContext: WeakReference<Context?> = WeakReference<Context?>(baseContext)

    override fun run() {
        val subsAdded = ArrayList<ChannelInfo>()
        val subsToCheck: MutableMap<String, ChannelInfo> = TreeMap<String, ChannelInfo>()


        Timber.v("Entered %s", this.javaClass.getSimpleName())

        val baseContext = this.baseContext.get()
        val mDbHelper = SubscriptionsDbHelper(baseContext)

        // Get the data repository in write mode
        val db = mDbHelper.writableDatabase

        Timber.d(TempStorage.loadedStreamers.toString())
        // Loop over the provided list of StreamerInfo objects
        for (subToAdd in subsToAdd) {
            // Make sure the streamer is not already in the database
            if (TempStorage.containsLoadedStreamer(subToAdd)) {
                Timber.d("Streamer (${subToAdd.login}) already in database")
                continue
            }

            val usersNotToNotifyWhenLive: ArrayList<String>? = usersNotToNotifyWhenLive
            val disableForStreamer =
                usersNotToNotifyWhenLive != null && usersNotToNotifyWhenLive.contains(subToAdd.userId)

            // Create a new map of values where column names are the keys
            val values = ContentValues()
            values.put(SubscriptionsDbHelper.COLUMN_ID, subToAdd.userId)
            values.put(SubscriptionsDbHelper.COLUMN_STREAMER_NAME, subToAdd.login)
            values.put(SubscriptionsDbHelper.COLUMN_DISPLAY_NAME, subToAdd.displayName)
            values.put(SubscriptionsDbHelper.COLUMN_DESCRIPTION, subToAdd.streamDescription)
            values.put(
                SubscriptionsDbHelper.COLUMN_FOLLOWERS,
                Objects.requireNonNullElse<Int?>(subToAdd.fetchFollowers(), 0)
            )
            values.put(
                SubscriptionsDbHelper.COLUMN_NOTIFY_WHEN_LIVE,
                if (subToAdd.isNotifyWhenLive && !disableForStreamer) 1 else 0
            )
            values.put(SubscriptionsDbHelper.COLUMN_IS_TWITCH_FOLLOW, 1)


            // Test if the URL strings are null, to make sure we don't call toString on a null.
            if (subToAdd.logoURL != null) values.put(
                SubscriptionsDbHelper.COLUMN_LOGO_URL,
                subToAdd.logoURL.toString()
            )

            if (subToAdd.videoBannerURL != null) values.put(
                SubscriptionsDbHelper.COLUMN_VIDEO_BANNER_URL,
                subToAdd.videoBannerURL.toString()
            )

            if (subToAdd.profileBannerURL != null) values.put(
                SubscriptionsDbHelper.COLUMN_PROFILE_BANNER_URL,
                subToAdd.profileBannerURL.toString()
            )


            db.insert(SubscriptionsDbHelper.TABLE_NAME, null, values)

            // we return the rowId so we can return the row we inserted to a cursor
            /*
            long rowId = db.insert(SubscriptionsDbHelper.TABLE_NAME, null, values);
            Cursor cursor = db.query(SubscriptionsDbHelper.TABLE_NAME, allColumns, SubscriptionsDbHelper.COLUMN_ID + " = " + rowId, null, null, null, null);
            cursor.close();
            */

            // The StreamerInfo to the result list
            subsAdded.add(subToAdd)

            // And to the map to ensure we can check if they are online
            subsToCheck.put(subToAdd.login, subToAdd)
        }
        db.close()

        TempStorage.addLoadedStreamer(subsAdded)
        Timber.d("Count of streamers added: %s", subsAdded.size)
        Timber.d("Streamers ($subsAdded) added to database")
    }
}
