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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
import com.perflyst.twire.activities.main.FeaturedStreamsActivity;
import com.perflyst.twire.activities.main.MyChannelsActivity;
import com.perflyst.twire.activities.main.MyStreamsActivity;
import com.perflyst.twire.activities.main.TopGamesActivity;
import com.perflyst.twire.activities.main.TopStreamsActivity;
import com.perflyst.twire.misc.SecretKeys;
import com.perflyst.twire.model.ChannelInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
     * Checks if two calendar objects have the same day of the year
     *
     * @param one I think it's pretty obvious
     * @param two what these two objects are for
     * @return True if the day is the same, otherwise false
     */
    public static boolean isCalendarSameDay(Calendar one, Calendar two) {
        return one.get(Calendar.YEAR) == two.get(Calendar.YEAR) && one.get(Calendar.DAY_OF_YEAR) == two.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Makes a timestamp from a length in seconds.
     *
     * @param videoLengthInSeconds Length in seconds
     */
    public static String calculateTwitchVideoLength(int videoLengthInSeconds) {
        String result = "";
        double hours = videoLengthInSeconds / 60.0 / 60.0;

        double minutesAsDecimalHours = hours - Math.floor(hours);
        double minutes = 60.0 * minutesAsDecimalHours;
        double secondsAsDecimalMinutes = minutes - Math.floor(minutes);
        double seconds = 60.0 * secondsAsDecimalMinutes;

        if (hours >= 1) {
            result = (int) Math.floor(hours) + ":";
        }

        result += numberToTime(minutes) + ":" + numberToTime(Math.round(seconds));

        return result;
    }

    /**
     * Converts Double to time. f.eks. 4.5 becomes "04"
     */
    private static String numberToTime(double time) {
        int timeInt = (int) Math.floor(time);

        if (timeInt < 10) {
            return "0" + timeInt;
        } else {
            return "" + timeInt;
        }
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
     * Returns the class related to the user-selected startup page
     *
     * @param context The Context from which the method is called
     * @return The Class of the startup activity
     */
    public static Class getClassFromStartPageTitle(Context context, String title) {
        Class result = MyStreamsActivity.class;
        if (title.equals(context.getString(R.string.navigation_drawer_featured_title))) {
            result = FeaturedStreamsActivity.class;
        } else if (title.equals(context.getString(R.string.navigation_drawer_follows_title))) {
            result = MyChannelsActivity.class;
        } else if (title.equals(context.getString(R.string.navigation_drawer_top_streams_title))) {
            result = TopStreamsActivity.class;
        } else if (title.equals(context.getString(R.string.navigation_drawer_top_games_title))) {
            result = TopGamesActivity.class;
        }

        return result;
    }

    /**
     * Returns an intent with the right destination activity for when the user is logged in.
     *
     * @param context The context from which the method is called
     * @return The intent
     */
    public static Intent getLoggedInIntent(Context context) {
        Class startPageClass = getClassFromStartPageTitle(context, new Settings(context).getStartPage());
        return new Intent(context, startPageClass);
    }

    /**
     * Returns an intent with the right destination activity for when the user is NOT logged in.
     *
     * @param context The context from which the method is called
     * @return The intent
     */

    public static Intent getNotLoggedInIntent(Context context) {
        Settings settings = new Settings(context);
        Class startPageClass = getClassFromStartPageTitle(context, settings.getStartPage());
        if (startPageClass == MyStreamsActivity.class ||
                startPageClass == MyChannelsActivity.class) {
            startPageClass = getClassFromStartPageTitle(context, settings.getDefaultNotLoggedInStartUpPageTitle());
        }
        return new Intent(context, startPageClass);
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
     * Converts a drawable to a bitmap and returns it.
     */
    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
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
                inputMethodManager.toggleSoftInputFromWindow(view.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
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

        TypedArray a = mContext.obtainStyledAttributes(typedValue.data, new int[]{R.attr.colorAccent});
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

    /**
     * Checks if the device is connected to a valid network
     * Can only be called on a thread
     */
    public static boolean isNetworkConnectedThreadOnly(Context context) {
        ConnectivityManager cm = ContextCompat.getSystemService(context, ConnectivityManager.class);
        NetworkInfo networkInfo = null;
        if (cm != null) {
            networkInfo = cm.getActiveNetworkInfo();
        }

        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            try {
                HttpURLConnection urlc = (HttpURLConnection)
                        new URL("https://clients3.google.com/generate_204")
                                .openConnection();
                urlc.setRequestProperty("User-Agent", "Android");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1500);
                urlc.connect();
                return urlc.getResponseCode() == 204 &&
                        urlc.getContentLength() == 0;
            } catch (IOException e) {
                Log.e("SERVICE", "Error checking internet connection", e);
            }
        } else {
            Log.d("SERVICE", "No network available!");
        }

        return false;
    }

    public static void startNotifications(Context context) {
    }

    public static void isTranslucentActionbar(String LOG_TAG, Context context, Toolbar toolbar, Activity activity) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            Log.d(LOG_TAG, "Settings translucent status bar");

            double statusBarHeight = Math.ceil(25 * context.getResources().getDisplayMetrics().density);

            Window w = activity.getWindow(); // in Activity's onCreate() for instance
            //w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            toolbar.getLayoutParams().height = (int) (context.getResources().getDimension(R.dimen.main_toolbar_height) + statusBarHeight);
        }
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
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
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

    private static String urlToJSONString(String urlToRead, Boolean useOurClientId) {
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

    public static String urlToJSONString(Request request) {
        SimpleResponse response = makeRequest(request);
        if (response == null)
            return null;

        String result = response.body;

        if (result.isEmpty() || result.length() >= 1 && result.charAt(0) != '{' && result.charAt(0) != '[') {
            Log.v("URL TO JSON STRING", request.url() + " did not successfully get read");
            Log.v("URL TO JSON STRING", "Result of reading - " + result);
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

    public static HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    public static ChannelInfo getStreamerInfoFromUserId(int streamerId) throws NullPointerException {

        ChannelInfo channelInfo = null;
        try {
            JSONObject JSONString = new JSONObject(urlToJSONString("https://api.twitch.tv/kraken/channels/" + streamerId));

            int userId = JSONString.getInt("_id");
            String displayName = JSONString.getString("display_name");
            String name = JSONString.getString("name");
            int followers = JSONString.getInt("followers");
            int views = JSONString.getInt("views");
            URL logoURL = null;
            URL videoBannerURL = null;
            URL profileBannerURL = null;

            // Make sure streamer has actually set the pictures
            if (!JSONString.isNull("logo")) {
                logoURL = new URL(JSONString.getString("logo"));
            }
            if (!JSONString.isNull("video_banner")) {
                videoBannerURL = new URL(JSONString.getString("video_banner"));
            }
            if (!JSONString.isNull("profile_banner")) {
                profileBannerURL = new URL(JSONString.getString("profile_banner"));
            }

            JSONObject JSONStringTwo = new JSONObject(urlToJSONString("https://api.twitch.tv/kraken/users/" + streamerId));
            String description = JSONStringTwo.getString("bio");

            channelInfo = new ChannelInfo(userId, name, displayName, description, followers, views, logoURL, videoBannerURL, profileBannerURL, false);

        } catch (JSONException e) {
            Log.v("Service: ", e.getMessage());
        } catch (MalformedURLException ef) {
            Log.v("Service : ", ef.getMessage());
        }

        return channelInfo;
    }

    /**
     * Connects to the database containing data of user follows. Loops through every record of in the database and creates a StreamerInfo object for these
     */
    public static Map<String, ChannelInfo> getStreamerInfoFromDB(Context context, boolean includeThumbnails) {
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
                int streamerId = cursor.getInt(0);
                String streamerName = cursor.getString(1);
                String displayName = cursor.getString(2);
                String streamDescription = cursor.getString(3);
                int followers = cursor.getInt(4);
                int views = cursor.getInt(5);
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
                ChannelInfo mChannelInfo = new ChannelInfo(streamerId, streamerName, displayName,
                        streamDescription, followers, views, logo, videoBanner, profileBanner, includeThumbnails);
                mChannelInfo.setNotifyWhenLive(notifyWhenLive);
                subscriptions.put(mChannelInfo.getStreamerName(), mChannelInfo);

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

    private static void updateStreamerInfoDbWithValues(ContentValues values, Context context, String streamerName) {
        updateStreamerInfoDbWithValues(values, context, SubscriptionsDbHelper.COLUMN_STREAMER_NAME + "=?", new String[]{streamerName});
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

    public static boolean deleteStreamerInfoFromDB(Context context, Integer streamerId) {
        SubscriptionsDbHelper mDbHelper = new SubscriptionsDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase(); // Get the data repository in write mode

        boolean result = db.delete(SubscriptionsDbHelper.TABLE_NAME, SubscriptionsDbHelper.COLUMN_ID + " = '" + streamerId + "'", null) > 0;
        db.close();

        return result;
    }

    public static void insertStreamerInfoToDB(Context context, ChannelInfo streamer) {
        ArrayList<Integer> usersNotToNotifyWhenLive = new Settings(context).getUsersNotToNotifyWhenLive();
        boolean disableForStreamer = usersNotToNotifyWhenLive != null && usersNotToNotifyWhenLive.contains(streamer.getUserId());

        // Create a new map of values where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SubscriptionsDbHelper.COLUMN_ID, streamer.getUserId());
        values.put(SubscriptionsDbHelper.COLUMN_STREAMER_NAME, streamer.getStreamerName());
        values.put(SubscriptionsDbHelper.COLUMN_DISPLAY_NAME, streamer.getDisplayName());
        values.put(SubscriptionsDbHelper.COLUMN_DESCRIPTION, streamer.getStreamDescription());
        values.put(SubscriptionsDbHelper.COLUMN_FOLLOWERS, streamer.getFollowers());
        values.put(SubscriptionsDbHelper.COLUMN_UNIQUE_VIEWS, streamer.getViews());
        values.put(SubscriptionsDbHelper.COLUMN_NOTIFY_WHEN_LIVE, disableForStreamer ? 0 : 1); // Enable by default


        // Test if the URL strings are null, to make sure we don't call toString on a null.
        if (streamer.getLogoURL() != null)
            values.put(SubscriptionsDbHelper.COLUMN_LOGO_URL, streamer.getLogoURL().toString());

        if (streamer.getVideoBannerURL() != null)
            values.put(SubscriptionsDbHelper.COLUMN_VIDEO_BANNER_URL, streamer.getVideoBannerURL().toString());

        if (streamer.getProfileBannerURL() != null)
            values.put(SubscriptionsDbHelper.COLUMN_PROFILE_BANNER_URL, streamer.getProfileBannerURL().toString());

        SubscriptionsDbHelper helper = new SubscriptionsDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.insert(SubscriptionsDbHelper.TABLE_NAME, null, values);
        db.close();


    }

    public static void clearStreamerInfoDb(Context context) {
        Log.i("SERVICE", "CLEARING STREAMERINFO DATABASE");
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
