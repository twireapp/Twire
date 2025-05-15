package com.perflyst.twire.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.perflyst.twire.R
import com.perflyst.twire.TwireApplication
import com.perflyst.twire.activities.stream.ClipActivity.Companion.createClipIntent
import com.perflyst.twire.activities.stream.LiveStreamActivity
import com.perflyst.twire.activities.stream.VODActivity
import com.perflyst.twire.model.ChannelInfo
import com.perflyst.twire.model.StreamInfo
import com.perflyst.twire.model.VideoOnDemand
import com.perflyst.twire.service.DialogService
import com.perflyst.twire.service.ReportErrors
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.reportErrors
import io.sentry.Sentry
import io.sentry.SentryEvent
import timber.log.Timber
import java.util.LinkedList
import java.util.Objects
import java.util.Queue

class DeepLinkActivity : AppCompatActivity() {
    private var errorMessage = R.string.router_unknown_error
    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)

        if (intent.hasExtra("reportErrors")) {
            reportErrorDialog()
            return
        }

        val data = getUri(intent)
        val params: MutableList<String> = LinkedList<String>(data!!.pathSegments)

        // twitch.tv/<channel>/video/<id> -> twitch.tv/videos/<id>
        if (params.size == 3 && (params[1] == "video" || params[1] == "v")) {
            params[1] = "videos"
            params.removeAt(0)
        }

        val paramSize = params.size

        Thread {
            var intent: Intent? = null
            try {
                intent = getNewIntent(params, paramSize)
            } catch (exception: Exception) {
                Timber.e(exception)
            }
            if (intent == null) {
                runOnUiThread {
                    DialogService.getRouterErrorDialog(this, errorMessage).show()
                }
            } else {
                startActivity(intent)
            }
        }.start()
    }

    private fun reportErrorDialog() {
        if (SENTRY_EVENTS.isEmpty()) return

        DialogService.getBaseThemedDialog(this)
            .title(R.string.report_error_title)
            .content(R.string.report_error_description)
            .positiveText(R.string.report_error_always)
            .negativeText(R.string.report_error_never)
            .onAny { dialog: MaterialDialog?, action: DialogAction? ->
                when (action) {
                    DialogAction.POSITIVE -> {
                        reportErrors = ReportErrors.ALWAYS
                        while (!SENTRY_EVENTS.isEmpty()) Sentry.captureEvent(
                            Objects.requireNonNull(
                                SENTRY_EVENTS.poll()
                            )
                        )
                    }

                    DialogAction.NEGATIVE -> {
                        reportErrors = ReportErrors.NEVER
                        SENTRY_EVENTS.clear()
                    }

                    else -> {}
                }
                finish()
            }.build().show()
    }


    @Throws(Exception::class)
    fun getNewIntent(params: MutableList<String>, paramSize: Int): Intent? {
        if (paramSize == 2 && params[0] == "videos") { // twitch.tv/videos/<id>
            errorMessage = R.string.router_vod_error

            val videos = TwireApplication.helix.getVideos(
                null,
                listOf(params[1]),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ).execute().videos
            if (videos.isEmpty()) return null

            val video = videos[0]
            val vod = VideoOnDemand(video)

            val users =
                TwireApplication.helix.getUsers(null, listOf<String?>(video.userId), null)
                    .execute().users
            if (users.isEmpty()) return null

            vod.channelInfo = ChannelInfo(users[0]!!)

            return VODActivity.createVODIntent(vod, this, false)
        } else if (paramSize == 1) { // twitch.tv/<channel>
            errorMessage = R.string.router_channel_error

            val streams = TwireApplication.helix.getStreams(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                listOf(params[0])
            ).execute().streams
            if (!streams.isEmpty()) {
                val stream = StreamInfo(streams[0]!!)
                return LiveStreamActivity.createLiveStreamIntent(stream, false, this)
            }

            val users = TwireApplication.helix.getUsers(null, null, listOf(params[0]))
                .execute().users
            if (!users.isEmpty()) {
                // If we can't load the stream, try to show the user's channel instead.
                val info = ChannelInfo(users[0]!!)
                return Intent(this, ChannelActivity::class.java)
                    .putExtra(getString(R.string.channel_info_intent_object), info)
            }

            errorMessage = R.string.router_nonexistent_user
            return null
        } else if (paramSize == 3) { // twitch.tv/<channel>/clip/<id>
            val clips = TwireApplication.helix.getClips(
                null,
                null,
                null,
                listOf(params[2]),
                null,
                null,
                1,
                null,
                null,
                false
            ).execute().data
            if (clips.isEmpty()) return null

            val clip = clips[0]
            val channel = Service.getStreamerInfoFromUserId(clip.broadcasterId)

            return createClipIntent(clip, channel, this, false)
        }

        return null
    }

    fun getUri(intent: Intent): Uri? {
        if (intent.data != null) {
            return intent.data!!
        } else if (intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
            return getUriFromString(intent.getStringExtra(Intent.EXTRA_TEXT)!!)
        }

        return null
    }

    fun getUriFromString(string: String): Uri? {
        val matcher = Patterns.WEB_URL.matcher(string)
        if (matcher.find()) {
            return matcher.group(0)?.toUri()
        }

        return null
    }

    companion object {
        @JvmField
        val SENTRY_EVENTS: Queue<SentryEvent> = LinkedList<SentryEvent>()
    }
}
