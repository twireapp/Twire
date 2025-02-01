package com.perflyst.twire;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.session.MediaController;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionResult;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.perflyst.twire.lowlatency.LLHlsPlaylistParserFactory;
import com.perflyst.twire.service.Settings;

import java.util.HashMap;

@UnstableApi
public class PlaybackService extends MediaSessionService {
    private MediaSession mediaSession = null;

    private static final DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
            .setUserAgent("Twire")
            .setDefaultRequestProperties(new HashMap<>() {{
                put("Referer", "https://player.twitch.tv");
                put("Origin", "https://player.twitch.tv");
            }});

    private static final MediaSource.Factory mediaSourceFactory = new HlsMediaSource.Factory(dataSourceFactory)
            .setPlaylistParserFactory(new LLHlsPlaylistParserFactory());

    @Override
    public void onCreate() {
        super.onCreate();
        ExoPlayer player = new ExoPlayer.Builder(this, mediaSourceFactory)
                .setSeekParameters(SeekParameters.CLOSEST_SYNC)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setHandleAudioBecomingNoisy(true)
                .build();
        mediaSession = new MediaSession.Builder(this, player)
                .setCallback(new MediaSessionCallback(player)).build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition, @NonNull Player.PositionInfo newPosition, int reason) {
                if (oldPosition.mediaItem == newPosition.mediaItem || oldPosition.mediaItem == null || oldPosition.mediaItem.mediaId.isEmpty())
                    return;

                Settings.setVodProgress(oldPosition.mediaItem.mediaId, oldPosition.positionMs);
            }

            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                var media = player.getCurrentMediaItem();
                if (media != null) {
                    if (media.mediaId.isEmpty()) {
                        player.seekToDefaultPosition(); // Go forward to live
                    } else if (!playWhenReady) {
                        Settings.setVodProgress(media.mediaId, player.getCurrentPosition());
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        Player player = mediaSession.getPlayer();
        if (!player.getPlayWhenReady() || player.getMediaItemCount() == 0) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        mediaSession.getPlayer().clearMediaItems();
        mediaSession.getPlayer().release();
        mediaSession.release();
        mediaSession = null;
        super.onDestroy();
    }

    public static void sendSkipSilenceUpdate(MediaController mediaController) {
        mediaController.sendCustomCommand(new SessionCommand(MediaSessionCallback.UPDATE_SKIP_SILENCE, Bundle.EMPTY), Bundle.EMPTY);
    }

    public static class MediaSessionCallback implements MediaSession.Callback {
        public static final String UPDATE_SKIP_SILENCE = "UPDATE_SKIP_SILENCE";

        private final ExoPlayer player;

        public MediaSessionCallback(ExoPlayer player) {
            this.player = player;
        }

        @NonNull
        @Override
        public MediaSession.ConnectionResult onConnect(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
            SessionCommands sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    .buildUpon()
                    .add(new SessionCommand(UPDATE_SKIP_SILENCE, Bundle.EMPTY))
                    .build();

            Player.Commands playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                    .buildUpon()
                    .removeAll(Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_NEXT)
                    .build();

            return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(playerCommands)
                    .build();
        }

        @NonNull
        @Override
        public ListenableFuture<SessionResult> onCustomCommand(
                @NonNull MediaSession session,
                @NonNull MediaSession.ControllerInfo controller,
                @NonNull SessionCommand customCommand,
                @NonNull Bundle args) {
            if (customCommand.customAction.equals(UPDATE_SKIP_SILENCE)) {
                player.setSkipSilenceEnabled(Settings.getSkipSilence());
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            }

            return MediaSession.Callback.super.onCustomCommand(session, controller, customCommand, args);
        }
    }
}

