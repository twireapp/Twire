package com.perflyst.twire.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.service.SubscriptionsDbHelper;
import com.perflyst.twire.service.TempStorage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import timber.log.Timber;

/**
 * Precondition: Current user subscriptions should already have been loaded from database
 * Adds a subscription to the internal database. The input should be a SubscriptionStreamerInfo object
 * Precondition: This task requires two inputs. First input should be an ArrayList of streamerInfo objects you want to add to the database.
 * The second should be the BaseContext
 */
public class AddFollowsToDB implements Runnable {
    private final WeakReference<Context> baseContext;
    private final ArrayList<ChannelInfo> subsToAdd;

    public AddFollowsToDB(Context baseContext, ArrayList<ChannelInfo> subsToAdd) {
        this.baseContext = new WeakReference<>(baseContext);
        this.subsToAdd = subsToAdd;
    }

    public void run() {
        @SuppressWarnings("unchecked")
        ArrayList<ChannelInfo> subsAdded = new ArrayList<>();
        Map<String, ChannelInfo> subsToCheck = new TreeMap<>();


        Timber.v("Entered %s", this.getClass().getSimpleName());

        Context baseContext = this.baseContext.get();
        SubscriptionsDbHelper mDbHelper = new SubscriptionsDbHelper(baseContext);

        // Get the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        Timber.d(TempStorage.getLoadedStreamers().toString());
        // Loop over the provided list of StreamerInfo objects
        for (ChannelInfo subToAdd : subsToAdd) {
            if (subToAdd == null) {
                Timber.d("StreamerInfo fed was null");
                continue;
            }
            // Make sure the streamer is not already in the database
            if (TempStorage.containsLoadedStreamer(subToAdd)) {
                Timber.d("Streamer (" + subToAdd.getLogin() + ") already in database");
                continue;
            }

            ArrayList<String> usersNotToNotifyWhenLive = Settings.getUsersNotToNotifyWhenLive();
            boolean disableForStreamer = usersNotToNotifyWhenLive != null && usersNotToNotifyWhenLive.contains(subToAdd.getUserId());

            // Create a new map of values where column names are the keys
            ContentValues values = new ContentValues();
            values.put(SubscriptionsDbHelper.COLUMN_ID, subToAdd.getUserId());
            values.put(SubscriptionsDbHelper.COLUMN_STREAMER_NAME, subToAdd.getLogin());
            values.put(SubscriptionsDbHelper.COLUMN_DISPLAY_NAME, subToAdd.getDisplayName());
            values.put(SubscriptionsDbHelper.COLUMN_DESCRIPTION, subToAdd.streamDescription);
            values.put(SubscriptionsDbHelper.COLUMN_FOLLOWERS, Objects.requireNonNullElse(subToAdd.fetchFollowers(), 0));
            values.put(SubscriptionsDbHelper.COLUMN_NOTIFY_WHEN_LIVE, subToAdd.isNotifyWhenLive() && !disableForStreamer ? 1 : 0);
            values.put(SubscriptionsDbHelper.COLUMN_IS_TWITCH_FOLLOW, 1);


            // Test if the URL strings are null, to make sure we don't call toString on a null.
            if (subToAdd.logoURL != null)
                values.put(SubscriptionsDbHelper.COLUMN_LOGO_URL, subToAdd.logoURL.toString());

            if (subToAdd.videoBannerURL != null)
                values.put(SubscriptionsDbHelper.COLUMN_VIDEO_BANNER_URL, subToAdd.videoBannerURL.toString());

            if (subToAdd.profileBannerURL != null)
                values.put(SubscriptionsDbHelper.COLUMN_PROFILE_BANNER_URL, subToAdd.profileBannerURL.toString());


            db.insert(SubscriptionsDbHelper.TABLE_NAME, null, values);

            // we return the rowId so we can return the row we inserted to a cursor
            /*
            long rowId = db.insert(SubscriptionsDbHelper.TABLE_NAME, null, values);
            Cursor cursor = db.query(SubscriptionsDbHelper.TABLE_NAME, allColumns, SubscriptionsDbHelper.COLUMN_ID + " = " + rowId, null, null, null, null);
            cursor.close();
            */

            // The StreamerInfo to the result list
            subsAdded.add(subToAdd);

            // And to the map to ensure we can check if they are online
            subsToCheck.put(subToAdd.getLogin(), subToAdd);
        }
        db.close();

        TempStorage.addLoadedStreamer(subsAdded);
        Timber.d("Count of streamers added: %s", subsAdded.size());
        Timber.d("Streamers (" + subsAdded + ") added to database");
    }
}
