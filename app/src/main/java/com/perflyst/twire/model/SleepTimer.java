package com.perflyst.twire.model;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;

import com.github.stephenvinouze.materialnumberpickercore.MaterialNumberPicker;
import com.perflyst.twire.R;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.Settings;

import timber.log.Timber;

/**
 * Created by Sebastian Rask Jepsen on 22/07/16.
 */
public class SleepTimer {
    private final Handler sleepTimerHandler;
    private final Runnable sleepTimerRunnable;
    private final SleepTimerDelegate delegate;
    private final Context context;
    private int sleepTimerProgressMinutes;
    private boolean isRunning;

    public SleepTimer(final SleepTimerDelegate delegate, Context context) {
        this.context = context;
        this.delegate = delegate;
        sleepTimerProgressMinutes = Integer.MIN_VALUE;
        sleepTimerHandler = new Handler();
        sleepTimerRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (sleepTimerProgressMinutes == 0) {
                        isRunning = false;
                        delegate.onTimesUp();
                    } else {
                        sleepTimerProgressMinutes--;
                        sleepTimerHandler.postDelayed(this, 1000 * 60);
                    }
                } catch (Exception e) {
                    Timber.e(e);
                    Timber.e("Sleep Timer runnable failed");
                }
            }
        };
    }

    public void show(Activity activity) {
        int hourToShow = Settings.getStreamSleepTimerHour();
        int minuteToShow = Settings.getStreamSleepTimerMinute();

        if (isRunning && sleepTimerProgressMinutes > 0) {
            hourToShow = sleepTimerProgressMinutes / 60;
            minuteToShow = sleepTimerProgressMinutes % 60;
        }

        DialogService.getSleepTimerDialog(activity, isRunning, (dialog, which) -> {
                            View customView = dialog.getCustomView();
                            if (customView == null) return;
                            MaterialNumberPicker hourPicker = customView.findViewById(R.id.hourPicker);
                            MaterialNumberPicker minPicker = customView.findViewById(R.id.minutePicker);

                            int hour = hourPicker.getValue(), minute = minPicker.getValue();

                            if (isRunning) {
                                sleepTimerProgressMinutes = hour * 60 + minute;
                            } else {
                                start(hour, minute);
                            }
                        },
                        (dialog, which) -> {
                            if (isRunning) {
                                stop();
                            }
                        },
                        hourToShow,
                        minuteToShow)
                .show();
    }

    private void start(int hour, int minute) {
        isRunning = true;
        sleepTimerProgressMinutes = hour * 60 + minute;
        sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        sleepTimerHandler.postDelayed(sleepTimerRunnable, 0);
        Settings.setStreamSleepTimerHour(hour);
        Settings.setStreamSleepTimerMinute(minute);
        if (hour > 0) {
            delegate.onStart(context.getString(R.string.stream_sleep_timer_started, hour, minute));
        } else {
            delegate.onStart(context.getString(R.string.stream_sleep_timer_started_minutes_only, minute));
        }
    }

    private void stop() {
        isRunning = false;
        sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        delegate.onStop(context.getString(R.string.stream_sleep_timer_stopped));
    }

    public interface SleepTimerDelegate {
        void onTimesUp();

        void onStart(String message);

        void onStop(String message);
    }
}
