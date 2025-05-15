package com.perflyst.twire.misc

import java.util.Locale

object OnlineSince {
    fun getOnlineSince(startedAt: Long): String {
        val seconds = (System.currentTimeMillis() - startedAt) / 1000
        return String.format(
            Locale.US,
            "%d:%02d:%02d",
            seconds / 3600,
            (seconds % 3600) / 60,
            (seconds % 60)
        )
    }
}
