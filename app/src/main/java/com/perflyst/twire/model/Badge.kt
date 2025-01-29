package com.perflyst.twire.model

import android.util.SparseArray


class Badge @JvmOverloads constructor(
    @JvmField val name: String,
    private val urls: SparseArray<String>,
    @JvmField val color: String? = null,
    @JvmField val replaces: String? = null
) {
    fun getUrl(size: Int): String {
        return urls[getBestAvailableSize(size)]
    }

    private fun getBestAvailableSize(size: Int): Int {
        for (i in size downTo 1) {
            if (urls.indexOfKey(i) >= 0) {
                return i
            }
        }

        return 1
    }
}
