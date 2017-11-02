package com.sebastianrask.bettersubscription.activities.settings;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.activities.ThemeActivity;
import com.sebastianrask.bettersubscription.fragments.AppearanceSettingsFragment;

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

        Toolbar mToolbar = (Toolbar) findViewById(R.id.settings_appearance_toolbar);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		onBackPressed();
		return super.onOptionsItemSelected(item);
	}
}
