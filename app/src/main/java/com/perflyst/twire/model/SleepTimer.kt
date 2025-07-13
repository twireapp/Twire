package com.perflyst.twire.model

import android.app.Activity
import android.content.Context
import android.os.Handler
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.github.stephenvinouze.materialnumberpickercore.MaterialNumberPicker
import com.perflyst.twire.R
import com.perflyst.twire.service.DialogService
import com.perflyst.twire.service.Settings.streamSleepTimerHour
import com.perflyst.twire.service.Settings.streamSleepTimerMinute
import timber.log.Timber

/**
 * Created by Sebastian Rask Jepsen on 22/07/16.
 */
class SleepTimer(private val delegate: SleepTimerDelegate, private val context: Context) {
    private val sleepTimerHandler: Handler
    private val sleepTimerRunnable: Runnable
    private var sleepTimerProgressMinutes: Int
    private var isRunning = false

    init {
        sleepTimerProgressMinutes = Int.Companion.MIN_VALUE
        sleepTimerHandler = Handler()
        sleepTimerRunnable = object : Runnable {
            override fun run() {
                try {
                    if (sleepTimerProgressMinutes == 0) {
                        isRunning = false
                        delegate.onTimesUp()
                    } else {
                        sleepTimerProgressMinutes--
                        sleepTimerHandler.postDelayed(this, (1000 * 60).toLong())
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Sleep Timer runnable failed")
                }
            }
        }
    }

    fun show(activity: Activity) {
        var hourToShow = streamSleepTimerHour
        var minuteToShow = streamSleepTimerMinute

        if (isRunning && sleepTimerProgressMinutes > 0) {
            hourToShow = sleepTimerProgressMinutes / 60
            minuteToShow = sleepTimerProgressMinutes % 60
        }

        DialogService.getSleepTimerDialog(
            activity,
            isRunning,
            { dialog: MaterialDialog?, which: DialogAction? ->
                val customView = dialog!!.customView
                if (customView == null) return@getSleepTimerDialog
                val hourPicker = customView.findViewById<MaterialNumberPicker>(R.id.hourPicker)
                val minPicker = customView.findViewById<MaterialNumberPicker>(R.id.minutePicker)

                val hour = hourPicker.value
                val minute = minPicker.value
                if (isRunning) {
                    sleepTimerProgressMinutes = hour * 60 + minute
                } else {
                    start(hour, minute)
                }
            },
            { dialog: MaterialDialog?, which: DialogAction? ->
                if (isRunning) {
                    stop()
                }
            },
            hourToShow,
            minuteToShow
        )
            .show()
    }

    private fun start(hour: Int, minute: Int) {
        isRunning = true
        sleepTimerProgressMinutes = hour * 60 + minute
        sleepTimerHandler.removeCallbacks(sleepTimerRunnable)
        sleepTimerHandler.postDelayed(sleepTimerRunnable, 0)
        streamSleepTimerHour = hour
        streamSleepTimerMinute = minute
        if (hour > 0) {
            delegate.onStart(context.getString(R.string.stream_sleep_timer_started, hour, minute))
        } else {
            delegate.onStart(
                context.getString(
                    R.string.stream_sleep_timer_started_minutes_only,
                    minute
                )
            )
        }
    }

    private fun stop() {
        isRunning = false
        sleepTimerHandler.removeCallbacks(sleepTimerRunnable)
        delegate.onStop(context.getString(R.string.stream_sleep_timer_stopped))
    }

    interface SleepTimerDelegate {
        fun onTimesUp()

        fun onStart(message: String)

        fun onStop(message: String)
    }
}
