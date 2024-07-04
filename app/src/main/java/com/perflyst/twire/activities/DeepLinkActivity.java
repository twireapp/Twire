package com.perflyst.twire.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.perflyst.twire.R;
import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.activities.stream.ClipActivity;
import com.perflyst.twire.activities.stream.LiveStreamActivity;
import com.perflyst.twire.activities.stream.VODActivity;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.model.VideoOnDemand;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.ReportErrors;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.regex.Matcher;

import io.sentry.Sentry;
import io.sentry.SentryEvent;
import timber.log.Timber;

public class DeepLinkActivity extends AppCompatActivity {
    private int errorMessage = R.string.router_unknown_error;
    public static final Queue<SentryEvent> SENTRY_EVENTS = new LinkedList<>();

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        if (getIntent().hasExtra("reportErrors")) {
            reportErrorDialog();
            return;
        }

        Uri data = getUri(getIntent());
        List<String> params = new LinkedList<>(data.getPathSegments());

        // twitch.tv/<channel>/video/<id> -> twitch.tv/videos/<id>
        if (params.size() == 3 && (params.get(1).equals("video") || params.get(1).equals("v"))) {
            params.set(1, "videos");
            params.remove(0);
        }

        int paramSize = params.size();

        new Thread(() -> {
            Intent intent = null;
            try {
                intent = getNewIntent(params, paramSize);
            } catch (Exception exception) {
                Timber.e(exception);
            }

            if (intent == null) {
                runOnUiThread(() -> DialogService.getRouterErrorDialog(this, errorMessage).show());
            } else {
                startActivity(intent);
            }
        }).start();
    }

    private void reportErrorDialog() {
        if (SENTRY_EVENTS.isEmpty()) return;

        DialogService.getBaseThemedDialog(this)
                .title(R.string.report_error_title)
                .content(R.string.report_error_description)
                .positiveText(R.string.report_error_always)
                .negativeText(R.string.report_error_never)
                .onAny((_dialog, action) -> {
                    switch (action) {
                        case POSITIVE:
                            Settings.setReportErrors(ReportErrors.ALWAYS);
                            while (!SENTRY_EVENTS.isEmpty())
                                Sentry.captureEvent(Objects.requireNonNull(SENTRY_EVENTS.poll()));
                            break;
                        case NEGATIVE:
                            Settings.setReportErrors(ReportErrors.NEVER);
                            SENTRY_EVENTS.clear();
                            break;
                    }

                    finish();
                }).build().show();
    }


    Intent getNewIntent(List<String> params, int paramSize) throws Exception {
        if (paramSize == 2 && params.get(0).equals("videos")) { // twitch.tv/videos/<id>
            errorMessage = R.string.router_vod_error;

            var videos = TwireApplication.helix.getVideos(null, List.of(params.get(1)), null, null, null, null, null, null, null, null, null).execute().getVideos();
            if (videos.isEmpty()) return null;

            var video = videos.get(0);
            VideoOnDemand vod = new VideoOnDemand(video);

            var users = TwireApplication.helix.getUsers(null, List.of(video.getUserId()), null).execute().getUsers();
            if (users.isEmpty()) return null;

            vod.channelInfo = new ChannelInfo(users.get(0));

            return VODActivity.createVODIntent(vod, this, false);
        } else if (paramSize == 1) { // twitch.tv/<channel>
            errorMessage = R.string.router_channel_error;

            var streams = TwireApplication.helix.getStreams(null, null, null, null, null, null, null, List.of(params.get(0))).execute().getStreams();
            if (!streams.isEmpty()) {
                StreamInfo stream = new StreamInfo(streams.get(0));
                return LiveStreamActivity.createLiveStreamIntent(stream, false, this);
            }

            var users = TwireApplication.helix.getUsers(null, null, List.of(params.get(0))).execute().getUsers();
            if (!users.isEmpty()) {
                // If we can't load the stream, try to show the user's channel instead.
                ChannelInfo info = new ChannelInfo(users.get(0));
                return new Intent(this, ChannelActivity.class)
                        .putExtra(getString(R.string.channel_info_intent_object), info);
            }

            errorMessage = R.string.router_nonexistent_user;
            return null;
        } else if (paramSize == 3) { // twitch.tv/<channel>/clip/<id>
            var clips = TwireApplication.helix.getClips(null, null, null, List.of(params.get(2)), null, null, 1, null, null, false).execute().getData();
            if (clips.isEmpty()) return null;

            var clip = clips.get(0);
            var channel = Service.getStreamerInfoFromUserId(clip.getBroadcasterId());

            return ClipActivity.createClipIntent(clip, channel, this, false);
        }

        return null;
    }

    Uri getUri(Intent intent) {
        if (intent.getData() != null) {
            return intent.getData();
        } else if (intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
            return getUriFromString(intent.getStringExtra(Intent.EXTRA_TEXT));
        }

        return null;
    }

    Uri getUriFromString(String string) {
        Matcher matcher = Patterns.WEB_URL.matcher(string);
        if (matcher.find()) {
            return Uri.parse(matcher.group(0));
        }

        return null;
    }
}
