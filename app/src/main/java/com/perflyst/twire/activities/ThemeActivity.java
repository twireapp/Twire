package com.perflyst.twire.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.main.MainActivity;
import com.perflyst.twire.service.Settings;

/**
 * Created by Sebastian Rask on 30-04-2016.
 */
public class ThemeActivity extends AppCompatActivity {
    private String theme;

    @Override
    protected void onCreate(Bundle savedInstance) {
        loadTheme();
        super.onCreate(savedInstance);
    }

    @Override
    public void onResume() {
        super.onResume();

        String currentTheme = new Settings(this).getTheme();
        if (!currentTheme.equals(theme)) {
            recreate();
        }
    }

    private void loadTheme() {
        int themeRes = R.style.BlueTheme;
        theme = new Settings(this).getTheme();
        if (theme.equals(getString(R.string.purple_theme_name))) {
            themeRes = R.style.PurpleTheme;
        } else if (theme.equals(getString(R.string.black_theme_name))) {
            themeRes = R.style.BlackTheme;
        } else if (theme.equals(getString(R.string.night_theme_name))) {
            themeRes = R.style.NightTheme;
        } else if (theme.equals(getString(R.string.true_night_theme_name))) {
            themeRes = R.style.TrueNightTheme;
        }
        setTheme(themeRes);
    }

    @Override
    public void recreate() {
        if (this instanceof MainActivity) {
            ((MainActivity) this).getRecyclerView().scrollToPosition(0);
        }
        super.recreate();
    }
}
