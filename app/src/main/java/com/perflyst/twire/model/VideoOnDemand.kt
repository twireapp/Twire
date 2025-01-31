package com.perflyst.twire.model

import android.content.Context
import android.os.Parcelable
import com.github.twitch4j.helix.domain.Video
import com.perflyst.twire.R
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Created by Sebastian Rask on 16-06-2016.
 */
@Parcelize
class VideoOnDemand(
    @JvmField val videoTitle: String?,
    @JvmField val gameTitle: String?,
    override val previewTemplate: String,
    @JvmField val videoId: String?,
    private val channelName: String?,
    @JvmField val displayName: String?,
    @JvmField var recordedAt: ZonedDateTime,
    @JvmField val views: Int,
    @JvmField val length: Long,
    @JvmField var isBroadcast: Boolean = false,
    @JvmField var channelInfo: ChannelInfo? = null
) : Comparable<VideoOnDemand>, Parcelable, MainElement {
    constructor(video: Video) : this(
        video.title,
        "",
        video.thumbnailUrl,
        video.id,
        video.userLogin,
        video.userName,
        video.publishedAtInstant.atZone(ZoneOffset.UTC),
        video.viewCount,
        Duration.parse("PT" + video.duration).seconds
    )

    override fun compareTo(other: VideoOnDemand): Int {
        return recordedAt.compareTo(other.recordedAt)
    }

    override fun getPlaceHolder(context: Context): Int {
        return R.drawable.template_stream
    }
}
