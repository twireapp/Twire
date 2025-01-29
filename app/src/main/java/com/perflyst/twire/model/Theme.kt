package com.perflyst.twire.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.perflyst.twire.R

enum class Theme(
    @field:StringRes @param:StringRes val nameRes: Int,
    @JvmField @field:DrawableRes @param:DrawableRes val chooser: Int,
    @JvmField @field:StyleRes @param:StyleRes val style: Int
) {
    BLUE(R.string.blue_theme_name, R.drawable.circle_theme_blue_chooser, R.style.BlueTheme),
    PURPLE(R.string.purple_theme_name, R.drawable.circle_theme_purple_chooser, R.style.PurpleTheme),
    BLACK(R.string.black_theme_name, R.drawable.circle_theme_black_chooser, R.style.BlackTheme),
    NIGHT(R.string.night_theme_name, R.drawable.circle_theme_night_chooser, R.style.NightTheme),
    TRUE_NIGHT(
        R.string.true_night_theme_name,
        R.drawable.circle_theme_black_chooser,
        R.style.TrueNightTheme
    )
}
