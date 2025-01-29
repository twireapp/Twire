package com.perflyst.twire.service;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.drawerlayout.widget.DrawerLayout;

import com.perflyst.twire.R;
import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.activities.main.MyChannelsActivity;
import com.perflyst.twire.activities.main.MyStreamsActivity;
import com.perflyst.twire.activities.main.TopGamesActivity;
import com.perflyst.twire.activities.main.TopStreamsActivity;
import com.perflyst.twire.misc.SecretKeys;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.UserInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

/**
 * Created by Sebastian Rask on 12-02-2015.
 * Class made purely for adding utility methods for other classes
 */
// TODO: Split this service out to multiple more cohesive service classes
public class Service {

    public static final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(3, TimeUnit.SECONDS)
            .build();

    /**
     * Returns the Twitch Client ID
     *
     * @return The ID
     */
    public static String getApplicationClientID() {
        return SecretKeys.TWITCH_CLIENT_ID;
    }

    /**
     * Returns the Twitch Client ID
     *
     * @return The ID
     */
    public static String getTwitchWebClientID() {
        return SecretKeys.TWITCH_WEB_CLIENT_ID;
    }

    public static String getErrorEmote() {
        String[] emotes = {"('.')", "('x')", "(>_<)", "(>.<)", "(;-;)", "\\(o_o)/", "(O_o)", "(o_0)", "(≥o≤)", "(≥o≤)", "(·.·)", "(·_·)"};
        Random rnd = new Random();
        return emotes[rnd.nextInt(emotes.length - 1)];
    }

    /**
     * Creates a bitmap with rounded corners.
     *
     * @param bitmap The bitmap
     * @param i      the corner radius in pixels
     * @return The bitmap with rounded corners
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int i) {
        if (bitmap == null) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, (float) i, (float) i, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * Returns an intent with the right destination activity.
     *
     * @param context The context from which the method is called
     * @return The intent
     */
    public static Intent getStartPageIntent(Context context) {
        String title = Settings.getStartPage();

        Class<?> activityClass = MyStreamsActivity.class;
        if (title.equals(context.getString(R.string.navigation_drawer_follows_title))) {
            activityClass = MyChannelsActivity.class;
        } else if (title.equals(context.getString(R.string.navigation_drawer_top_streams_title))) {
            activityClass = TopStreamsActivity.class;
        } else if (title.equals(context.getString(R.string.navigation_drawer_top_games_title))) {
            activityClass = TopGamesActivity.class;
        }

        Intent intent = new Intent(context, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    /**
     * Animates the background color of a view from one color to another color.
     *
     * @param v         The view to animate
     * @param toColor   The To Color
     * @param fromColor The From Color
     * @param duration  The Duration of the animation
     */
    public static void animateBackgroundColorChange(View v, int toColor, int fromColor, int duration) {
        ObjectAnimator colorFade = ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), fromColor, toColor);
        colorFade.setDuration(duration);
        colorFade.start();
    }

    /**
     * Finds and returns an attribute color. If it was not found the method returns the default color
     */
    public static int getColorAttribute(@AttrRes int attribute, @ColorRes int defaultColor, Context context) {
        TypedValue a = new TypedValue();
        context.getTheme().resolveAttribute(attribute, a, true);
        if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return a.data;
        } else {
            return ContextCompat.getColor(context, defaultColor);
        }
    }

    /**
     * @param view         The view to get the color from
     * @param defaultColor The color to return if the view's background isn't a ColorDrawable
     * @return The color
     */
    public static int getBackgroundColorFromView(View view, int defaultColor) {
        int color = defaultColor;
        Drawable background = view.getBackground();
        if (background instanceof ColorDrawable) {
            color = ((ColorDrawable) background).getColor();
        }

        return color;
    }

    /**
     * Creates a string with a unicode emoticon.
     */
    public static String getEmojiByUnicode(int unicode) {
        return new String(Character.toChars(unicode));
    }

    /**
     * Hides the onscreen keyboard if it is visible
     */
    public static void hideKeyboard(Activity activity) {
        // Check if no view has focus:
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = ContextCompat.getSystemService(activity, InputMethodManager.class);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    /**
     * Shows the soft keyboard
     */
    public static void showKeyboard(Activity activity) {
        // Check if no view has focus:
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager = ContextCompat.getSystemService(activity, InputMethodManager.class);
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(view, 0);
            }
        }
    }

    /**
     * Returns whether the device is a tablet or not.
     */
    public static boolean isTablet(Context context) {
        if (context == null) {
            return false;
        } else {
            return context.getResources().getBoolean(R.bool.isTablet);
        }
    }

    /**
     * Gets the accent color from the current theme
     */
    public static int getAccentColor(Context mContext) {
        TypedValue typedValue = new TypedValue();

        TypedArray a = mContext.obtainStyledAttributes(typedValue.data, new int[] {androidx.appcompat.R.attr.colorAccent});
        int color = a.getColor(0, 0);

        a.recycle();

        return color;
    }

    /**
     * Method for increasing a Navigation Drawer's edge size.
     */
    public static void increaseNavigationDrawerEdge(DrawerLayout aDrawerLayout) {
        // Increase the area from which you can open the navigation drawer.
        try {
            Field mDragger = aDrawerLayout.getClass().getDeclaredField("mLeftDragger");
            mDragger.setAccessible(true);
            ViewDragHelper draggerObj = (ViewDragHelper) mDragger.get(aDrawerLayout);

            Field mEdgeSize = draggerObj.getClass().getDeclaredField("mEdgeSize");
            mEdgeSize.setAccessible(true);
            int edgeSize = mEdgeSize.getInt(draggerObj) * 3;

            mEdgeSize.setInt(draggerObj, edgeSize); //optimal value as for me, you may set any constant in dp
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startNotifications(Context context) {
    }

    /**
     * Does the opposite of the View.bringToFront() method
     *
     * @param v the view you want to send to the back
     */
    public static void bringToBack(final View v) {
        final ViewGroup parent = (ViewGroup) v.getParent();
        if (null != parent) {
            parent.removeView(v);
            parent.addView(v, 0);
        }
    }

    /**
     * Gets the navigation drawer toggle view from a toolbar
     *
     * @param toolbar The toolbar containing the navigation button
     * @return The ImageButton
     */
    public static ImageButton getNavButtonView(Toolbar toolbar) {
        try {
            Class<?> toolbarClass = Toolbar.class;
            Field navButtonField = toolbarClass.getDeclaredField("mNavButtonView");
            navButtonField.setAccessible(true);

            return (ImageButton) navButtonField.get(toolbar);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Returns the height of the device screen
     */
    public static int getScreenHeight(Context context) {
        WindowManager wm = ContextCompat.getSystemService(context, WindowManager.class);
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        if (wm != null) {
            wm.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return displayMetrics.heightPixels;
    }

    public static String urlToJSONString(String urlToRead) {
        // Alright, so sometimes Twitch decides that our client ID should be blocked. Currently only happens for the hidden /api endpoints.
        // IF we are being blocked, then retry the request with Twitch web ClientID. They are typically not blocking this.
        String result = urlToJSONString(urlToRead, true); // "{\"error\":\"Gone\",\"status\":410,\"message\":\"this API has been removed.\"}";
        boolean retryWithWebClientId = false;
        if (result == null || result.isEmpty()) {
            retryWithWebClientId = true;
        } else {
            try {
                JSONObject resultJson = new JSONObject(result);
                int status = resultJson.getInt("status");
                String error = resultJson.getString("error");
                retryWithWebClientId = status == 410 || error.equals("Gone");
            } catch (Exception ignored) {
            }
        }

        if (retryWithWebClientId) {
            result = urlToJSONString(urlToRead, false);
        }

        return result == null ? "" : result;
    }

    public static String urlToJSONString(String urlToRead, Boolean useOurClientId) {
        String clientId;
        if (useOurClientId) {
            clientId = Service.getApplicationClientID();
        } else {
            clientId = Service.getTwitchWebClientID();
        }

        Request request = new Request.Builder()
                .url(urlToRead)
                .header("Client-ID", clientId)
                .header("Accept", "application/vnd.twitchtv.v5+json")
                .build();

        return urlToJSONString(request);
    }

    public static JSONObject graphQL(String operation, String hash, Map<String, Object> variables) {
        String query = "[{\"operationName\":\"" + operation + "\",\"variables\":" + new JSONObject(variables) + ",\"extensions\":{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"" + hash + "\"}}}]";

        Request request = new Request.Builder()
                .url("https://gql.twitch.tv/gql")
                .header("Client-ID", SecretKeys.TWITCH_GRAPHQL_ID)
                .post(RequestBody.create(MediaType.get("application/json"), query))
                .build();

        String result = urlToJSONString(request);
        if (result == null) return null;

        try {
            JSONArray array = new JSONArray(result);
            return array.getJSONObject(0).getJSONObject("data");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String urlToJSONString(Request request) {
        SimpleResponse response = makeRequest(request);
        if (response == null)
            return null;

        String result = response.body;

        if (result == null || result.isEmpty() || result.charAt(0) != '{' && result.charAt(0) != '[') {
            Timber.tag("URL TO JSON STRING").v("%s did not successfully get read", request.url());
            Timber.tag("URL TO JSON STRING").v("Result of reading - %s", result);
        }

        return result;
    }

    public static SimpleResponse makeRequest(Request request) {
        Response response;
        try {
            response = client.newCall(request).execute();
            return new SimpleResponse(response);
        } catch (IOException exception) {
            return null;
        }
    }

    public static ChannelInfo getStreamerInfoFromUserId(String streamerId) {
        var users = TwireApplication.helix.getUsers(null, List.of(streamerId), null).execute().getUsers();
        if (users.isEmpty()) return null;

        return new ChannelInfo(users.get(0));
    }

    /**
     * Connects to the database containing data of user follows. Loops through every record of in the database and creates a StreamerInfo object for these
     */
    public static Map<String, ChannelInfo> getStreamerInfoFromDB(Context context) {
        Map<String, ChannelInfo> subscriptions = new TreeMap<>();
        SubscriptionsDbHelper mDbHelper = new SubscriptionsDbHelper(context);
        final boolean DISTINCT = true;
        String[] allColumns = {
                SubscriptionsDbHelper.COLUMN_ID, SubscriptionsDbHelper.COLUMN_STREAMER_NAME,
                SubscriptionsDbHelper.COLUMN_DISPLAY_NAME, SubscriptionsDbHelper.COLUMN_DESCRIPTION,
                SubscriptionsDbHelper.COLUMN_FOLLOWERS, SubscriptionsDbHelper.COLUMN_UNIQUE_VIEWS,
                SubscriptionsDbHelper.COLUMN_LOGO_URL, SubscriptionsDbHelper.COLUMN_VIDEO_BANNER_URL,
                SubscriptionsDbHelper.COLUMN_PROFILE_BANNER_URL, SubscriptionsDbHelper.COLUMN_NOTIFY_WHEN_LIVE};

        // Get the data repository in read mode
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DISTINCT,
                SubscriptionsDbHelper.TABLE_NAME,
                allColumns,
                null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String streamerId = cursor.getString(0);
                String streamerName = cursor.getString(1);
                String displayName = cursor.getString(2);
                String streamDescription = cursor.getString(3);
                int followers = cursor.getInt(4);
                URL logo = null;
                URL videoBanner = null;
                URL profileBanner = null;
                boolean notifyWhenLive = cursor.getInt(9) == 1;

                // Make sure the streamer has uploaded pictures
                try {
                    if (!cursor.isNull(6))
                        logo = new URL(cursor.getString(6));

                    if (!cursor.isNull(7))
                        videoBanner = new URL(cursor.getString(7));

                    if (!cursor.isNull(8))
                        profileBanner = new URL(cursor.getString(8));


                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                // Create new StreamerInfo object from data fetched from database
                ChannelInfo mChannelInfo = new ChannelInfo(new UserInfo(streamerId, streamerName, displayName),
                        streamDescription, followers, logo, videoBanner, profileBanner);
                mChannelInfo.setNotifyWhenLive(notifyWhenLive);
                subscriptions.put(mChannelInfo.getDisplayName(), mChannelInfo);

                // Move to the next record in the database
                cursor.moveToNext();
            }
        }

        cursor.close();
        db.close();

        return subscriptions;
    }

    /**
     * Determines whether or not the user is currently following a streamer.
     * This is done by looking in the SQLite database
     */
    public static boolean isUserFollowingStreamer(String streamername, Context context) {
        SubscriptionsDbHelper mDbHelper = new SubscriptionsDbHelper(context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + SubscriptionsDbHelper.TABLE_NAME + " WHERE " + SubscriptionsDbHelper.COLUMN_STREAMER_NAME + "='" + streamername + "';";
        boolean result = false;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.getCount() > 0) {
            result = true;
        }
        cursor.close();
        db.close();
        return result;
    }

    public static boolean isUserTwitch(String streamerId, Context context) {
        SubscriptionsDbHelper mDbHelper = new SubscriptionsDbHelper(context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + SubscriptionsDbHelper.TABLE_NAME + " WHERE " + SubscriptionsDbHelper.COLUMN_ID + "='" + streamerId + "';";
        boolean result = false;
        Cursor cursor = db.rawQuery(query, null);
        int columnIndex = cursor.getColumnIndex(SubscriptionsDbHelper.COLUMN_IS_TWITCH_FOLLOW);
        if (cursor.moveToFirst()) {
            result = cursor.getInt(columnIndex) > 0;
        }
        cursor.close();
        db.close();
        return result;
    }

    public static void updateStreamerInfoDbWithValues(ContentValues values, Context context, String id) {
        updateStreamerInfoDbWithValues(values, context, SubscriptionsDbHelper.COLUMN_ID + "=?", new String[] {id});
    }

    private static void updateStreamerInfoDbWithValues(ContentValues values, Context context, String whereClause, String[] whereArgs) {
        SubscriptionsDbHelper helper = new SubscriptionsDbHelper(context);

        try {
            SQLiteDatabase db = helper.getWritableDatabase();

            if (isDbSafe(db)) {
                db.update(SubscriptionsDbHelper.TABLE_NAME, values, whereClause, whereArgs);
            }

            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isDbSafe(SQLiteDatabase db) {
        return db.isOpen() && !db.isReadOnly() && !db.isDbLockedByCurrentThread();
    }

    public static boolean deleteStreamerInfoFromDB(Context context, ChannelInfo channelInfo) {
        SubscriptionsDbHelper mDbHelper = new SubscriptionsDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase(); // Get the data repository in write mode

        boolean result = false;
        String streamerId = channelInfo.getUserId();
        if (!isUserTwitch(streamerId, context)) {
            result = db.delete(SubscriptionsDbHelper.TABLE_NAME, SubscriptionsDbHelper.COLUMN_ID + " = '" + streamerId + "'", null) > 0;

            if (result)
                TempStorage.removeLoadedStreamer(channelInfo);
        }

        db.close();

        return result;
    }

    public static void insertStreamerInfoToDB(Context context, ChannelInfo streamer) {
        ArrayList<String> usersNotToNotifyWhenLive = Settings.getUsersNotToNotifyWhenLive();
        boolean disableForStreamer = usersNotToNotifyWhenLive != null && usersNotToNotifyWhenLive.contains(streamer.getUserId());

        // Create a new map of values where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SubscriptionsDbHelper.COLUMN_ID, streamer.getUserId());
        values.put(SubscriptionsDbHelper.COLUMN_STREAMER_NAME, streamer.getLogin());
        values.put(SubscriptionsDbHelper.COLUMN_DISPLAY_NAME, streamer.getDisplayName());
        values.put(SubscriptionsDbHelper.COLUMN_DESCRIPTION, streamer.streamDescription);
        values.put(SubscriptionsDbHelper.COLUMN_UNIQUE_VIEWS, 0);
        values.put(SubscriptionsDbHelper.COLUMN_NOTIFY_WHEN_LIVE, disableForStreamer ? 0 : 1); // Enable by default
        values.put(SubscriptionsDbHelper.COLUMN_IS_TWITCH_FOLLOW, 0);


        // Test if the URL strings are null, to make sure we don't call toString on a null.
        if (streamer.logoURL != null)
            values.put(SubscriptionsDbHelper.COLUMN_LOGO_URL, streamer.logoURL.toString());

        if (streamer.videoBannerURL != null)
            values.put(SubscriptionsDbHelper.COLUMN_VIDEO_BANNER_URL, streamer.videoBannerURL.toString());

        if (streamer.profileBannerURL != null)
            values.put(SubscriptionsDbHelper.COLUMN_PROFILE_BANNER_URL, streamer.profileBannerURL.toString());


        streamer.getFollowers(followers -> {
            values.put(SubscriptionsDbHelper.COLUMN_FOLLOWERS, followers);
            SubscriptionsDbHelper helper = new SubscriptionsDbHelper(context);
            SQLiteDatabase db = helper.getWritableDatabase();
            db.insert(SubscriptionsDbHelper.TABLE_NAME, null, values);
            db.close();
        }, 0);

        TempStorage.addLoadedStreamer(streamer);
    }

    public static void clearStreamerInfoDb(Context context) {
        Timber.i("CLEARING STREAMERINFO DATABASE");
        TempStorage.getLoadedStreamers().clear();
        SubscriptionsDbHelper helper = new SubscriptionsDbHelper(context);
        helper.onUpgrade(helper.getWritableDatabase(), SubscriptionsDbHelper.DATABASE_VERSION, SubscriptionsDbHelper.DATABASE_VERSION + 1);
    }

    public static int dpToPixels(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static class SimpleResponse {
        public int code;
        public String body;
        public Response response;

        public SimpleResponse(Response response) {
            assert response.body() != null;

            code = response.code();
            this.response = response;

            try {
                body = response.body().string();
            } catch (IOException ignored) {
            }
        }
    }

}
