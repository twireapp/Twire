package com.perflyst.twire;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Application;
import android.content.Intent;
import android.os.Build;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.ITwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.domain.Clip;
import com.perflyst.twire.activities.DeepLinkActivity;
import com.perflyst.twire.misc.SecretKeys;
import com.perflyst.twire.service.ReportErrors;
import com.perflyst.twire.service.Settings;

import org.parceler.Parcel;
import org.parceler.ParcelClass;

import java.util.HashMap;

import io.github.xanthic.cache.core.CacheApiSettings;
import io.github.xanthic.cache.provider.androidx.AndroidLruProvider;
import io.sentry.android.core.SentryAndroid;
import io.sentry.protocol.App;
import io.sentry.protocol.Device;
import io.sentry.protocol.OperatingSystem;
import timber.log.Timber;

/**
 * Created by SebastianRask on 20-02-2016.
 */
@ParcelClass(value = Clip.class, annotation = @Parcel(Parcel.Serialization.BEAN))
public class TwireApplication extends Application {
    public static ITwitchClient twitchClient;
    public static TwitchHelix helix;
    public static final OAuth2Credential credential = new OAuth2Credential("twitch", "");
    public static TwitchIdentityProvider identityProvider;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());

        SentryAndroid.init(this, options -> {
            options.setEnableDeduplication(false);
            options.setBeforeSend((event, hint) -> {
                event.setUser(null);
                event.setBreadcrumbs(null);
                event.setModules(null);
                event.setEnvironment(BuildConfig.BUILD_TYPE);
                event.setRelease(BuildConfig.VERSION_NAME);
                event.getContexts().remove(Device.TYPE);
                event.getContexts().put(OperatingSystem.TYPE, new HashMap<>() {{
                    put("name", "Android");
                    put("version", Build.VERSION.RELEASE);
                }});
                event.getContexts().put(App.TYPE, new HashMap<>() {{
                    put("app_version", BuildConfig.VERSION_NAME);
                }});
                event.removeTag("isSideLoaded");
                event.setTag("os.version", Build.VERSION.RELEASE);
                event.setSdk(null);

                if (event.getExtra("consent") != null) return event;

                if (Settings.getReportErrors() == ReportErrors.ASK) {
                    DeepLinkActivity.SENTRY_EVENTS.add(event);
                    startActivity(new Intent(this, DeepLinkActivity.class)
                            .addFlags(FLAG_ACTIVITY_NEW_TASK)
                            .putExtra("reportErrors", true));
                } else if (Settings.getReportErrors() == ReportErrors.ALWAYS) {
                    return event;
                }

                return null;
            });
        });

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
