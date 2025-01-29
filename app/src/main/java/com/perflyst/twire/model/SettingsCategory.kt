package com.perflyst.twire.model

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Created by Sebastian Rask on 16-05-2017.
 */
class SettingsCategory(
    @JvmField @field:StringRes val titleRes: Int,
    @JvmField @field:StringRes val summaryRes: Int,
    @JvmField @field:DrawableRes val iconRes: Int,
    @JvmField val intent: Intent
)
