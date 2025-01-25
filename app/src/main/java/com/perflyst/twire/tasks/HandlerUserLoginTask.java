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
        var mSettings = new Settings(mLoginActivity.get());
        mSettings.setLogin(true);
        var users = TwireApplication.helix.getUsers(null, null, null).execute();
        var user = users.getUsers().get(0);

        mSettings.setGeneralTwitchDisplayName(user.getDisplayName());
        mSettings.setGeneralTwitchName(user.getLogin());
        //mSettings.setGeneralTwitchUserEmail((String) mUserInfo[4]);
        mSettings.setGeneralTwitchUserCreatedDate(user.getCreatedAt().toString());
        mSettings.setGeneralTwitchUserType(user.getType());
        mSettings.setGeneralTwitchUserIsPartner(user.getBroadcasterType().equals("partner"));
        mSettings.setGeneralTwitchUserID(user.getId());

        if (user.getDescription() != null)
            mSettings.setGeneralTwitchUserBio(user.getDescription());

        if (user.getProfileImageUrl() != null)
            mSettings.setGeneralTwitchUserLogo(user.getProfileImageUrl());

        if (user.getCreatedAt() != null)
            mSettings.setGeneralTwitchUserUpdatedDate(user.getCreatedAt().toString());

        // Now that we have the user, update the credential
        TwireApplication.updateCredential(mLoginActivity.get());

        Execute.ui(() -> mLoginActivity.get().handleLoginSuccess());
    }
}
