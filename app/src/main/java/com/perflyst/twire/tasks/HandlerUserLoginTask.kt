package com.perflyst.twire.tasks

import com.perflyst.twire.TwireApplication
import com.perflyst.twire.activities.setup.LoginActivity
import com.perflyst.twire.service.Settings.generalTwitchDisplayName
import com.perflyst.twire.service.Settings.generalTwitchName
import com.perflyst.twire.service.Settings.generalTwitchUserBio
import com.perflyst.twire.service.Settings.generalTwitchUserCreatedDate
import com.perflyst.twire.service.Settings.generalTwitchUserID
import com.perflyst.twire.service.Settings.generalTwitchUserIsPartner
import com.perflyst.twire.service.Settings.generalTwitchUserLogo
import com.perflyst.twire.service.Settings.generalTwitchUserType
import com.perflyst.twire.service.Settings.generalTwitchUserUpdatedDate
import com.perflyst.twire.service.Settings.isLoggedIn
import com.perflyst.twire.utils.Execute
import java.lang.ref.WeakReference

/**
 * Created by SebastianRask on 03-11-2015.
 */
class HandlerUserLoginTask(mLoginActivity: LoginActivity?) : Runnable {
    private val mLoginActivity: WeakReference<LoginActivity?> =
        WeakReference<LoginActivity?>(mLoginActivity)

    override fun run() {
        // the User is fetched by the Bearer token
        isLoggedIn = true
        val users = TwireApplication.helix.getUsers(null, null, null).execute()
        val user = users.users[0]

        generalTwitchDisplayName = user.displayName
        generalTwitchName = user.login
        //mSettings.setGeneralTwitchUserEmail((String) mUserInfo[4]);
        generalTwitchUserCreatedDate = user.createdAt.toString()
        generalTwitchUserType = user.type
        generalTwitchUserIsPartner = user.broadcasterType == "partner"
        generalTwitchUserID = user.id

        if (user.description != null) generalTwitchUserBio = user.description

        if (user.profileImageUrl != null) generalTwitchUserLogo = user.profileImageUrl

        if (user.createdAt != null) generalTwitchUserUpdatedDate =
            user.createdAt.toString()

        // Now that we have the user, update the credential
        TwireApplication.updateCredential()

        Execute.ui { mLoginActivity.get()!!.handleLoginSuccess() }
    }
}
