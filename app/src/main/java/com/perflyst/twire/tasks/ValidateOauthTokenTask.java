package com.perflyst.twire.tasks;

import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.utils.Constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;

/**
 * Created by Sebastian Rask on 10-05-2016.
 */
public class ValidateOauthTokenTask implements Callable<String> {

    public ValidateOauthTokenTask() {
    }

    public String call() {
        var response = TwireApplication.identityProvider.getAdditionalCredentialInformation(TwireApplication.credential).orElse(null);
        if (response == null) return null;

        // Validate that all required scopes are present
        if (!new HashSet<>(response.getScopes()).containsAll(Arrays.asList(Constants.TWITCH_SCOPES)))
            return null;

        return response.getUserId();
    }
}
