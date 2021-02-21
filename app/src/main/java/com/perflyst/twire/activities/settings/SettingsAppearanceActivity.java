package com.perflyst.twire.activities.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.fragments.AppearanceSettingsFragment;

public class SettingsAppearanceActivity extends ThemeActivity {
    AppearanceSettingsFragment mSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_appearance);
        if (savedInstanceState == null) {
            FragmentManager fm = getSupportFragmentManager();
            mSettingsFragment = (AppearanceSettingsFragment) fm.findFragmentById(R.id.appearance_fragment);

            if (mSettingsFragment == null) {
                mSettingsFragment = AppearanceSettingsFragment.newInstance();
            }
        }

        Toolbar mToolbar = findViewById(R.id.settings_appearance_toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_right_anim);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }
}
