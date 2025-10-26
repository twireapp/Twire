package com.perflyst.twire.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

/**
 * Created by Sebastian Rask on 16-05-2017.
 */
class SettingsCategory(
    @JvmField @field:StringRes val titleRes: Int,
    @JvmField @field:StringRes val summaryRes: Int,
    @JvmField @field:DrawableRes val iconRes: Int,
    @JvmField val fragment: Class<out Fragment>
)
