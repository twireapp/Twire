package com.sebastianrask.bettersubscription.activities.tests;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;

import java.util.List;

import com.sebastianrask.bettersubscription.R;

public class TestPreferenceActivity extends android.preference.PreferenceActivity {

	private AppCompatDelegate mDelegate;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getDelegate().installViewFactory();
		getDelegate().onCreate(savedInstanceState);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preference_categories);
		setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
		addPreferencesFromResource(R.xml.preference_test);

	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		super.onBuildHeaders(target);
//		loadHeadersFromResource(R.xml.preference_test, target);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		getDelegate().onPostCreate(savedInstanceState);
	}

	@Override
	public void setContentView(@LayoutRes int layoutResID) {
		getDelegate().setContentView(layoutResID);
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		getDelegate().onPostResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
		getDelegate().onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getDelegate().onDestroy();
	}

	private void setSupportActionBar(@Nullable Toolbar toolbar) {
		getDelegate().setSupportActionBar(toolbar);
	}

	private AppCompatDelegate getDelegate() {
		if (mDelegate == null) {
			mDelegate = AppCompatDelegate.create(this, null);
		}
		return mDelegate;
	}
}
