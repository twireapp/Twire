package com.perflyst.twire.model

import android.os.Parcelable
import androidx.core.util.Consumer
import com.github.twitch4j.helix.domain.ChannelSearchResult
import com.github.twitch4j.helix.domain.User
import com.perflyst.twire.TwireApplication
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.utils.Execute
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.net.URL
import java.util.Objects

/**
 * Created by Sebastian Rask on 30-01-2015.
 * This class is designed to hold all relevant information about a twitch user/streamer
 */
// TODO: Use this when Kotlin 2.1 comes to Android Studio: https://developer.android.com/kotlin/parcelize#non_val_or_var_parameters_in_primary_constructor
@Parcelize
class ChannelInfo(
    private val followers: Int?,
    @JvmField var streamDescription: String?,
    @JvmField var logoURL: URL?,
    @JvmField val videoBannerURL: URL?,
    @JvmField val profileBannerURL: URL?,
    override var userId: String,
    override var login: String,
    override var displayName: String,
) : UserInfo(userId, login, displayName), Comparable<ChannelInfo>, Parcelable {
    // Parcel Part End
    @IgnoredOnParcel
    var isNotifyWhenLive: Boolean = false

    constructor(
        userInfo: UserInfo,
        streamDescription: String?,
        followers: Int,
        logoURL: URL?,
        videoBannerURL: URL?,
        profileBannerURL: URL?
    ) : this(
        if (followers == -1) null else followers,
        streamDescription,
        logoURL,
        videoBannerURL,
        profileBannerURL,
        userInfo.userId,
        userInfo.login,
        userInfo.displayName,
    )

    constructor(user: User) : this(
        null,
        user.description,
        Utils.safeUrl(user.profileImageUrl),
        Utils.safeUrl(user.offlineImageUrl),
        null,
        user.id,
        user.login,
        user.displayName,
    )

    constructor(channel: ChannelSearchResult) : this(
        null, "", null, null, null,
        channel.id, channel.broadcasterLogin, channel.displayName
    )

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ChannelInfo -> userId == other.userId
            else -> false
        }
    }

    override fun hashCode(): Int = userId.hashCode()

    override fun compareTo(other: ChannelInfo): Int =
        java.lang.String.CASE_INSENSITIVE_ORDER.compare(other.displayName, displayName)

    fun getFollowers(callback: Consumer<Int?>, defaultValue: Int) {
        Execute.background({ this.fetchFollowers() },
            { followers: Int? ->
                callback.accept(
                    Objects.requireNonNullElse(
                        followers,
                        defaultValue
                    )
                )
            })
    }

    fun fetchFollowers(): Int? {
        if (followers != null) {
            return followers
        }

        Timber.d("Fetching followers for $userId")

        val followers =
            TwireApplication.helix.getChannelFollowers(null, userId, null, 1, null).execute()
        return followers.total
    }
}
