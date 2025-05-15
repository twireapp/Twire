package com.perflyst.twire.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.perflyst.twire.R
import com.perflyst.twire.service.DialogService
import com.perflyst.twire.service.Settings.appearanceChannelSize
import com.perflyst.twire.service.Settings.appearanceChannelStyle
import com.perflyst.twire.service.Settings.appearanceGameSize
import com.perflyst.twire.service.Settings.appearanceGameStyle
import com.perflyst.twire.service.Settings.appearanceStreamSize
import com.perflyst.twire.service.Settings.appearanceStreamStyle
import com.perflyst.twire.service.Settings.theme


class AppearanceSettingsFragment : Fragment() {
    private lateinit var themeSummary: TextView
    private lateinit var streamsStyleSummary: TextView
    private lateinit var gameStyleSummary: TextView
    private lateinit var followStyleSummary: TextView
    private lateinit var streamSizeSummary: TextView
    private lateinit var gameSizeSummary: TextView
    private lateinit var streamerSizeSummary: TextView
    private lateinit var themeSummaryColor: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_appearance_settings, container, false)
        themeSummary = rootView.findViewById(R.id.appearance_theme_color_summary)
        themeSummaryColor = rootView.findViewById(R.id.appearance_theme_color)

        streamsStyleSummary = rootView.findViewById(R.id.appearance_streams_style_summary)
        gameStyleSummary = rootView.findViewById(R.id.appearance_game_style_summary)
        followStyleSummary = rootView.findViewById(R.id.appearance_streamer_style_summary)

        streamSizeSummary = rootView.findViewById(R.id.appearance_streams_size_summary)
        gameSizeSummary = rootView.findViewById(R.id.appearance_game_size_summary)
        streamerSizeSummary = rootView.findViewById(R.id.appearance_streamer_size_summary)

        initSummaries()
        initOnClicks(rootView)

        return rootView
    }

    private fun initOnClicks(rootView: View) {
        val themeColorWrapper = rootView.findViewById<View>(R.id.appearance_theme_color_wrapper)
        themeColorWrapper.setOnClickListener { v: View? -> onClickThemeColor() }

        val streamStyleWrapper = rootView.findViewById<View>(R.id.appearance_streams_style_wrapper)
        streamStyleWrapper.setOnClickListener { view: View? -> onClickStreamStyle() }

        val streamSizeWrapper = rootView.findViewById<View>(R.id.appearance_streams_size_wrapper)
        streamSizeWrapper.setOnClickListener { v: View? -> onClickStreamSize() }

        val gameStyleWrapper = rootView.findViewById<View>(R.id.appearance_game_style_wrapper)
        gameStyleWrapper.setOnClickListener { view: View? -> onClickGameStyle() }

        val gameSizeWrapper = rootView.findViewById<View>(R.id.appearance_game_size_wrapper)
        gameSizeWrapper.setOnClickListener { v1: View? -> onClickGameSize() }

        val streamerStyleWrapper =
            rootView.findViewById<View>(R.id.appearance_streamer_style_wrapper)
        streamerStyleWrapper.setOnClickListener { view: View? -> onClickStreamerStyle() }

        val streamerSizeWrapper = rootView.findViewById<View>(R.id.appearance_streamer_size_wrapper)
        streamerSizeWrapper.setOnClickListener { v: View? -> onClickStreamerSize() }
    }

    private fun initSummaries() {
        // Theme Summary
        themeSummary.setText(theme.nameRes)
        themeSummaryColor.setImageDrawable(
            AppCompatResources.getDrawable(
                requireContext(),
                theme.chooser
            )
        )

        // Style Summary
        streamsStyleSummary.text = appearanceStreamStyle
        gameStyleSummary.text = appearanceGameStyle
        followStyleSummary.text = appearanceChannelStyle

        // Size Summary
        streamSizeSummary.text = appearanceStreamSize
        gameSizeSummary.text = appearanceGameSize
        streamerSizeSummary.text = appearanceChannelSize
    }

    private fun onClickThemeColor() {
        val themeChooserDialog = DialogService.getThemeDialog(requireActivity())
        themeChooserDialog.show()
    }

    private fun onClickStreamStyle() {
        DialogService.getChooseStreamCardStyleDialog(
            requireActivity()
        ) { title: String?, index: Int, previewView: View? ->
            val sharedPadding = previewView!!.findViewById<View>(R.id.shared_padding)
            val view1 = previewView.findViewById<ImageView>(R.id.image_stream_preview)
            view1.setImageResource(R.drawable.preview_stream)
            val streamTitle = previewView.findViewById<View>(R.id.stream_title)
            val viewersAndGame = previewView.findViewById<View>(R.id.stream_game_and_viewers)

            when (title) {
                getString(R.string.card_style_expanded) -> {
                    streamTitle.visibility = View.VISIBLE
                    viewersAndGame.visibility = View.VISIBLE
                    sharedPadding.visibility = View.VISIBLE
                }

                getString(R.string.card_style_normal) -> {
                    streamTitle.visibility = View.GONE
                    viewersAndGame.visibility = View.VISIBLE
                    sharedPadding.visibility = View.VISIBLE
                }

                getString(R.string.card_style_minimal) -> {
                    streamTitle.visibility = View.GONE
                    viewersAndGame.visibility = View.GONE
                    sharedPadding.visibility = View.GONE
                }
            }

            appearanceStreamStyle = title!!
            initSummaries()
        }.show()
    }

    private fun onClickGameStyle() {
        DialogService.getChooseGameCardStyleDialog(
            requireActivity()
        ) { title: String?, index: Int, previewView: View? ->
            val sharedPadding = previewView!!.findViewById<View>(R.id.shared_padding)
            val view1 = previewView.findViewById<ImageView>(R.id.image_game_preview)
            view1.setImageResource(R.drawable.preview_game)

            val lp = FrameLayout.LayoutParams(
                requireContext().resources.getDimension(R.dimen.game_preview_width)
                    .toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            previewView.setLayoutParams(lp)

            val gameTitle = previewView.findViewById<View>(R.id.game_card_title)

            /*
        TextView gameViewers = previewView.findViewById(R.id.game_viewers);
        if (gameViewers.getText().equals("")) {
            gameViewers.setText(R.string.preview_game_viewers);
        }
         */
            when (title) {
                getString(R.string.card_style_expanded) -> {
                    gameTitle.visibility = View.VISIBLE
                    //gameViewers.setVisibility(View.VISIBLE);
                    sharedPadding.visibility = View.VISIBLE
                }

                getString(R.string.card_style_normal) -> {
                    gameTitle.visibility = View.GONE
                    //gameViewers.setVisibility(View.VISIBLE);
                    sharedPadding.visibility = View.VISIBLE
                }

                getString(R.string.card_style_minimal) -> {
                    gameTitle.visibility = View.GONE
                    //gameViewers.setVisibility(View.GONE);
                    sharedPadding.visibility = View.GONE
                }
            }

            appearanceGameStyle = title!!
            initSummaries()
        }.show()
    }

    private fun onClickStreamerStyle() {
        DialogService.getChooseStreamerCardStyleDialog(
            requireActivity()
        ) { title: String?, index: Int, previewView: View? ->
            val nameView = previewView!!.findViewById<View>(R.id.displayName)
            val streamerLogo = previewView.findViewById<ImageView>(R.id.profileLogoImageView)
            streamerLogo.setImageResource(R.drawable.preview_streamer)

            val lp = FrameLayout.LayoutParams(
                requireContext().resources
                    .getDimension(R.dimen.subscription_card_preview_width).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            previewView.setLayoutParams(lp)

            if (title == getString(R.string.card_style_normal)) {
                nameView.visibility = View.VISIBLE
            } else if (title == getString(R.string.card_style_minimal)) {
                nameView.visibility = View.GONE
            }

            appearanceChannelStyle = title!!
            initSummaries()
        }.show()
    }

    private fun onClickStreamSize() {
        DialogService.getChooseCardSizeDialog(
            requireActivity(),
            R.string.appearance_streams_size_title,
            appearanceStreamSize
        ) { dialog: MaterialDialog?, itemView: View?, which: Int, text: CharSequence? ->
            appearanceStreamSize = text.toString()
            initSummaries()
            true
        }.show()
    }

    private fun onClickStreamerSize() {
        DialogService.getChooseCardSizeDialog(
            requireActivity(),
            R.string.appearance_streamer_size_title,
            appearanceChannelSize
        ) { dialog: MaterialDialog?, itemView: View?, which: Int, text: CharSequence? ->
            appearanceChannelSize = text.toString()
            initSummaries()
            true
        }.show()
    }

    private fun onClickGameSize() {
        DialogService.getChooseCardSizeDialog(
            requireActivity(),
            R.string.appearance_game_size_title,
            appearanceGameSize
        ) { dialog: MaterialDialog?, itemView: View?, which: Int, text: CharSequence? ->
            appearanceGameSize = text.toString()
            initSummaries()
            true
        }.show()
    }

    companion object {
        fun newInstance(): AppearanceSettingsFragment {
            return AppearanceSettingsFragment()
        }
    }
}
