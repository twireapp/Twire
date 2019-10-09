package com.perflyst.twire.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.perflyst.twire.service.Service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sebastian Rask on 10-05-2016.
 */
public class ValidateOauthTokenTask extends AsyncTask<Void, Void, ValidateOauthTokenTask.TokenValidation> {
    private String LOG_TAG = getClass().getSimpleName();
    private ValidationDelegate delegate;
    private String oauthToken;
    private Context context;

    public ValidateOauthTokenTask(ValidationDelegate delegate, String oauthToken, Context context) {
        this.delegate = delegate;
        this.oauthToken = oauthToken;
        this.context = context;
    }

    @Override
    protected TokenValidation doInBackground(Void... params) {
        String baseUrl = "https://api.twitch.tv/kraken?oauth_token=";

        try {
            final String TOKEN_OBJECT = "token";
            final String TWITCHNAME_STRING = "user_name";
            final String IS_TOKEN_VALID_BOOLEAN = "valid";
            final String AUTHORIZATION_OBJECT = "authorization";
            final String SCOPES_STRING_ARRAY = "scopes";

            JSONObject topObject = new JSONObject(Service.urlToJSONString(baseUrl + oauthToken));
            JSONObject tokenObject = topObject.getJSONObject(TOKEN_OBJECT);
            JSONArray scopeArray = tokenObject.getJSONObject(AUTHORIZATION_OBJECT).getJSONArray(SCOPES_STRING_ARRAY);

            String username = tokenObject.getString(TWITCHNAME_STRING);
            boolean isTokenValid = tokenObject.getBoolean(IS_TOKEN_VALID_BOOLEAN);
            ArrayList<String> scopes = new ArrayList<>();
            for (int i = 0; i < scopeArray.length(); i++) {
                scopes.add(scopeArray.getString(i));
            }

            return new TokenValidation(username, isTokenValid, scopes);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (Service.isNetworkConnectedThreadOnly(context)) {
            return new TokenValidation("", true, new ArrayList<>());
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(TokenValidation tokenValidation) {
        super.onPostExecute(tokenValidation);
        delegate.onFinished(tokenValidation);
    }

    @Override
    protected void onCancelled(TokenValidation validation) {
        super.onCancelled(validation);
        delegate.onFinished(null);
    }

    public interface ValidationDelegate {
        void onFinished(TokenValidation validation);
    }

    public class TokenValidation {
        private String twitchName;
        private boolean tokenValid;
        private List<String> scopes;

        TokenValidation(String twitchName, boolean tokenValid, List<String> scopes) {
            this.twitchName = twitchName;
            this.tokenValid = tokenValid;
            this.scopes = scopes;
        }

        public String getTwitchName() {
            return twitchName;
        }

        public void setTwitchName(String twitchName) {
            this.twitchName = twitchName;
        }

        public boolean isTokenValid() {
            return tokenValid;
        }

        public void setTokenValid(boolean tokenValid) {
            this.tokenValid = tokenValid;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }
    }
}
