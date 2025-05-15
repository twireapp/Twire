package com.perflyst.twire.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.perflyst.twire.R
import com.perflyst.twire.activities.setup.LoginActivity
import com.perflyst.twire.activities.setup.WelcomeActivity
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.isLoggedIn
import com.perflyst.twire.service.Settings.isNotificationsDisabled
import com.perflyst.twire.service.Settings.isSetup
import com.perflyst.twire.tasks.ValidateOauthTokenTask
import com.perflyst.twire.utils.Execute
import timber.log.Timber

class StartUpActivity : ThemeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_up)

        // Don't dismiss the splash screen
        val content = findViewById<View>(android.R.id.content)
        content.getViewTreeObserver()
            .addOnPreDrawListener { false }

        val isSetup = isSetup
        val intent: Intent?
        if (isSetup) {
            intent = Service.getStartPageIntent(baseContext)
            if (isLoggedIn) {
                validateToken()
            }

            if (!isNotificationsDisabled) {
                Service.startNotifications(baseContext)
            }
        } else {
            intent = Intent(baseContext, WelcomeActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun validateToken() {
        Execute.background(ValidateOauthTokenTask()) { validation: String? ->
            if (validation == null) {
                Timber.e("Token invalid")
                val loginIntent = Intent(baseContext, LoginActivity::class.java)
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                loginIntent.putExtra(getString(R.string.login_intent_part_of_setup), false)
                loginIntent.putExtra(getString(R.string.login_intent_token_not_valid), true)

                baseContext.startActivity(loginIntent)
            }
        }
    }
}
