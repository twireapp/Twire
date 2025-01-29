package com.perflyst.twire.tasks;

import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.activities.setup.LoginActivity;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.utils.Execute;

import java.lang.ref.WeakReference;

/**
 * Created by SebastianRask on 03-11-2015.
 */
public class HandlerUserLoginTask implements Runnable {
    private final WeakReference<LoginActivity> mLoginActivity;

    public HandlerUserLoginTask(LoginActivity mLoginActivity) {
        this.mLoginActivity = new WeakReference<>(mLoginActivity);
    }

    public void run() {
        // the User is fetched by the Bearer token
        Settings.setLoggedIn(true);
        var users = TwireApplication.helix.getUsers(null, null, null).execute();
        var user = users.getUsers().get(0);

        Settings.setGeneralTwitchDisplayName(user.getDisplayName());
        Settings.setGeneralTwitchName(user.getLogin());
        //mSettings.setGeneralTwitchUserEmail((String) mUserInfo[4]);
        Settings.setGeneralTwitchUserCreatedDate(user.getCreatedAt().toString());
        Settings.setGeneralTwitchUserType(user.getType());
        Settings.setGeneralTwitchUserIsPartner(user.getBroadcasterType().equals("partner"));
        Settings.setGeneralTwitchUserID(user.getId());

        if (user.getDescription() != null)
            Settings.setGeneralTwitchUserBio(user.getDescription());

        if (user.getProfileImageUrl() != null)
            Settings.setGeneralTwitchUserLogo(user.getProfileImageUrl());

        if (user.getCreatedAt() != null)
            Settings.setGeneralTwitchUserUpdatedDate(user.getCreatedAt().toString());

        // Now that we have the user, update the credential
        TwireApplication.updateCredential();

        Execute.ui(() -> mLoginActivity.get().handleLoginSuccess());
    }
}
