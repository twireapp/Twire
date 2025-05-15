package com.perflyst.twire.activities.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.perflyst.twire.R
import com.perflyst.twire.activities.ThemeActivity
import com.perflyst.twire.fragments.AppearanceSettingsFragment

class SettingsAppearanceActivity : ThemeActivity() {
    var mSettingsFragment: AppearanceSettingsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_appearance)
        if (savedInstanceState == null) {
            val fm = supportFragmentManager
            mSettingsFragment =
                fm.findFragmentById(R.id.appearance_fragment) as AppearanceSettingsFragment?

            if (mSettingsFragment == null) {
                mSettingsFragment = AppearanceSettingsFragment.newInstance()
            }
        }

        val mToolbar = findViewById<Toolbar?>(R.id.settings_appearance_toolbar)
        setSupportActionBar(mToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        this.overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_right_anim)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onBackPressed()
        return super.onOptionsItemSelected(item)
    }
}
