package com.perflyst.twire.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
open class UserInfo(
    open var userId: String,
    open var login: String,
    open var displayName: String
) :
    Parcelable {
    override fun toString(): String = this.displayName

    override fun equals(other: Any?): Boolean = when (other) {
        is UserInfo -> this.userId == other.userId
        else -> false
    }

    override fun hashCode(): Int = userId.hashCode()
}
