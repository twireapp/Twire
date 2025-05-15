package com.perflyst.twire.model

/**
 * Created by Sebastian Rask on 24-02-2017.
 */
class Panel(
    private val mStreamerName: String?,
    private val mUserId: String?,
    private var mOrder: Int,
    private val mDescription: String?,
    private val mImageUrl: String?,
    private val mLinkUrl: String?,
    private val mTitle: String?,
    private val mHtml: String
) {
    fun getmHtml(): String {
        return mHtml
    }

    fun getmStreamerName(): String? {
        return mStreamerName
    }

    fun getmUserId(): String? {
        return mUserId
    }

    fun getmOrder(): Int {
        return mOrder
    }

    fun setmOrder(mOrder: Int) {
        this.mOrder = mOrder
    }

    fun getmDescription(): String? {
        return mDescription
    }

    fun getmImageUrl(): String? {
        return mImageUrl
    }

    fun getmLinkUrl(): String? {
        return mLinkUrl
    }

    fun getmTitle(): String? {
        return mTitle
    }
}
