package com.perflyst.twire.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
    public static final String COLUMN_IS_TWITCH_FOLLOW = "isTwitchFollow";
    static final int DATABASE_VERSION = 8;
    private static final String DATABASE_NAME = "SubscriptionsDb_09.db";
    private static final String EXPORT_NAME = "export.json";
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
                    COLUMN_NOTIFY_WHEN_LIVE + " INTEGER, " +
                    COLUMN_IS_TWITCH_FOLLOW + " INTEGER" +
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

    // Wipe

    public void onWipe(SQLiteDatabase db, boolean isLoggedin) {
        if (!db.isOpen()) {
            return;
        }
        String query;
        if (isLoggedin) {
            query = "DELETE FROM " + SubscriptionsDbHelper.TABLE_NAME + " WHERE " + SubscriptionsDbHelper.COLUMN_IS_TWITCH_FOLLOW + " == 0;";
        } else {
            query = "DELETE FROM " + SubscriptionsDbHelper.TABLE_NAME + ";";
        }
        Log.d("query", query);
        db.execSQL(query);
    }

    // Export import below

    public int onImport(SQLiteDatabase db) {
        if (!db.isOpen()) {
            return 0;
        }
        try {
            String filedata = read(mContext, EXPORT_NAME);
            if (filedata == null) {
                return 0;
            }
            JSONObject channelsfile = new JSONObject(filedata);
            JSONArray channels = channelsfile.getJSONArray("Channels");
            for (int i = 0; i < channels.length(); i++) {
                JSONObject tempchannel = channels.getJSONObject(i);

                ContentValues values = new ContentValues();
                // these can´t be empty
                values.put(SubscriptionsDbHelper.COLUMN_ID, tempchannel.getInt("ID"));
                values.put(SubscriptionsDbHelper.COLUMN_STREAMER_NAME, tempchannel.getString("NAME"));
                values.put(SubscriptionsDbHelper.COLUMN_DISPLAY_NAME, tempchannel.getString("DISPLAY_NAME"));
                values.put(SubscriptionsDbHelper.COLUMN_DESCRIPTION, tempchannel.getString("DESCRIPTION"));
                values.put(SubscriptionsDbHelper.COLUMN_FOLLOWERS, tempchannel.getInt("FOLLOWERS"));
                values.put(SubscriptionsDbHelper.COLUMN_UNIQUE_VIEWS, tempchannel.getInt("VIEWS"));
                values.put(SubscriptionsDbHelper.COLUMN_NOTIFY_WHEN_LIVE, tempchannel.getInt("NOTIFY"));
                values.put(SubscriptionsDbHelper.COLUMN_IS_TWITCH_FOLLOW, tempchannel.getInt("IS_TWITCH"));

                // these could be empty
                if (tempchannel.has("LOGO"))
                    values.put(SubscriptionsDbHelper.COLUMN_LOGO_URL, tempchannel.getString("LOGO"));

                if (tempchannel.has("BANNER"))
                    values.put(SubscriptionsDbHelper.COLUMN_VIDEO_BANNER_URL, tempchannel.getString("BANNER"));

                if (tempchannel.has("PROFILE_BANNER"))
                    values.put(SubscriptionsDbHelper.COLUMN_PROFILE_BANNER_URL, tempchannel.getString("PROFILE_BANNER"));

                // check what methode should be used
                String sql = "SELECT * FROM " + SubscriptionsDbHelper.TABLE_NAME + " WHERE _ID=" + tempchannel.getInt("ID");
                Cursor cursor = db.rawQuery(sql,null);

                if (cursor.getCount()>0) {
                    db.replace(SubscriptionsDbHelper.TABLE_NAME, null, values);
                } else {
                    db.insert(SubscriptionsDbHelper.TABLE_NAME, null, values);
                }

            }
            return channels.length();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int onExport(SQLiteDatabase db) {
        String query = "SELECT * FROM " + SubscriptionsDbHelper.TABLE_NAME + ";";
        Cursor cursor = db.rawQuery(query, null);

        JSONArray channelstoExport = new JSONArray();

        while (cursor.moveToNext()) {
            int idPosition = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_ID);
            int streamerNamePosition = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_STREAMER_NAME);
            int displayNamePosition = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_DISPLAY_NAME);
            int bioPosition = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_DESCRIPTION);
            int followersPosition = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_FOLLOWERS);
            int viewsPosition = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_UNIQUE_VIEWS);
            int logoPosition = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_LOGO_URL);
            int bannerPosition = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_VIDEO_BANNER_URL);
            int banner_profilePosition = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_PROFILE_BANNER_URL);
            int notifyPosition = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_NOTIFY_WHEN_LIVE);
            int istwitchPosition = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_IS_TWITCH_FOLLOW);

            int userId = cursor.getInt(idPosition);
            String name = cursor.getString(streamerNamePosition);
            String displayname = cursor.getString(displayNamePosition);
            String description = cursor.getString(bioPosition);
            int followers = cursor.getInt(followersPosition);
            int views = cursor.getInt(viewsPosition);
            String logo = cursor.getString(logoPosition);
            String banner = cursor.getString(bannerPosition);
            String profilebanner = cursor.getString(banner_profilePosition);
            int notify = cursor.getInt(notifyPosition);
            int isTwitch = cursor.getInt(istwitchPosition);

            try {
                JSONObject tempchannel = new JSONObject();
                tempchannel.put("ID", userId);
                tempchannel.put("NAME", name);
                tempchannel.put("DISPLAY_NAME", displayname);
                tempchannel.put("DESCRIPTION", description);
                tempchannel.put("FOLLOWERS", followers);
                tempchannel.put("VIEWS", views);
                tempchannel.put("LOGO", logo);
                tempchannel.put("BANNER", banner);
                tempchannel.put("PROFILE_BANNER", profilebanner);
                tempchannel.put("NOTIFY", notify);
                tempchannel.put("IS_TWITCH", isTwitch);
                channelstoExport.put(tempchannel);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        cursor.close();

        if (channelstoExport.length()>0) {
            try {
                JSONObject channels = new JSONObject();
                channels.put("Channels", channelstoExport);
                String jsonStr = channels.toString();
                Log.d("Export String", jsonStr);
                
                create(mContext, EXPORT_NAME, jsonStr);
                return channelstoExport.length();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private String read(Context context, String fileName) {
        try {
            ContextWrapper cw = new ContextWrapper(context);
            File directory;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                directory = cw.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            } else {
                directory = android.os.Environment.getExternalStorageDirectory();
            }
            File importfile = new File(directory, fileName);
            FileInputStream fis = new FileInputStream(importfile);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (FileNotFoundException fileNotFound) {
            return null;
        } catch (IOException ioException) {
            return null;
        }
    }

    private void create(Context context, String fileName, String jsonString) {
        try {
            ContextWrapper cw = new ContextWrapper(context);
            File directory;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                directory = cw.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            } else {
                directory = android.os.Environment.getExternalStorageDirectory();
            }
            File exportfile = new File(directory, fileName);
            FileOutputStream fos = new FileOutputStream(exportfile);
            fos.write(jsonString.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
