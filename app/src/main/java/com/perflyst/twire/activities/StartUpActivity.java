package com.perflyst.twire.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.setup.LoginActivity;
import com.perflyst.twire.activities.setup.WelcomeActivity;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.tasks.ValidateOauthTokenTask;

public class StartUpActivity extends ThemeActivity {
    private final String LOG_TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        final Settings settings = new Settings(getBaseContext());
        final boolean isSetup = settings.isSetup();
        Intent intent;
        if (isSetup) {
            intent = Service.getNotLoggedInIntent(getBaseContext());
            if (settings.isLoggedIn()) {
                validateToken();
                intent = Service.getLoggedInIntent(getBaseContext());
            }

            if (!settings.isNotificationsDisabled()) {
                Service.startNotifications(getBaseContext());
            }
        } else {
            intent = new Intent(getBaseContext(), WelcomeActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void validateToken() {
        ValidateOauthTokenTask validateTask = new ValidateOauthTokenTask(validation -> {
            if (validation != null && !validation.isTokenValid()) {
                Log.e(LOG_TAG, "Token invalid");
                Intent loginIntent = new Intent(getBaseContext(), LoginActivity.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                loginIntent.putExtra(getString(R.string.login_intent_part_of_setup), false);
                loginIntent.putExtra(getString(R.string.login_intent_token_not_valid), true);

                getBaseContext().startActivity(loginIntent);
            }
        }, new Settings(getBaseContext()).getGeneralTwitchAccessToken(), getBaseContext());
        validateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
