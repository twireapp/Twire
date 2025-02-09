package com.perflyst.twire;

import android.app.Application;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.ITwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.domain.Clip;
import com.perflyst.twire.misc.SecretKeys;
import com.perflyst.twire.service.Settings;

import org.parceler.ParcelClass;

import io.github.xanthic.cache.core.CacheApiSettings;
import io.github.xanthic.cache.provider.androidx.AndroidLruProvider;
import timber.log.Timber;

/**
 * Created by SebastianRask on 20-02-2016.
 */
@ParcelClass(Clip.class)
public class TwireApplication extends Application {
    public static ITwitchClient twitchClient;
    public static TwitchHelix helix;
    public static final OAuth2Credential credential = new OAuth2Credential("twitch", "");
    public static TwitchIdentityProvider identityProvider;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());

        Settings.init(this);

        CredentialManager credentialManager = CredentialManagerBuilder.builder().build();
        updateCredential();

        CacheApiSettings.getInstance().setDefaultCacheProvider(new AndroidLruProvider());
        twitchClient = TwitchClientBuilder.builder()
                .withClientId(SecretKeys.TWITCH_CLIENT_ID)
                .withDefaultAuthToken(credential)
                .withCredentialManager(credentialManager)
                .withEnableHelix(true)
                .withRequestQueueSize(Integer.MAX_VALUE)
                .build();
        helix = twitchClient.getHelix();

        identityProvider = credentialManager.getIdentityProviderByName("twitch", TwitchIdentityProvider.class).get();
    }

    public static void updateCredential() {
        OAuth2Credential result;
        if (!Settings.isLoggedIn()) {
            result = new OAuth2Credential("twitch", "invalid-token");
        } else {
            result = new OAuth2Credential("twitch", Settings.getGeneralTwitchAccessToken(), Settings.getGeneralTwitchDisplayName(), Settings.getGeneralTwitchUserID(), null, -1, null);
        }

        credential.updateCredential(result);
    }
}
