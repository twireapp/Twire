package com.perflyst.twire.tasks;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;

import static com.perflyst.twire.service.Service.SimpleResponse;
import static com.perflyst.twire.service.Service.isNetworkConnectedThreadOnly;
import static com.perflyst.twire.service.Service.makeRequest;

/**
 * Created by Sebastian Rask on 10-05-2016.
 */
public class ValidateOauthTokenTask extends AsyncTask<Void, Void, ValidateOauthTokenTask.TokenValidation> {
    private ValidationDelegate delegate;
    private String oauthToken;
    private WeakReference<Context> context;

    public ValidateOauthTokenTask(ValidationDelegate delegate, String oauthToken, Context context) {
        this.delegate = delegate;
        this.oauthToken = oauthToken;
        this.context = new WeakReference<>(context);
    }

    @Override
    protected TokenValidation doInBackground(Void... params) {
        String url = "https://id.twitch.tv/oauth2/validate";

        try {
            final String USER_ID_STRING = "user_id";
            final String SCOPES_STRING_ARRAY = "scopes";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "OAuth " + oauthToken)
                    .build();

            SimpleResponse response = makeRequest(request);
            if (response == null)
                return null;

            if (response.code == 401)
                return new TokenValidation(null, null);

            String result = response.body;
            JSONObject topObject = new JSONObject(result);
            String user_id = topObject.has(USER_ID_STRING) ? topObject.getString(USER_ID_STRING) : null;

            ArrayList<String> scopes = new ArrayList<>();
            if (topObject.has(SCOPES_STRING_ARRAY)) {
                JSONArray scopeArray = topObject.getJSONArray(SCOPES_STRING_ARRAY);
                for (int i = 0; i < scopeArray.length(); i++) {
                    scopes.add(scopeArray.getString(i));
                }
            }

            return new TokenValidation(user_id, scopes);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (isNetworkConnectedThreadOnly(context.get())) {
            return new TokenValidation("", new ArrayList<>());
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

    public static class TokenValidation {
        private String userID;
        private List<String> scopes;

        TokenValidation(String userID, List<String> scopes) {
            this.userID = userID;
            this.scopes = scopes;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }

        public boolean isTokenValid() {
            return userID != null;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }
    }
}
