package com.perflyst.twire.activities.settings

import android.os.Bundle
import android.util.Patterns
import android.view.MenuItem
import android.view.View
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import com.perflyst.twire.R
import com.perflyst.twire.activities.setup.LoginFragment
import com.perflyst.twire.databinding.ActivitySettingsGeneralBinding
import com.perflyst.twire.fragments.BindingFragment
import com.perflyst.twire.fragments.ChangelogDialogFragment
import com.perflyst.twire.misc.navigate
import com.perflyst.twire.misc.popBackStack
import com.perflyst.twire.misc.setupToolbar
import com.perflyst.twire.service.DialogService
import com.perflyst.twire.service.ReportErrors
import com.perflyst.twire.service.Settings.generalFilterTopStreamsByLanguage
import com.perflyst.twire.service.Settings.generalTwitchDisplayName
import com.perflyst.twire.service.Settings.generalUseImageProxy
import com.perflyst.twire.service.Settings.imageProxyUrl
import com.perflyst.twire.service.Settings.isLoggedIn
import com.perflyst.twire.service.Settings.isTipsShown
import com.perflyst.twire.service.Settings.reportErrors
import com.perflyst.twire.service.Settings.startPage
import com.perflyst.twire.service.SubscriptionsDbHelper
import timber.log.Timber

class SettingsGeneralFragment :
    BindingFragment<ActivitySettingsGeneralBinding>(ActivitySettingsGeneralBinding::inflate) {
    private lateinit var twitchNameView: TextView
    private lateinit var startPageSubText: TextView
    private lateinit var generalImageProxySummary: TextView
    private lateinit var errorReportSubText: TextView
    private lateinit var filterTopStreamsByLanguageView: CheckedTextView
    private lateinit var generalImageProxy: CheckedTextView
    private lateinit var mImageProxyUrl: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(binding.settingsGeneralToolbar, R.string.settings_general_name)

        twitchNameView = view.findViewById(R.id.general_current_twitch_name)
        startPageSubText = view.findViewById(R.id.start_page_sub_text)
        filterTopStreamsByLanguageView = view.findViewById(R.id.language_filter_title)

        generalImageProxySummary = view.findViewById(R.id.general_image_proxy_summary)
        generalImageProxy = view.findViewById(R.id.general_image_proxy)
        mImageProxyUrl = view.findViewById(R.id.image_proxy_url_input)
        errorReportSubText = binding.errorReportSubText

        updateSummaries()

        initTwitchDisplayName()
        initStartPageText()
        initFilterTopsStreamsByLanguage()

        binding.twitchNameButton.setOnClickListener { v: View? ->
            this.onClickTwitchName()
        }
        binding.startPageButton.setOnClickListener { v: View? ->
            this.onClickStartPage()
        }
        binding.resetTipsButton.setOnClickListener { v: View? ->
            this.onClickResetTips()
        }
        binding.languageFilterButton.setOnClickListener { v: View? ->
            this.onClickFiltersStreamsByLanguageEnable()
        }
        binding.changelogButton.setOnClickListener { v: View? ->
            this.onClickOpenChangelog()
        }
        binding.imageProxyButton.setOnClickListener { v: View? ->
            this.onClickImageProxy()
        }
        binding.imageProxyUrlButton.setOnClickListener { v: View? ->
            this.onClickImageProxyUrl()
        }
        binding.wipeFollowsButton.setOnClickListener { v: View? ->
            this.onClickWipeFollows()
        }
        binding.exportFollowsButton.setOnClickListener { v: View? ->
            this.onExport()
        }
        binding.importFollowsButton.setOnClickListener { v: View? ->
            this.onImport()
        }
        binding.errorReportButton.setOnClickListener { view: View? ->
            DialogService.getChooseDialog(
                requireActivity(),
                (R.string.report_error_title),
                R.array.ErrorReportOptions,
                reportErrors.ordinal
            ) { dialog: MaterialDialog?, view: View?, which: Int, text: CharSequence? ->
                reportErrors = ReportErrors.entries[which]
                updateSummaries()
                false
            }.show()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        popBackStack()
        return super.onOptionsItemSelected(item)
    }

    private fun updateSummary(checkView: CheckedTextView, summary: TextView, isEnabled: Boolean) {
        checkView.isChecked = isEnabled
        summary.setText(if (isEnabled) R.string.enabled else R.string.disabled)
    }

    private fun updateSummaries() {
        updateSummary(generalImageProxy, generalImageProxySummary, generalUseImageProxy)
        mImageProxyUrl.setText(imageProxyUrl)
        errorReportSubText.setText(reportErrors.stringRes)
    }

    private fun initStartPageText() {
        startPageSubText.text = startPage
    }

    private fun initTwitchDisplayName() {
        if (isLoggedIn) {
            twitchNameView.text = generalTwitchDisplayName
        } else {
            twitchNameView.setText(R.string.gen_not_logged_in)
        }
    }

    private fun initFilterTopsStreamsByLanguage() {
        filterTopStreamsByLanguageView.isChecked = generalFilterTopStreamsByLanguage
    }

    fun onClickTwitchName() {
        if (isLoggedIn) {
            val dialog =
                DialogService.getSettingsLoginOrLogoutDialog(
                    requireActivity(),
                    generalTwitchDisplayName
                )
            dialog.builder
                .onPositive { dialog1: MaterialDialog?, which: DialogAction? -> navigateToLogin() }

            dialog.builder
                .onNegative { dialog12: MaterialDialog?, which: DialogAction? ->
                    isLoggedIn = false
                    initTwitchDisplayName()
                }

            dialog.show()
        } else {
            navigateToLogin()
        }
    }

    fun onClickStartPage() {
        val dialog = DialogService.getChooseStartUpPageDialog(
            requireActivity(),
            startPageSubText.getText().toString()
        ) { dialog1: MaterialDialog?, view: View?, which: Int, text: CharSequence? ->
            startPage = text.toString()
            startPageSubText.text = text.toString()
            true
        }

        dialog.show()
    }

    private fun navigateToLogin() {
        navigate(
            LoginFragment::class.java, bundleOf(
                getString(R.string.login_intent_part_of_setup) to false
            )
        )
    }

    fun onClickResetTips() {
        if (isTipsShown) {
            val topView = requireView().findViewById<View?>(R.id.container_settings_general)
            if (topView != null) {
                Snackbar.make(
                    topView,
                    getString(R.string.gen_tips_have_been_reset),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
        isTipsShown = false
    }

    fun onClickFiltersStreamsByLanguageEnable() {
        generalFilterTopStreamsByLanguage = !generalFilterTopStreamsByLanguage
        initFilterTopsStreamsByLanguage()
    }

    fun onClickOpenChangelog() {
        ChangelogDialogFragment().show(parentFragmentManager, "ChangelogDialog")
    }

    // Database Stuff below
    fun onClickWipeFollows() {
        val dialog = DialogService.getSettingsWipeFollowsDialog(requireActivity())
        dialog.builder
            .onPositive { dialog1: MaterialDialog?, which: DialogAction? ->
                val helper = SubscriptionsDbHelper(requireContext())
                helper.onWipe(helper.writableDatabase, isLoggedIn)
                val infoToast = Toast.makeText(
                    requireContext(),
                    getString(R.string.gen_toast_wipe_database),
                    Toast.LENGTH_SHORT
                )
                infoToast.show()
            }.show()
    }

    // Export/Import for Follows
    fun onExport() {
        val dialog = DialogService.getSettingsExportFollowsDialog(requireActivity())
        dialog.builder
            .onPositive { dialog1: MaterialDialog?, which: DialogAction? ->
                val helper = SubscriptionsDbHelper(requireContext())
                val exported = helper.onExport(helper.writableDatabase)
                val infoToast = Toast.makeText(
                    requireContext(),
                    String.format(getString(R.string.gen_toast_export_database), exported),
                    Toast.LENGTH_SHORT
                )
                infoToast.show()
            }.show()
    }

    fun onImport() {
        val dialog = DialogService.getSettingsImportFollowsDialog(requireActivity())
        dialog.builder
            .onPositive { dialog1: MaterialDialog?, which: DialogAction? ->
                val helper = SubscriptionsDbHelper(requireContext())
                val imported = helper.onImport(helper.writableDatabase)
                val infoToast = Toast.makeText(
                    requireContext(),
                    String.format(getString(R.string.gen_toast_import_database), imported),
                    Toast.LENGTH_SHORT
                )
                infoToast.show()
            }.show()
    }

    fun onClickImageProxy() {
        generalUseImageProxy = !generalUseImageProxy
        updateSummaries()
    }

    fun onClickImageProxyUrl() {
        val proxyUrl = mImageProxyUrl.getText().toString()

        // app/src/main/java/com/perflyst/twire/activities/DeepLinkActivity.java 115
        val matcher = Patterns.WEB_URL.matcher(proxyUrl)
        if (matcher.find()) {
            imageProxyUrl = proxyUrl
            Timber.d("Setting as Image Proxy: %s", proxyUrl)
            updateSummaries()
        } else {
            Timber.d("Url looks wrong%s", proxyUrl)
        }
    }
}
