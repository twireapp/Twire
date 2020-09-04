package com.perflyst.twire.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * Created by SebastianRask on 27-01-2015.
 * Helper for creating and maintaining our database for StreamerInfo objects
 */
public class SubscriptionsDbHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "Subscriptions";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_STREAMER_NAME = "streamerName";
    public static final String COLUMN_DISPLAY_NAME = "displayName";
    public static final String COLUMN_DESCRIPTION = "biography";
    public static final String COLUMN_FOLLOWERS = "followers";
    public static final String COLUMN_UNIQUE_VIEWS = "views";
    public static final String COLUMN_LOGO_URL = "logoURL";
    public static final String COLUMN_VIDEO_BANNER_URL = "videoBannerURL";
    public static final String COLUMN_PROFILE_BANNER_URL = "profileBannerURL";
    public static final String COLUMN_NOTIFY_WHEN_LIVE = "notifityWhenLive";
    static final int DATABASE_VERSION = 7;
    private static final String DATABASE_NAME = "SubscriptionsDb_09.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_STREAMER_NAME + " TEXT NOT NULL UNIQUE, " +
                    COLUMN_DISPLAY_NAME + " TEXT, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_FOLLOWERS + " INTEGER, " +
                    COLUMN_UNIQUE_VIEWS + " INTEGER, " +
                    COLUMN_LOGO_URL + " TEXT, " +
                    COLUMN_VIDEO_BANNER_URL + " TEXT, " +
                    COLUMN_PROFILE_BANNER_URL + " TEXT, " +
                    COLUMN_NOTIFY_WHEN_LIVE + " INTEGER" +
                    " );";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;

    private final Context mContext;

    public SubscriptionsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            if (newVersion == 7) {
                new Settings(mContext).setUsersNotToNotifyWhenLive(getUsersNotToNotify(db));
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }


        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
        //db.close();
    }

    private ArrayList<Integer> getUsersNotToNotify(SQLiteDatabase db) throws SQLiteException {
        String query = "SELECT * FROM " + SubscriptionsDbHelper.TABLE_NAME + " WHERE " + SubscriptionsDbHelper.COLUMN_NOTIFY_WHEN_LIVE + "=" + 0 + ";";
        Cursor cursor = db.rawQuery(query, null);

        ArrayList<Integer> usersToNotify = new ArrayList<>();

        while (cursor.moveToNext()) {
            int idPosition = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_ID);
            int userId = cursor.getInt(idPosition);

            usersToNotify.add(userId);
        }

        cursor.close();

        return usersToNotify;
    }
}
