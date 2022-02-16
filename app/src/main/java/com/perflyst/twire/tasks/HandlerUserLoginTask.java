package com.perflyst.twire.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.perflyst.twire.activities.setup.LoginActivity;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Created by SebastianRask on 03-11-2015.
 */
public class HandlerUserLoginTask extends AsyncTask<Object, Void, Object[]> {
    private final String LOG_TAG = getClass().getSimpleName();
    private Settings mSettings;
    private String token;
    private WeakReference<LoginActivity> mLoginActivity;

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected Object[] doInBackground(Object... params) {
        mSettings = new Settings((Context) params[0]);
        token = (String) params[1];
        mLoginActivity = new WeakReference<>((LoginActivity) params[2]);

        try {
            // the User is fetched by the Bearer token
            String BASE_USER_INFO_URL = "https://api.twitch.tv/helix/users";
            String jsonString = Service.urlToJSONStringHelix(BASE_USER_INFO_URL, mSettings.getContext());
            Log.d(LOG_TAG, "JSON: " + jsonString);

            JSONObject baseJSON = new JSONObject(jsonString).getJSONArray("data").getJSONObject(0);
            // This is the keys to get information from the JSONObject
            String USER_DISPLAY_NAME_STRING = "display_name";
            String mDisplayName = baseJSON.getString(USER_DISPLAY_NAME_STRING);
            String USER_NAME_STRING = "login";
            String mUserName = baseJSON.getString(USER_NAME_STRING);
            String USER_BIO_STRING = "description";
            String mUserBio = baseJSON.getString(USER_BIO_STRING);
            String USER_LOGO_URL_STRING = "profile_image_url";
            String mLogoURL = baseJSON.getString(USER_LOGO_URL_STRING);
            String USER_EMAIL_ADDRESS_STRING = "email";
            String mEmail = baseJSON.getString(USER_EMAIL_ADDRESS_STRING);
            String USER_CREATION_DATE_STRING = "created_at";
            String mCreatedAtDate = baseJSON.getString(USER_CREATION_DATE_STRING);
            String mUpdateAtDate = baseJSON.getString(USER_CREATION_DATE_STRING);
            String USER_TYPE_STRING = "type";
            String mUserType = baseJSON.getString(USER_TYPE_STRING);
            String USER_TYPE = "broadcaster_type";
            boolean isPartner = baseJSON.getString(USER_TYPE) == "partner";
            String USER_ID_INT = "id";
            int mID = baseJSON.getInt(USER_ID_INT);

            return new Object[]{
                    mDisplayName,
                    mUserName,
                    mUserBio,
                    mLogoURL,
                    mEmail,
                    mCreatedAtDate,
                    mUpdateAtDate,
                    mUserType,
                    isPartner,
                    mID
            };

        } catch (JSONException e) {
            Log.e(LOG_TAG, "CAUGHT EXCEPTION " + e.getMessage() + " WHILE HANDLING USER LOGIN");
        }

        return null;
    }

    @Override
    protected void onPostExecute(Object[] mUserInfo) {
        if (mUserInfo != null) {
            mSettings.setGeneralTwitchAccessToken(token);
            mSettings.setGeneralTwitchDisplayName((String) mUserInfo[0]);
            mSettings.setGeneralTwitchName((String) mUserInfo[1]);
            //mSettings.setGeneralTwitchUserEmail((String) mUserInfo[4]);
            mSettings.setGeneralTwitchUserCreatedDate((String) mUserInfo[5]);
            mSettings.setGeneralTwitchUserType((String) mUserInfo[7]);
            mSettings.setGeneralTwitchUserIsPartner((boolean) mUserInfo[8]);
            mSettings.setGeneralTwitchUserID((int) mUserInfo[9]);

            if (mUserInfo[2] != null)
                mSettings.setGeneralTwitchUserBio((String) mUserInfo[2]);

            if (mUserInfo[3] != null)
                mSettings.setGeneralTwitchUserLogo((String) mUserInfo[3]);

            if (mUserInfo[6] != null)
                mSettings.setGeneralTwitchUserUpdatedDate((String) mUserInfo[6]);

            mLoginActivity.get().handleLoginSuccess();
        }
    }
}
