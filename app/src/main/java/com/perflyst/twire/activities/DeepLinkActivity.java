package com.perflyst.twire.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.stream.LiveStreamActivity;
import com.perflyst.twire.activities.stream.VODActivity;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.model.VideoOnDemand;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.JSONService;
import com.perflyst.twire.service.Service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import static java.lang.Integer.parseInt;

public class DeepLinkActivity extends AppCompatActivity {
    private int errorMessage = R.string.router_unknown_error;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

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
                exception.printStackTrace();
            }

            if (intent == null) {
                runOnUiThread(() -> DialogService.getRouterErrorDialog(this, errorMessage).show());
            } else {
                startActivity(intent);
            }
        }).start();
    }

    Intent getNewIntent(List<String> params, int paramSize) throws Exception {
        Intent intent = null;
        if (paramSize == 2 && params.get(0).equals("videos")) { // twitch.tv/videos/<id>
            errorMessage = R.string.router_vod_error;

            int vodId = parseInt(params.get(1));
            JSONObject jsonObject = new JSONObject(Service.urlToJSONString("https://api.twitch.tv/kraken/videos/" + vodId));
            VideoOnDemand vod = JSONService.getVod(jsonObject);
            vod.setChannelInfo(JSONService.getStreamerInfo(jsonObject.getJSONObject("channel"), true));

            intent = VODActivity.createVODIntent(vod, this, false);
        } else if (paramSize == 1) { // twitch.tv/<channel>
            JSONObject jsonObject = new JSONObject(Service.urlToJSONString("https://api.twitch.tv/kraken/users?login=" + params.get(0)));
            JSONArray users = jsonObject.getJSONArray("users");
            if (users.length() == 0) {
                errorMessage = R.string.router_nonexistent_user;
                return null;
            }

            errorMessage = R.string.router_channel_error;

            String userID = users.getJSONObject(0).getString("_id");
            jsonObject = new JSONObject(Service.urlToJSONString("https://api.twitch.tv/kraken/streams/" + userID));
            JSONObject streamObject = jsonObject.isNull("stream") ? null : jsonObject.getJSONObject("stream");

            if (streamObject != null) {
                StreamInfo stream = JSONService.getStreamInfo(this, streamObject, null, false);
                intent = LiveStreamActivity.createLiveStreamIntent(stream, false, this);
            } else {
                // If we can't load the stream, try to show the user's channel instead.
                ChannelInfo info = Service.getStreamerInfoFromUserId(Integer.parseInt(userID));
                if (info != null) {
                    intent = new Intent(this, ChannelActivity.class);
                    intent.putExtra(getResources().getString(R.string.channel_info_intent_object), info);
                }
            }
        }

        return intent;
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
