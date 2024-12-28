package com.perflyst.twire.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.perflyst.twire.activities.main.MainActivity;
import com.perflyst.twire.model.Theme;
import com.perflyst.twire.service.Settings;

/**
 * Created by Sebastian Rask on 30-04-2016.
 */
public class ThemeActivity extends AppCompatActivity {
    private Theme theme;

    @Override
    protected void onCreate(Bundle savedInstance) {
        loadTheme();
        super.onCreate(savedInstance);
    }

    @Override
    public void onResume() {
        super.onResume();

        Theme currentTheme = new Settings(this).getTheme();
        if (!currentTheme.equals(theme)) {
            recreate();
        }
    }

    private void loadTheme() {
        this.theme = new Settings(this).getTheme();
        setTheme(theme.style);
    }

    @Override
    public void recreate() {
        if (this instanceof MainActivity) {
            ((MainActivity) this).getRecyclerView().scrollToPosition(0);
        }
        super.recreate();
    }
}
