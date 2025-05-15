package com.perflyst.twire.utils

object Constants {
    val TWITCH_SCOPES: Array<String> = arrayOf(
        "user:read:email",
        "user:edit:follows",
        "user:read:subscriptions",
        "chat:edit",
        "chat:read",
        "user:read:follows"
    )

    const val KEY_CLIP: String = "key_clip"
}
