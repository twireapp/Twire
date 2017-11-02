package com.sebastianrask.bettersubscription.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.sebastianrask.bettersubscription.activities.setup.LoginActivity;
import com.sebastianrask.bettersubscription.model.StreamInfo;
import com.sebastianrask.bettersubscription.service.Service;
import com.sebastianrask.bettersubscription.service.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by SebastianRask on 03-11-2015.
 */
public class HandlerUserLoginTask extends AsyncTask<Object, Void, Object[]> {
	private final String BASE_USER_INFO_URL = "https://api.twitch.tv/kraken/user?oauth_token=";
	private final String LOG_TAG = "HandleUserLoginTask";
	private Context mContext;
	private String token;
	private LoginActivity mLoginActivity;

	// This is the keys to get information from the JSONObject
	private final String USER_DISPLAY_NAME_STRING 	= "display_name";
	private final String USER_ID_INT 				= "_id";
	private final String USER_NAME_STRING 			= "name";
	private final String USER_TYPE_STRING 			= "type";
	private final String USER_BIO_STRING 			= "bio";
	private final String USER_LOGO_URL_STRING 		= "logo";
	private final String USER_CREATION_DATE_STRING 	= "created_at";
	private final String USER_UPDATED_DATE_STRING 	= "updated_at";
	private final String USER_EMAIL_ADDRESS_STRING 	= "email";
	private final String USER_IS_PARTNERED_BOOLEAN 	= "partnered";

	@Override
	protected void onPreExecute(){

	}

	@Override
	protected Object[] doInBackground(Object... params) {
		mContext 		= (Context) params[0];
		token 			= (String) params[1];
		mLoginActivity 	= (LoginActivity) params[2];

		try  {
			String jsonString = Service.urlToJSONString(BASE_USER_INFO_URL + token);
			Log.d(LOG_TAG, "JSON: " + jsonString);

			JSONObject baseJSON = new JSONObject(jsonString);
			String mDisplayName 	= baseJSON.getString(USER_DISPLAY_NAME_STRING);
			String mUserName 		= baseJSON.getString(USER_NAME_STRING);
			String mUserBio 		= baseJSON.getString(USER_BIO_STRING);
			String mLogoURL 		= baseJSON.getString(USER_LOGO_URL_STRING);
			String mEmail 			= baseJSON.getString(USER_EMAIL_ADDRESS_STRING);
			String mCreatedAtDate 	= baseJSON.getString(USER_CREATION_DATE_STRING);
			String mUpdateAtDate 	= baseJSON.getString(USER_UPDATED_DATE_STRING);
			String mUserType 		= baseJSON.getString(USER_TYPE_STRING);
			boolean isPartner 		= baseJSON.getBoolean(USER_IS_PARTNERED_BOOLEAN);
			int mID 				= baseJSON.getInt(USER_ID_INT);

			Object[] resultArray = {
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

			return resultArray;

		} catch (JSONException e) {
			Log.e(LOG_TAG, "CAUGHT EXCEPTION " + e.getMessage() + " WHILE HANDLING USER LOGIN");
		}

		return null;
	}

	@Override
	protected void onPostExecute(Object[] mUserInfo) {
		Settings mSettings = new Settings(mContext);
		if(mUserInfo != null) {
			mSettings.setGeneralTwitchAccessToken(token);
			mSettings.setGeneralTwitchDisplayName((String) mUserInfo[0]);
			mSettings.setGeneralTwitchName((String) mUserInfo[1]);
			//mSettings.setGeneralTwitchUserEmail((String) mUserInfo[4]);
			mSettings.setGeneralTwitchUserCreatedDate((String) mUserInfo[5]);
			mSettings.setGeneralTwitchUserType((String) mUserInfo[7]);
			mSettings.setGeneralTwitchUserIsPartner((boolean) mUserInfo[8]);
			mSettings.setGeneralTwitchUserID((int) mUserInfo[9]);

			if(mUserInfo[2] != null)
				mSettings.setGeneralTwitchUserBio((String) mUserInfo[2]);

			if(mUserInfo[3] != null)
				mSettings.setGeneralTwitchUserLogo((String) mUserInfo[3]);

			if(mUserInfo[6] != null)
				mSettings.setGeneralTwitchUserUpdatedDate((String) mUserInfo[6]);

			mLoginActivity.handleLoginSuccess();
		} else {
			mLoginActivity.handleLoginFailure();
		}
	}
}