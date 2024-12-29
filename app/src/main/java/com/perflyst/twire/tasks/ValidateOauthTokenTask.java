package com.perflyst.twire.tasks;

import static com.perflyst.twire.service.Service.SimpleResponse;
import static com.perflyst.twire.service.Service.isNetworkConnectedThreadOnly;
import static com.perflyst.twire.service.Service.makeRequest;

import android.content.Context;

import com.perflyst.twire.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

import okhttp3.Request;

/**
 * Created by Sebastian Rask on 10-05-2016.
 */
public class ValidateOauthTokenTask implements Callable<ValidateOauthTokenTask.TokenValidation> {
    private final String oauthToken;
    private final WeakReference<Context> context;

    public ValidateOauthTokenTask(String oauthToken, Context context) {
        this.oauthToken = oauthToken;
        this.context = new WeakReference<>(context);
    }

    public TokenValidation call() {
        String url = "https://id.twitch.tv/oauth2/validate";

        try {
            final String USER_ID_STRING = "user_id";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "OAuth " + oauthToken)
                    .build();

            SimpleResponse response = makeRequest(request);
            if (response == null)
                return null;

            if (response.code == 401)
                return new TokenValidation(null);

            String result = response.body;
            JSONObject topObject = new JSONObject(result);
            String user_id = topObject.has(USER_ID_STRING) ? topObject.getString(USER_ID_STRING) : null;

            // Validate that all required scopes are present
            JSONArray scopes = topObject.getJSONArray("scopes");
            for (String required_scope : Constants.TWITCH_SCOPES) {
                boolean scope_found = false;
                for (int i = 0; i < scopes.length(); i++) {
                    if (scopes.getString(i).equals(required_scope)) {
                        scope_found = true;
                        break;
                    }
                }

                if (!scope_found) {
                    // The token is invalid because it lacks the required scopes.
                    return new TokenValidation(null);
                }
            }

            return new TokenValidation(user_id);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (isNetworkConnectedThreadOnly(context.get())) {
            return new TokenValidation("");
        } else {
            return null;
        }
    }

    public static class TokenValidation {
        private final String userID;

        TokenValidation(String userID) {
            this.userID = userID;
        }

        public boolean isTokenValid() {
            return userID != null;
        }

    }
}
