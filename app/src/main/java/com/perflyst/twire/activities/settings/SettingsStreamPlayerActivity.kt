package com.perflyst.twire.activities.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.CheckedTextView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.afollestad.materialdialogs.MaterialDialog
import com.perflyst.twire.R
import com.perflyst.twire.activities.ThemeActivity
import com.perflyst.twire.databinding.ActivitySettingsStreamPlayerBinding
import com.perflyst.twire.service.DialogService
import com.perflyst.twire.service.Settings.streamPlayerAutoContinuePlaybackOnReturn
import com.perflyst.twire.service.Settings.streamPlayerLockedPlayback
import com.perflyst.twire.service.Settings.streamPlayerProxy
import com.perflyst.twire.service.Settings.streamPlayerRuntime
import com.perflyst.twire.service.Settings.streamPlayerShowNavigationBar
import com.perflyst.twire.service.Settings.streamPlayerShowViewerCount
import com.perflyst.twire.service.Settings.streamPlayerType

class SettingsStreamPlayerActivity : ThemeActivity() {
    private lateinit var mShowViewCountSummary: TextView
    private lateinit var mShowNavigationBarSummary: TextView
    private lateinit var mAutoPlaybackSummary: TextView
    private lateinit var mLockedPlaybackSummary: TextView
    private lateinit var mShowRuntimeSummary: TextView
    private lateinit var mPlayerTypeSummary: TextView
    private lateinit var mPlayerProxySummary: TextView
    private lateinit var mShowViewCountView: CheckedTextView
    private lateinit var mShowNavigationBarView: CheckedTextView
    private lateinit var mAutoPlaybackView: CheckedTextView
    private lateinit var mLockedPlaybackView: CheckedTextView
    private lateinit var mShowRuntimeView: CheckedTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsStreamPlayerBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        mShowNavigationBarView = findViewById(R.id.player_show_navigation_title)
        mShowViewCountView = findViewById(R.id.player_show_viewercount_title)
        mShowRuntimeView = findViewById(R.id.player_show_runtime)
        mAutoPlaybackView = findViewById(R.id.player_auto_continue_playback_title)
        mLockedPlaybackView = findViewById(R.id.player_locked_playback_title)
        mPlayerTypeSummary = findViewById(R.id.player_type_summary)
        mPlayerProxySummary = findViewById(R.id.player_proxy_summary)

        mShowViewCountSummary = findViewById(R.id.player_show_viewercount_title_summary)
        mShowRuntimeSummary = findViewById(R.id.player_show_runtime_summary)
        mShowNavigationBarSummary = findViewById(R.id.player_show_navigation_summary)
        mAutoPlaybackSummary = findViewById(R.id.player_auto_continue_playback_summary)
        mLockedPlaybackSummary = findViewById(R.id.player_locked_playback_summary)

        val toolbar = findViewById<Toolbar?>(R.id.settings_player_toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = getString(R.string.settings_stream_player_name)
        }

        updateSummaries()

        binding.showViewercountButton.setOnClickListener { v: View? ->
            this.onClickShowViewerCount()
        }
        binding.showRuntimeButton.setOnClickListener { v: View? ->
            this.onClickShowRuntime()
        }
        binding.showNavigationButton.setOnClickListener { v: View? ->
            this.onClickShowNavigationBar()
        }
        binding.autoPlaybackButton.setOnClickListener { v: View? ->
            this.onClickAutoPlayback()
        }
        binding.lockedPlaybackButton.setOnClickListener { v: View? ->
            this.onClickLockedPlayback()
        }
        binding.playerTypeButton.setOnClickListener { view: View? ->
            this.onClickPlayerType(
            )
        }
        binding.playerProxyButton.setOnClickListener { view: View? ->
            this.onClickPlayerProxy(
            )
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

    private fun updateSummary(checkView: CheckedTextView, summary: TextView, isEnabled: Boolean) {
        checkView.isChecked = isEnabled
        summary.setText(if (isEnabled) R.string.enabled else R.string.disabled)
    }

    private fun updateSummaries() {
        val types = getResources().getStringArray(R.array.PlayerType)
        mPlayerTypeSummary.text = types[streamPlayerType]
        mPlayerProxySummary.text = convertProxyOption(streamPlayerProxy)
        updateSummary(mShowViewCountView, mShowViewCountSummary, streamPlayerShowViewerCount)
        updateSummary(mShowRuntimeView, mShowRuntimeSummary, streamPlayerRuntime)
        updateSummary(
            mShowNavigationBarView,
            mShowNavigationBarSummary,
            streamPlayerShowNavigationBar
        )
        updateSummary(
            mAutoPlaybackView,
            mAutoPlaybackSummary,
            streamPlayerAutoContinuePlaybackOnReturn
        )
        updateSummary(mLockedPlaybackView, mLockedPlaybackSummary, streamPlayerLockedPlayback)
    }

    fun onClickShowNavigationBar() {
        streamPlayerShowNavigationBar = !streamPlayerShowNavigationBar
        updateSummaries()
    }

    fun onClickShowViewerCount() {
        streamPlayerShowViewerCount = !streamPlayerShowViewerCount
        updateSummaries()
    }

    fun onClickShowRuntime() {
        streamPlayerRuntime = !streamPlayerRuntime
        updateSummaries()
    }

    fun onClickAutoPlayback() {
        streamPlayerAutoContinuePlaybackOnReturn = !streamPlayerAutoContinuePlaybackOnReturn
        updateSummaries()
    }

    fun onClickLockedPlayback() {
        streamPlayerLockedPlayback = !streamPlayerLockedPlayback
        updateSummaries()
    }

    fun onClickPlayerType() {
        val dialog = DialogService.getChoosePlayerTypeDialog(
            this,
            R.string.player_type,
            R.array.PlayerType,
            streamPlayerType
        ) { dialog1: MaterialDialog?, itemView: View?, which: Int, text: CharSequence? ->
            streamPlayerType = which
            updateSummaries()
            true
        }
        dialog.show()
    }

    private fun convertProxyOption(option: String): String {
        return if (option == "custom") getString(R.string.player_proxy_custom)
        else option.ifEmpty { getString(R.string.disabled) }
    }

    fun onClickPlayerProxy() {
        val proxies = listOf(*getResources().getStringArray(R.array.PlayerProxies))
        var selectedIndex = proxies.indexOf(streamPlayerProxy)
        // Since the custom proxy is not in the presets, we need to select the custom option
        if (selectedIndex == -1) {
            selectedIndex = proxies.size - 1
        }

        DialogService.getBaseThemedDialog(this)
            .title(R.string.player_proxy)
            .items(proxies.map(this::convertProxyOption))
            .itemsCallbackSingleChoice(
                selectedIndex
            ) { dialog: MaterialDialog?, itemView: View?, which: Int, text: CharSequence? ->
                if (which == proxies.size - 1) {
                    DialogService.getBaseThemedDialog(this)
                        .title(R.string.player_proxy_custom)
                        .input(
                            "https://example.com",
                            streamPlayerProxy,
                            false
                        ) { dialog1: MaterialDialog?, input: CharSequence? ->
                            streamPlayerProxy = input.toString()
                            updateSummaries()
                        }
                        .show()
                } else {
                    streamPlayerProxy = proxies[which]!!
                    updateSummaries()
                }
                true
            }
            .show()
    }
}
