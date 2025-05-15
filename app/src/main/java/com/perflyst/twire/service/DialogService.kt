package com.perflyst.twire.service

import android.app.Activity
import android.content.DialogInterface
import android.view.KeyEvent
import android.view.View
import android.widget.NumberPicker
import android.widget.NumberPicker.OnValueChangeListener
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.ListCallbackSingleChoice
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem
import com.github.stephenvinouze.materialnumberpickercore.MaterialNumberPicker
import com.perflyst.twire.PlaybackService
import com.perflyst.twire.R
import com.perflyst.twire.model.Theme
import com.perflyst.twire.service.Settings.appearanceChannelStyle
import com.perflyst.twire.service.Settings.appearanceGameStyle
import com.perflyst.twire.service.Settings.appearanceStreamStyle
import com.perflyst.twire.service.Settings.playbackSpeed
import com.perflyst.twire.service.Settings.skipSilence
import com.perflyst.twire.service.Settings.theme
import com.perflyst.twire.views.LayoutSelector
import com.perflyst.twire.views.LayoutSelector.OnLayoutSelected
import com.rey.material.widget.CheckedTextView
import com.rey.material.widget.Slider
import com.rey.material.widget.Slider.OnPositionChangeListener
import java.util.Arrays
import kotlin.math.min

/**
 * Created by Sebastian Rask on 02-05-2016.
 */
object DialogService {
    fun getThemeDialog(activity: Activity): MaterialDialog {
        val CURRENT_THEME = theme
        val adapter = MaterialSimpleListAdapter(activity)
        adapter.addAll(
            Arrays.stream(Theme.entries.toTypedArray())
                .map<MaterialSimpleListItem?> { theme: Theme? ->
                    getThemeDialogAdapterItem(
                        theme!!.nameRes, theme.chooser, activity
                    )
                }.toList()
        )

        val dialog = getBaseThemedDialog(activity)
            .title(R.string.theme_dialog_title)
            .adapter(
                adapter
            ) { dialog1: MaterialDialog?, itemView: View?, which: Int, text: CharSequence? ->
                val theme = Theme.entries[which]
                dialog1!!.dismiss()

                Settings.theme = theme
                if (theme != CURRENT_THEME) {
                    activity.recreate()
                }
            }

        return dialog.build()
    }

    private fun getThemeDialogAdapterItem(
        @StringRes title: Int,
        @DrawableRes icon: Int,
        activity: Activity?
    ): MaterialSimpleListItem? {
        val builder = MaterialSimpleListItem.Builder(activity)
            .content(title)
            .icon(icon)

        return builder.build()
    }

    fun getSettingsLoginOrLogoutDialog(activity: Activity, username: String?): MaterialDialog {
        return getBaseThemedDialog(activity)
            .content(activity.getString(R.string.gen_dialog_login_or_out_content, username))
            .positiveText(R.string.gen_dialog_login_or_out_login_action)
            .negativeText(R.string.gen_dialog_login_or_out_logout_action).build()
    }

    fun getSettingsWipeFollowsDialog(activity: Activity): MaterialDialog {
        return getBaseThemedDialog(activity)
            .content(R.string.gen_dialog_wipe_follows_content)
            .positiveText(R.string.gen_dialog_wipe_follows_action)
            .negativeText(R.string.cancel).build()
    }

    fun getSettingsExportFollowsDialog(activity: Activity): MaterialDialog {
        return getBaseThemedDialog(activity)
            .content(R.string.gen_dialog_export_follows_content)
            .positiveText(R.string.gen_dialog_export_follows_action)
            .negativeText(R.string.cancel).build()
    }

    fun getSettingsImportFollowsDialog(activity: Activity): MaterialDialog {
        return getBaseThemedDialog(activity)
            .content(R.string.gen_dialog_import_follows_content)
            .positiveText(R.string.gen_dialog_import_follows_action)
            .negativeText(R.string.cancel).build()
    }

    fun getChooseStartUpPageDialog(
        activity: Activity,
        currentlySelectedPageTitle: String?,
        listCallbackSingleChoice: ListCallbackSingleChoice
    ): MaterialDialog {
        @ArrayRes val arrayResource = R.array.StartupPages

        var indexOfPage = 0
        val androidStrings = activity.resources.getStringArray(arrayResource)
        for (i in androidStrings.indices) {
            if (androidStrings[i] == currentlySelectedPageTitle) {
                indexOfPage = i
                break
            }
        }

        return getBaseThemedDialog(activity)
            .title(R.string.gen_start_page)
            .items(arrayResource)
            .itemsCallbackSingleChoice(indexOfPage, listCallbackSingleChoice)
            .positiveText(android.R.string.ok)
            .negativeText(R.string.cancel)
            .build()
    }

    fun getChooseStreamCardStyleDialog(
        activity: Activity,
        onLayoutSelected: OnLayoutSelected
    ): MaterialDialog {
        val layoutSelector = LayoutSelector(
            R.layout.cell_stream,
            R.array.StreamsCardStyles,
            onLayoutSelected,
            activity
        )
            .setSelectedLayoutTitle(appearanceStreamStyle)
            .setTextColorAttr(R.attr.navigationDrawerTextColor)
            .setPreviewMaxHeightRes(R.dimen.stream_preview_max_height)

        return getBaseThemedDialog(activity)
            .title(R.string.appearance_streams_style_title)
            .customView(layoutSelector.build(), true)
            .positiveText(R.string.done)
            .build()
    }

    fun getChooseGameCardStyleDialog(
        activity: Activity,
        onLayoutSelected: OnLayoutSelected
    ): MaterialDialog {
        val layoutSelector =
            LayoutSelector(R.layout.cell_game, R.array.GameCardStyles, onLayoutSelected, activity)
                .setSelectedLayoutTitle(appearanceGameStyle)
                .setTextColorAttr(R.attr.navigationDrawerTextColor)
                .setPreviewMaxHeightRes(R.dimen.game_preview_max_height)

        return getBaseThemedDialog(activity)
            .title(R.string.appearance_game_style_title)
            .customView(layoutSelector.build(), true)
            .positiveText(R.string.done)
            .build()
    }

    fun getChooseStreamerCardStyleDialog(
        activity: Activity,
        onLayoutSelected: OnLayoutSelected
    ): MaterialDialog {
        val layoutSelector = LayoutSelector(
            R.layout.cell_channel,
            R.array.FollowCardStyles,
            onLayoutSelected,
            activity
        )
            .setSelectedLayoutTitle(appearanceChannelStyle)
            .setTextColorAttr(R.attr.navigationDrawerTextColor)
            .setPreviewMaxHeightRes(R.dimen.subscription_card_preview_max_height)

        return getBaseThemedDialog(activity)
            .title(R.string.appearance_streamer_style_title)
            .customView(layoutSelector.build(), true)
            .positiveText(R.string.done)
            .build()
    }

    fun getChooseCardSizeDialog(
        activity: Activity,
        @StringRes dialogTitle: Int,
        currentlySelected: String?,
        callbackSingleChoice: ListCallbackSingleChoice
    ): MaterialDialog {
        var indexOfPage = 0
        val sizeTitles = activity.resources.getStringArray(R.array.CardSizes)
        for (i in sizeTitles.indices) {
            if (sizeTitles[i] == currentlySelected) {
                indexOfPage = i
                break
            }
        }

        return getBaseThemedDialog(activity)
            .title(dialogTitle)
            .itemsCallbackSingleChoice(indexOfPage, callbackSingleChoice)
            .items(*sizeTitles)
            .positiveText(R.string.done)
            .build()
    }

    fun getChooseChatSizeDialog(
        activity: Activity,
        @StringRes dialogTitle: Int,
        @ArrayRes array: Int,
        currentSize: Int,
        callbackSingleChoice: ListCallbackSingleChoice
    ): MaterialDialog {
        val indexOfPage = currentSize - 1
        val sizeTitles = activity.resources.getStringArray(array)

        return getBaseThemedDialog(activity)
            .title(dialogTitle)
            .itemsCallbackSingleChoice(indexOfPage, callbackSingleChoice)
            .items(*sizeTitles)
            .positiveText(R.string.done)
            .build()
    }

    fun getChoosePlayerTypeDialog(
        activity: Activity,
        @StringRes dialogTitle: Int,
        @ArrayRes array: Int,
        currentSize: Int,
        callbackSingleChoice: ListCallbackSingleChoice
    ): MaterialDialog {
        val playerTypes = activity.resources.getStringArray(array)

        return getBaseThemedDialog(activity)
            .title(dialogTitle)
            .itemsCallbackSingleChoice(currentSize, callbackSingleChoice)
            .items(*playerTypes)
            .positiveText(R.string.done)
            .build()
    }

    fun getChooseDialog(
        activity: Activity,
        @StringRes dialogTitle: Int,
        @ArrayRes array: Int,
        selectedIndex: Int,
        callbackSingleChoice: ListCallbackSingleChoice
    ): MaterialDialog {
        return getBaseThemedDialog(activity)
            .title(dialogTitle)
            .itemsCallbackSingleChoice(selectedIndex, callbackSingleChoice)
            .items(array)
            .positiveText(R.string.done)
            .build()
    }

    fun getSleepTimerDialog(
        activity: Activity,
        isTimerRunning: Boolean,
        onStartCallback: SingleButtonCallback,
        onStopCallBack: SingleButtonCallback,
        hourValue: Int,
        minuteValue: Int
    ): MaterialDialog {
        @StringRes val positiveText = if (isTimerRunning) R.string.resume else R.string.start
        @StringRes val negativeText = if (isTimerRunning) R.string.stop else R.string.cancel


        val dialog = getBaseThemedDialog(activity)
            .title(R.string.stream_sleep_timer_title)
            .customView(R.layout.dialog_sleep_timer, false)
            .positiveText(positiveText)
            .negativeText(negativeText)
            .onPositive(onStartCallback)
            .onNegative(onStopCallBack)
            .build()

        val customView = dialog.customView
        val hourPicker = customView!!.findViewById<MaterialNumberPicker>(R.id.hourPicker)
        val minPicker = customView.findViewById<MaterialNumberPicker>(R.id.minutePicker)

        hourPicker.value = hourValue
        minPicker.value = minuteValue

        return dialog
    }

    private var newTime: Long = 0

    fun getSeekDialog(activity: Activity, player: Player): MaterialDialog {
        val dialog = getBaseThemedDialog(activity)
            .title(R.string.stream_seek_dialog_title)
            .customView(R.layout.dialog_seek, false)
            .positiveText(R.string.done)
            .negativeText(R.string.cancel)
            .onPositive { dialog1: MaterialDialog?, which: DialogAction? ->
                player.seekTo(
                    newTime
                )
            }
            .build()

        val customView = dialog.customView
        val hourPicker = customView!!.findViewById<MaterialNumberPicker>(R.id.hour_picker)
        val minutePicker = customView.findViewById<MaterialNumberPicker>(R.id.minute_picker)
        val secondPicker = customView.findViewById<MaterialNumberPicker>(R.id.second_picker)

        val maxProgress = (player.duration / 1000).toInt()
        hourPicker.setMaxValue(maxProgress / 3600)
        minutePicker.setMaxValue(min(maxProgress / 60, 59))
        secondPicker.setMaxValue(min(maxProgress, 59))

        val currentProgress = (player.currentPosition / 1000).toInt()
        hourPicker.value = currentProgress / 3600
        minutePicker.value = currentProgress / 60 % 60
        secondPicker.value = currentProgress % 60

        if (maxProgress > 60 * 60) minutePicker.setWrapSelectorWheel(true)
        if (maxProgress > 60) secondPicker.setWrapSelectorWheel(true)

        val updateTime = OnValueChangeListener { a: NumberPicker?, b: Int, c: Int ->
            newTime =
                (hourPicker.value * 3600L + minutePicker.value * 60L + secondPicker.value) * 1000
        }
        hourPicker.setOnValueChangedListener(updateTime)
        linkPickers(minutePicker, hourPicker, updateTime)
        linkPickers(secondPicker, minutePicker, updateTime)

        newTime = player.currentPosition

        return dialog
    }

    private fun linkPickers(
        source: NumberPicker,
        destination: NumberPicker,
        changeListener: OnValueChangeListener
    ) {
        source.setOnValueChangedListener { picker: NumberPicker?, old_value: Int, new_value: Int ->
            // If we overflow the picker, increment the next picker.
            if (old_value == 59 && new_value == 0) scrollPicker(destination, true)
            else if (old_value == 0 && new_value == 59) scrollPicker(destination, false)
            changeListener.onValueChange(picker, old_value, new_value)
        }
    }

    private fun scrollPicker(numberPicker: NumberPicker, increment: Boolean) {
        numberPicker.dispatchKeyEvent(
            KeyEvent(
                KeyEvent.ACTION_DOWN,
                if (increment) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP
            )
        )
    }

    fun getSliderDialog(
        activity: Activity,
        onCancelCallback: SingleButtonCallback,
        sliderChangeListener: OnPositionChangeListener?,
        startValue: Int,
        minValue: Int,
        maxValue: Int,
        title: String
    ): MaterialDialog {
        val dialog = getBaseThemedDialog(activity)
            .title(title)
            .customView(R.layout.dialog_slider, false)
            .positiveText(R.string.done)
            .negativeText(R.string.cancel)
            .onNegative(onCancelCallback)
            .build()

        val customView = dialog.customView
        if (customView != null) {
            val slider = customView.findViewById<Slider>(R.id.slider)
            slider.setValueRange(minValue, maxValue, false)
            slider.setValue(startValue.toFloat(), false)
            slider.setOnPositionChangeListener(sliderChangeListener)
        }

        return dialog
    }

    @OptIn(markerClass = [UnstableApi::class])
    fun getPlaybackDialog(activity: Activity, player: MediaController): MaterialDialog {
        val dialog = getBaseThemedDialog(activity)
            .title(R.string.menu_playback)
            .customView(R.layout.dialog_playback, false)
            .positiveText(R.string.done)
            .build()

        val customView = dialog.customView

        // Speed
        val initialSpeed = playbackSpeed
        val speedValues: Array<Float> = arrayOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

        val speedDisplay = customView!!.findViewById<TextView>(R.id.speed_display)
        val slider = customView.findViewById<Slider>(R.id.speed_slider)
        slider.setValue(listOf(*speedValues).indexOf(initialSpeed).toFloat(), false)
        slider.setValueDescriptionProvider { value: Int ->
            activity.getString(
                R.string.playback_speed,
                speedValues[value]
            )
        }
        slider.setOnPositionChangeListener { view: Slider?, fromUser: Boolean, oldPos: Float, newPos: Float, oldValue: Int, newValue: Int ->
            val newSpeed: Float = speedValues[newValue]
            playbackSpeed = newSpeed
            player.setPlaybackSpeed(newSpeed)
            speedDisplay.text = activity.getString(R.string.playback_speed_display, newSpeed)
        }

        speedDisplay.text = activity.getString(R.string.playback_speed_display, initialSpeed)

        // Skip Silence
        val skipSilenceView = customView.findViewById<CheckedTextView>(R.id.skip_silence)
        skipSilenceView.setChecked(skipSilence)
        skipSilenceView.setOnClickListener { view: View? ->
            val newState = !skipSilence
            skipSilence = newState
            PlaybackService.sendSkipSilenceUpdate(player)
            skipSilenceView.setChecked(newState)
        }

        return dialog
    }

    fun getRouterErrorDialog(activity: Activity, errorMessage: Int): MaterialDialog {
        return getBaseThemedDialog(activity)
            .title(R.string.router_error_dialog_title)
            .content(errorMessage)
            .cancelListener { dialogInterface: DialogInterface? -> activity.finish() }
            .build()
    }

    fun getBaseThemedDialog(activity: Activity): MaterialDialog.Builder {
        return MaterialDialog.Builder(activity)
            .titleColorAttr(R.attr.navigationDrawerTextColor)
            .backgroundColorAttr(R.attr.navigationDrawerBackground)
            .contentColorAttr(R.attr.navigationDrawerTextColor)
            .itemsColorAttr(R.attr.navigationDrawerTextColor)
    }
}
