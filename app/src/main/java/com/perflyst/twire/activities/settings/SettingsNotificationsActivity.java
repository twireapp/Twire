package com.perflyst.twire.activities.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.balysv.materialripple.MaterialRippleLayout;
import com.google.android.material.snackbar.Snackbar;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.rey.material.app.TimePickerDialog;


public class SettingsNotificationsActivity extends ThemeActivity {
    final Settings settings = new Settings(this);
    private String LOG_TAG = getClass().getSimpleName();

    public static String formatTimeOfDay(int hourOrMinute) {
        String result = "" + hourOrMinute;
        if (result.length() == 0)
            result = "0";

        if (result.length() == 1)
            result = "0" + result;

        return result;
    }

    /**
     * Enables or disables all of the textviews in this activity.
     * If it enables it also checks if the quiet hours is enables and disables or enables the pickers accordingly
     * It also changes the color of the view accordingly
     */

    private static void enableNotifications(boolean enable, boolean isQuietHoursEnabled, Activity activity) {
        if (enable) {
            enableVibrate(true, activity);
            enableScreenWake(true, activity);
            enableSoundPlay(true, activity);
            enableBlinkLED(true, activity);
            enableQuietHours(isQuietHoursEnabled, activity);
            enableQuietHoursCheck(true, activity);
        } else {
            enableVibrate(false, activity);
            enableScreenWake(false, activity);
            enableSoundPlay(false, activity);
            enableBlinkLED(false, activity);
            enableQuietHours(false, activity);
            enableQuietHoursCheck(false, activity);
        }
    }

    private static String formatCheckInterval(String interval, Context context) {
        if (!interval.toLowerCase().equals("never") && (interval.equals("10") || interval.equals("15") || interval.equals("30")))
            interval = context.getResources().getString(R.string.notifications_minutes, interval);
        else if (interval.equals("60"))
            interval = context.getResources().getString(R.string.notifications_hours, "1");
        else if (interval.equals("120"))
            interval = context.getResources().getString(R.string.notifications_hours, "2");
        else
            interval = context.getResources().getString(R.string.notifications_never);

        return interval;
    }

    /**
     * Enables or disables the ability to change the start and end time for the Quiet Hours functionality.
     * It also changes the color of the view
     */
    private static void enableQuietHours(boolean boo, Activity ac) {
        final TextView quietHourStartView = ac.findViewById(R.id.notifications_quiet_hours_start);
        final TextView quietHourEndView = ac.findViewById(R.id.notifications_quiet_hours_end);
        final TextView quietHourStartTagView = ac.findViewById(R.id.notifications_quiet_hours_start_tag);
        final TextView quietHourEndTagView = ac.findViewById(R.id.notifications_quiet_hours_end_tag);
        final TextView notificationsDuringQuietHours = ac.findViewById(R.id.notifications_show_in_quiet_hours);

        int color = getEnabledTextColor(ac);

        if (!boo)
            color = getDisabledTextColor(ac);

        quietHourStartView.setTextColor(color);
        quietHourEndView.setTextColor(color);
        quietHourStartTagView.setTextColor(color);
        quietHourEndTagView.setTextColor(color);
        notificationsDuringQuietHours.setTextColor(color);

        MaterialRippleLayout rippleStart = ac.findViewById(R.id.notifications_quiet_hours_start_ripple);
        rippleStart.setEnabled(boo);

        MaterialRippleLayout rippleEnd = ac.findViewById(R.id.notifications_quiet_hours_end_ripple);
        rippleEnd.setEnabled(boo);

        MaterialRippleLayout rippleShowNotificationsInQuietHours = ac.findViewById(R.id.notifications_show_in_quiet_hours_ripple);
        rippleShowNotificationsInQuietHours.setEnabled(boo);
    }

    /**
     * Enables or disables the ability to change the vibrate function.
     * It also changes the color of the view
     */
    private static void enableVibrate(boolean boo, Activity ac) {
        final TextView vibrateView = ac.findViewById(R.id.notifications_vibrate);
        MaterialRippleLayout rippleVibrate = ac.findViewById(R.id.notifications_vibrate_ripple);

        int color = getEnabledTextColor(ac);

        if (!boo)
            color = getDisabledTextColor(ac);


        vibrateView.setTextColor(color);
        rippleVibrate.setEnabled(boo);
    }

    /**
     * Enables or disables the ability to change the screen wake function.
     * It also changes the color of the view
     */
    private static void enableScreenWake(boolean boo, Activity ac) {
        final TextView screenWakeView = ac.findViewById(R.id.notifications_screen_wake);
        MaterialRippleLayout rippleScreenWake = ac.findViewById(R.id.notifications_screen_wake_ripple);

        int color = getEnabledTextColor(ac);

        if (!boo)
            color = getDisabledTextColor(ac);

        screenWakeView.setTextColor(color);
        rippleScreenWake.setEnabled(boo);
    }

    /**
     * Enables or disables the ability to change the play sound function.
     * It also changes the color of the view
     */
    private static void enableSoundPlay(boolean boo, Activity ac) {
        final TextView soundPlayView = ac.findViewById(R.id.notifications_sound);
        MaterialRippleLayout rippleSoundPlay = ac.findViewById(R.id.notifications_sound_ripple);

        int color = getEnabledTextColor(ac);

        if (!boo)
            color = getDisabledTextColor(ac);

        soundPlayView.setTextColor(color);
        rippleSoundPlay.setEnabled(boo);
    }

    /**
     * Enables or disables the ability to change the blink LED function.
     * It also changes the color of the view
     */
    private static void enableBlinkLED(boolean boo, Activity ac) {
        final CheckedTextView soundPlayView = ac.findViewById(R.id.notifications_led);
        MaterialRippleLayout rippleSoundPlay = ac.findViewById(R.id.notifications_led_ripple);

        int color = getEnabledTextColor(ac);

        if (!boo)
            color = getDisabledTextColor(ac);

        soundPlayView.setTextColor(color);
        rippleSoundPlay.setEnabled(boo);
    }

    /**
     * Enables or disables the ability to change the quiet hours function.
     * It also changes the color of the view
     */
    private static void enableQuietHoursCheck(boolean boo, Activity ac) {
        final TextView quietHoursView = ac.findViewById(R.id.notifications_quiet_hours);
        MaterialRippleLayout rippleSoundPlay = ac.findViewById(R.id.notifications_quiet_hours_ripple);

        int color = getEnabledTextColor(ac);

        if (!boo)
            color = getDisabledTextColor(ac);

        quietHoursView.setTextColor(color);
        rippleSoundPlay.setEnabled(boo);
    }

    public static int getDisabledTextColor(Activity activity) {
        return Service.getColorAttribute(R.attr.settingsDisabledTextColor, R.color.black_text_disabled, activity);
    }

    public static int getEnabledTextColor(Activity activity) {
        return Service.getColorAttribute(R.attr.settingsTextColor, R.color.black_text, activity);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View containerView = findViewById(R.id.container_settings_notifications);
            if (containerView != null && !settings.isLoggedIn()) {
                Snackbar snackbar = Snackbar.make(containerView, getString(R.string.notifications_not_logged_in), Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_notifications);

        checkChooseWhoViability();

        final Toolbar toolbar = findViewById(R.id.settings_notifications_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        //Service.isTranslucentActionbar(LOG_TAG, getBaseContext(), toolbar, this);

        final TextView currentCheckView = findViewById(R.id.notifications_current_check);
        String interval = settings.getNotificationsCheckInterval();
        // Format the string received from settings in a correct way
        interval = formatCheckInterval(interval, this);
        if (currentCheckView != null) {
            currentCheckView.setText(interval);
        }

        final CheckedTextView vibrateView = findViewById(R.id.notifications_vibrate);
        if (vibrateView != null) {
            vibrateView.setChecked(settings.getNotificationsVibrations());

            vibrateView.setOnClickListener(v -> {
                if (vibrateView.isChecked()) {
                    vibrateView.setChecked(false);
                    settings.setNotificationsVibration(false);
                } else {
                    vibrateView.setChecked(true);
                    settings.setNotificationsVibration(true);
                }
            });
        }

        final CheckedTextView screenWakeView = findViewById(R.id.notifications_screen_wake);
        if (screenWakeView != null) {
            screenWakeView.setChecked(settings.getNotificationsScreenWake());

            screenWakeView.setOnClickListener(v -> {
                if (screenWakeView.isChecked()) {
                    screenWakeView.setChecked(false);
                    settings.setNotificationsScreenWake(false);
                } else {
                    screenWakeView.setChecked(true);
                    settings.setNotificationsScreenWake(true);
                }
            });
        }

        final CheckedTextView soundView = findViewById(R.id.notifications_sound);
        if (soundView != null) {
            soundView.setChecked(settings.getNotificationsSound());

            soundView.setOnClickListener(v -> {
                if (soundView.isChecked()) {
                    soundView.setChecked(false);
                    settings.setNotificationsSound(false);
                } else {
                    soundView.setChecked(true);
                    settings.setNotificationsSound(true);
                }
            });
        }

        final CheckedTextView ledView = findViewById(R.id.notifications_led);
        if (ledView != null) {
            ledView.setChecked(settings.getNotificationsLED());

            ledView.setOnClickListener(v -> {
                if (ledView.isChecked()) {
                    ledView.setChecked(false);
                    settings.setNotificationsLED(false);
                } else {
                    ledView.setChecked(true);
                    settings.setNotificationsLED(true);
                }
            });
        }

        final CheckedTextView quietHoursView = findViewById(R.id.notifications_quiet_hours);
        boolean isQuietHoursEnabled = settings.getNotificationsQuietHours();
        if (quietHoursView != null) {
            quietHoursView.setChecked(isQuietHoursEnabled);
        }
        final Activity activity = this;
        enableQuietHours(isQuietHoursEnabled, activity);


        if (quietHoursView != null) {
            quietHoursView.setOnClickListener(v -> {
                if (quietHoursView.isChecked()) {
                    quietHoursView.setChecked(false);
                    settings.setNotificationsQuietHours(false);
                    enableQuietHours(false, activity);

                } else {
                    quietHoursView.setChecked(true);
                    settings.setNotificationsQuietHours(true);
                    enableQuietHours(true, activity);
                }
            });
        }

        final CheckedTextView showNotificationsDuringQuietHoursView = findViewById(R.id.notifications_show_in_quiet_hours);
        showNotificationsDuringQuietHoursView.setChecked(settings.isNotificationsShowInQuietHours());
        showNotificationsDuringQuietHoursView.setOnClickListener(view -> {
            if (showNotificationsDuringQuietHoursView.isChecked()) {
                showNotificationsDuringQuietHoursView.setChecked(false);
                settings.setNotificationsShowInQuietHours(false);
            } else {
                showNotificationsDuringQuietHoursView.setChecked(true);
                settings.setNotificationsShowInQuietHours(true);
            }
        });

        final TextView quietHourStartView = findViewById(R.id.notifications_quiet_hours_start);
        String timeOfDayStart = getResources().getString(R.string.notifications_time_of_day, formatTimeOfDay(settings.getNotificationsQuietStartHour()), formatTimeOfDay(settings.getNotificationsQuietStartMinute()));
        if (quietHourStartView != null) {
            quietHourStartView.setText(timeOfDayStart);
        }

        final TextView quietHourEndView = findViewById(R.id.notifications_quiet_hours_end);
        String timeOfDayEnd = getResources().getString(R.string.notifications_time_of_day, formatTimeOfDay(settings.getNotificationsQuietEndHour()), formatTimeOfDay(settings.getNotificationsQuietEndMinute()));
        if (quietHourEndView != null) {
            quietHourEndView.setText(timeOfDayEnd);
        }

        // Enable or disable this activities views depending if notifications are enabled or not
        if (interval.equals(getResources().getString(R.string.notifications_disabled_key))) {
            enableNotifications(false, isQuietHoursEnabled, this);
        } else {
            enableNotifications(true, isQuietHoursEnabled, this);
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

    private void checkChooseWhoViability() {
        View chooseWho = findViewById(R.id.notifications_choose_who_ripple);

        if (!settings.isLoggedIn() || !settings.isSetup()) {
            chooseWho.setVisibility(View.GONE);
        }
    }

    public void chooseStreamers(View v) {
        Intent intent = new Intent(getBaseContext(), SettingsPickNotificationsActivity.class);
        startActivity(intent);
    }

    public void setQuietHoursStart(View v) {
        final Settings settings = new Settings(getBaseContext());

        final TimePickerDialog dialog = new TimePickerDialog(this);
        dialog
                .hour(settings.getNotificationsQuietStartHour())
                .minute(settings.getNotificationsQuietStartMinute())
                .positiveAction(getResources().getString(R.string.ok))
                .positiveActionClickListener(v12 -> {
                    int hour = dialog.getHour();
                    int minute = dialog.getMinute();
                    Log.d(LOG_TAG, "Quiet Hours Start Time has changed to- " + hour + ":" + minute);
                    settings.setNotificationsQuietStartHour(hour);
                    settings.setNotificationsQuietStartMinute(minute);

                    final TextView quietHourStartView = findViewById(R.id.notifications_quiet_hours_start);
                    String timeOfDay = getResources().getString(R.string.notifications_time_of_day, formatTimeOfDay(hour), formatTimeOfDay(minute));
                    if (quietHourStartView != null) {
                        quietHourStartView.setText(timeOfDay);
                    }
                    dialog.dismiss();
                })
                .negativeAction(getResources().getString(R.string.cancel))
                .negativeActionClickListener(v1 -> dialog.cancel())
                .cancelable(true)
                .show();
    }

    public void setQuietHoursEnd(View v) {
        final Settings settings = new Settings(getBaseContext());

        final TimePickerDialog dialog = new TimePickerDialog(this);
        dialog
                .hour(settings.getNotificationsQuietEndHour())
                .minute(settings.getNotificationsQuietEndMinute())
                .positiveAction(getResources().getString(R.string.ok))
                .positiveActionClickListener(v12 -> {
                    int hour = dialog.getHour();
                    int minute = dialog.getMinute();
                    Log.d(LOG_TAG, "Quiet Hours End Time has changed to- " + hour + ":" + minute);
                    settings.setNotificationsQuietEndHour(hour);
                    settings.setNotificationsQuietEndMinute(minute);

                    final TextView quietHourEndView = findViewById(R.id.notifications_quiet_hours_end);
                    String timeOfDay = getResources().getString(R.string.notifications_time_of_day, formatTimeOfDay(hour), formatTimeOfDay(minute));
                    if (quietHourEndView != null) {
                        quietHourEndView.setText(timeOfDay);
                    }
                    dialog.dismiss();
                })
                .negativeAction(getResources().getString(R.string.cancel))
                .negativeActionClickListener(v1 -> dialog.cancel())
                .cancelable(true)
                .show();
    }

    public void setCheckInterval(View v) {
        CheckIntervalFragment timePickerFragment = new CheckIntervalFragment();
        timePickerFragment.show(getFragmentManager(), "checkIntervalPicker");
    }

    /**
     * Dialog for picking the notifications check interval
     */

    public static class CheckIntervalFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Settings settings = new Settings(getActivity().getBaseContext());
            String interval = settings.getNotificationsCheckInterval();

            // Get the correct position of currently checked item
            int positionOfInterval = 0;

            switch (interval) {
                case "15":
                    positionOfInterval = 1;
                    break;
                case "30":
                    positionOfInterval = 2;
                    break;
                case "60":
                    positionOfInterval = 3;
                    break;
                case "120":
                    positionOfInterval = 4;
                    break;
            }
            final int finalPosition = positionOfInterval;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.notifications_check_interval)
                    .setSingleChoiceItems(R.array.checkIntervals, positionOfInterval,
                            (dialog, which) -> dialog.dismiss())

                    .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());

            return builder.create();
        }
    }
}
