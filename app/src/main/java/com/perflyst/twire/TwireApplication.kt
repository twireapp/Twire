package com.perflyst.twire

import android.app.Application
import android.content.Intent
import android.os.Build
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.ITwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.auth.providers.TwitchIdentityProvider
import com.github.twitch4j.helix.TwitchHelix
import com.github.twitch4j.helix.domain.Clip
import com.perflyst.twire.activities.DeepLinkActivity
import com.perflyst.twire.misc.SecretKeys
import com.perflyst.twire.service.ReportErrors
import com.perflyst.twire.service.Settings.generalTwitchAccessToken
import com.perflyst.twire.service.Settings.generalTwitchDisplayName
import com.perflyst.twire.service.Settings.generalTwitchUserID
import com.perflyst.twire.service.Settings.init
import com.perflyst.twire.service.Settings.isLoggedIn
import com.perflyst.twire.service.Settings.reportErrors
import io.github.xanthic.cache.core.CacheApiSettings
import io.github.xanthic.cache.provider.androidx.AndroidLruProvider
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import io.sentry.protocol.App
import io.sentry.protocol.Device
import io.sentry.protocol.OperatingSystem
import org.parceler.Parcel
import org.parceler.ParcelClass
import timber.log.Timber
import timber.log.Timber.DebugTree

/**
 * Created by SebastianRask on 20-02-2016.
 */
@ParcelClass(value = Clip::class, annotation = Parcel(Parcel.Serialization.BEAN))
class TwireApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTree())

        SentryAndroid.init(this) { options: SentryAndroidOptions? ->
            options!!.isEnableDeduplication = false
            options.setBeforeSend { event, hint ->
                event.user = null
                event.breadcrumbs = null
                event.setModules(null)
                event.environment = BuildConfig.BUILD_TYPE
                event.release = BuildConfig.VERSION_NAME
                event.contexts.remove(Device.TYPE)
                event.contexts.put(OperatingSystem.TYPE, object : HashMap<Any, Any>() {
                    init {
                        put("name", "Android")
                        put("version", Build.VERSION.RELEASE)
                    }
                })
                event.contexts.put(App.TYPE, object : HashMap<Any, Any>() {
                    init {
                        put("app_version", BuildConfig.VERSION_NAME)
                    }
                })
                event.removeTag("isSideLoaded")
                event.setTag("os.version", Build.VERSION.RELEASE)
                event.sdk = null

                if (event.getExtra("consent") != null) return@setBeforeSend event

                if (reportErrors == ReportErrors.ASK) {
                    DeepLinkActivity.Companion.SENTRY_EVENTS.add(event)
                    startActivity(
                        Intent(this, DeepLinkActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra("reportErrors", true)
                    )
                } else if (reportErrors == ReportErrors.ALWAYS) {
                    return@setBeforeSend event
                }
                null
            }
        }

        init(this)

        val credentialManager = CredentialManagerBuilder.builder().build()
        updateCredential()

        CacheApiSettings.getInstance().setDefaultCacheProvider(AndroidLruProvider())
        twitchClient = TwitchClientBuilder.builder()
            .withClientId(SecretKeys.APPLICATION_CLIENT_ID)
            .withDefaultAuthToken(credential)
            .withCredentialManager(credentialManager)
            .withEnableHelix(true)
            .withRequestQueueSize(Int.Companion.MAX_VALUE)
            .build()
        helix = twitchClient!!.helix

        identityProvider = credentialManager.getIdentityProviderByName(
            "twitch",
            TwitchIdentityProvider::class.java
        ).get()
    }

    companion object {
        var twitchClient: ITwitchClient? = null
        lateinit var helix: TwitchHelix
        val credential: OAuth2Credential = OAuth2Credential("twitch", "")
        lateinit var identityProvider: TwitchIdentityProvider

        fun updateCredential() {
            val result = if (!isLoggedIn) {
                OAuth2Credential("twitch", "invalid-token")
            } else {
                OAuth2Credential(
                    "twitch",
                    generalTwitchAccessToken,
                    generalTwitchDisplayName,
                    generalTwitchUserID,
                    null,
                    -1,
                    null
                )
            }

            credential.updateCredential(result)
        }
    }
}
