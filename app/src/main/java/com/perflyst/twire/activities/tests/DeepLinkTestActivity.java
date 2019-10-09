package com.perflyst.twire.activities.tests;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.ChannelActivity;
import com.perflyst.twire.activities.StartUpActivity;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.activities.main.MyGamesActivity;
import com.perflyst.twire.activities.main.MyStreamsActivity;
import com.perflyst.twire.activities.main.TopGamesActivity;
import com.perflyst.twire.activities.main.TopStreamsActivity;
import com.perflyst.twire.service.Settings;

import java.util.List;


public class DeepLinkTestActivity extends ThemeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deep_link_test);

        Settings settings = new Settings(this);
        Uri data = getIntent().getData();
        List<String> params = data.getPathSegments();
        Intent intent = null;
        if (params.isEmpty()) {
            intent = new Intent(getBaseContext(), StartUpActivity.class);
        } else {
            int size = params.size();
            switch (params.get(size - 1)) {
                case "directory":
                    // Open Top Games
                    intent = new Intent(getBaseContext(), TopGamesActivity.class);
                    break;
                case "all":
                    if (params.get(size - 2).equals("videos")) {
                        // Top Broadcasts
                        //ToDo: This is not supported. Fix when it is
                        intent = new Intent(getBaseContext(), StartUpActivity.class);
                    } else if (params.get(0).equals("directory")) {
                        // Top Streams
                        intent = new Intent(getBaseContext(), TopStreamsActivity.class);
                    } else {
                        String streamerName = params.get(0);
                        intent = new Intent(getBaseContext(), ChannelActivity.class);
                    }
                    break;
                case "past-broadcasts":
                case "highlights":
                    if (params.get(0).equals("directory")) {
                        //ToDo: Not supported. Fix when it is
                        intent = new Intent(getBaseContext(), StartUpActivity.class);
                    }
                    break;
                case "following":
                case "live":
                    // My Streams
                    intent = new Intent(
                            getBaseContext(),
                            settings.isLoggedIn()
                                    ? MyStreamsActivity.class
                                    : StartUpActivity.class
                    );
                    break;
                case "games":
                    // My games
                    intent = new Intent(
                            getBaseContext(),
                            settings.isLoggedIn()
                                    ? MyGamesActivity.class
                                    : StartUpActivity.class
                    );
                    break;
            }

            if (intent == null) {
                if (params.size() > 2 && params.get(1).equals("game")) {
                    // Specific game
                    String gameName = params.get(2);
                } else if (!params.isEmpty()) {
                    // Streamer
                    // If live to to live stream. If not go to profile
                    String streamerName = params.get(0);

                } else {
                    intent = new Intent(getBaseContext(), StartUpActivity.class);
                }
            }
        }

        startActivity(intent);
    }
}
