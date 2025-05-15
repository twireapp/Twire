package com.perflyst.twire

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.PositionInfo
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.perflyst.twire.lowlatency.LLHlsPlaylistParserFactory
import com.perflyst.twire.service.Settings.setVodProgress
import com.perflyst.twire.service.Settings.skipSilence

@UnstableApi
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player: ExoPlayer = ExoPlayer.Builder(this, mediaSourceFactory)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .setHandleAudioBecomingNoisy(true)
            .build()
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback(player)).build()

        player.addListener(object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: PositionInfo,
                newPosition: PositionInfo,
                reason: Int
            ) {
                if (oldPosition.mediaItem === newPosition.mediaItem || oldPosition.mediaItem == null || oldPosition.mediaItem!!.mediaId.isEmpty()) return

                setVodProgress(oldPosition.mediaItem!!.mediaId, oldPosition.positionMs)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                val media = player.currentMediaItem
                if (media != null) {
                    if (media.mediaId.isEmpty()) {
                        player.seekToDefaultPosition() // Go forward to live
                    } else if (!playWhenReady) {
                        setVodProgress(media.mediaId, player.currentPosition)
                    }
                }
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession!!.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession!!.player.clearMediaItems()
        mediaSession!!.player.release()
        mediaSession!!.release()
        mediaSession = null
        super.onDestroy()
    }

    class MediaSessionCallback(private val player: ExoPlayer) : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ConnectionResult {
            val sessionCommands = ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(UPDATE_SKIP_SILENCE, Bundle.EMPTY))
                .build()

            val playerCommands = ConnectionResult.DEFAULT_PLAYER_COMMANDS
                .buildUpon()
                .removeAll(Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_NEXT)
                .build()

            return AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == UPDATE_SKIP_SILENCE) {
                player.skipSilenceEnabled = skipSilence
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            return super.onCustomCommand(session, controller, customCommand, args)
        }

        companion object {
            const val UPDATE_SKIP_SILENCE: String = "UPDATE_SKIP_SILENCE"
        }
    }

    companion object {
        private val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Twire")
            .setDefaultRequestProperties(object : HashMap<String, String>() {
                init {
                    put("Referer", "https://player.twitch.tv")
                    put("Origin", "https://player.twitch.tv")
                }
            })

        private val mediaSourceFactory: MediaSource.Factory = object : MediaSource.Factory {
            override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
                return this
            }

            override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
                return this
            }

            override fun getSupportedTypes(): IntArray {
                return intArrayOf(
                    C.CONTENT_TYPE_HLS,
                    C.CONTENT_TYPE_OTHER
                )
            }

            override fun createMediaSource(mediaItem: MediaItem): MediaSource {
                checkNotNull(mediaItem.localConfiguration)

                val type: @C.ContentType Int = Util.inferContentTypeForUriAndMimeType(
                    mediaItem.localConfiguration!!.uri,
                    mediaItem.localConfiguration!!.mimeType
                )

                if (type == C.CONTENT_TYPE_HLS) {
                    return HlsMediaSource.Factory(dataSourceFactory)
                        .setPlaylistParserFactory(LLHlsPlaylistParserFactory())
                        .createMediaSource(mediaItem)
                }

                return DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
            }
        }

        fun sendSkipSilenceUpdate(mediaController: MediaController) {
            mediaController.sendCustomCommand(
                SessionCommand(
                    MediaSessionCallback.Companion.UPDATE_SKIP_SILENCE,
                    Bundle.EMPTY
                ), Bundle.EMPTY
            )
        }
    }
}

