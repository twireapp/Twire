package com.perflyst.twire.activities.main

/**
 * Created by SebastianRask on 18-09-2015.
 */
interface LazyFetchingActivity<T> {
    fun addToAdapter(aObjectList: MutableList<T>)

    fun startRefreshing()

    fun stopRefreshing()

    var cursor: String?

    fun startProgress()

    fun stopProgress()

    var limit: Int

    var maxElementsToFetch: Int

    fun notifyUserNoElementsAdded()

    @get:Throws(Exception::class)
    val visualElements: MutableList<T>
}
