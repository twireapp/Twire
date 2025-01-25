package com.perflyst.twire.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.setup.LoginActivity;
import com.perflyst.twire.activities.setup.WelcomeActivity;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.tasks.ValidateOauthTokenTask;
import com.perflyst.twire.utils.Execute;

import timber.log.Timber;

public class StartUpActivity extends ThemeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        // Don't dismiss the splash screen
        final View content = findViewById(android.R.id.content);
        content.getViewTreeObserver().addOnPreDrawListener(() -> false);

        final Settings settings = new Settings(getBaseContext());
        final boolean isSetup = settings.isSetup();
        Intent intent;
        if (isSetup) {
            intent = Service.getStartPageIntent(getBaseContext());
            if (settings.isLoggedIn()) {
                validateToken();
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
        Execute.background(new ValidateOauthTokenTask(), validation -> {
            if (validation == null) {
                Timber.e("Token invalid");
                Intent loginIntent = new Intent(getBaseContext(), LoginActivity.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                loginIntent.putExtra(getString(R.string.login_intent_part_of_setup), false);
                loginIntent.putExtra(getString(R.string.login_intent_token_not_valid), true);

                getBaseContext().startActivity(loginIntent);
            }
        });
    }
}
