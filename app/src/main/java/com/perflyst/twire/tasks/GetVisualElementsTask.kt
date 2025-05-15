package com.perflyst.twire.tasks

import com.perflyst.twire.activities.main.LazyFetchingActivity
import com.perflyst.twire.utils.Execute
import timber.log.Timber
import java.util.concurrent.Callable

class GetVisualElementsTask<T>(private val mLazyActivity: LazyFetchingActivity<T>) :
    Callable<MutableList<T>> {
    override fun call(): MutableList<T> {
        val resultList: MutableList<T> = ArrayList()

        try {
            resultList.addAll(mLazyActivity.visualElements)
        } catch (_: InterruptedException) {
            return resultList
        } catch (e: Exception) {
            Timber.e(e)
        }

        Execute.ui {
            if (resultList.isEmpty()) {
                Timber.i("ADDING 0 VISUAL ELEMENTS")
                mLazyActivity.notifyUserNoElementsAdded()
            }
            mLazyActivity.addToAdapter(resultList)
            mLazyActivity.stopProgress()
            mLazyActivity.stopRefreshing()
        }

        return resultList
    }
}
