package com.perflyst.twire.views.recyclerviews

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.perflyst.twire.R
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour
import kotlin.math.max

/**
 * Created by Sebastian Rask Jepsen on 08-02-2016.
 */
class AutoSpanRecyclerView : RecyclerView {
    private lateinit var mManager: GridLayoutManager
    private var mSpanCount = 0
    private var mScrollAmount = 0
    private var scrolled = false
    private var mSizeName: String? = null
    private var mBehaviour: AutoSpanBehaviour? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    private fun init() {
        mSpanCount = 1

        this.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                mScrollAmount += dy
                setScrolled(mScrollAmount != 0)
            }
        })

        mManager = GridLayoutManager(context, mSpanCount)
        setLayoutManager(mManager)
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        if (mBehaviour == null) {
            return
        }

        val mElementWidth = mBehaviour!!.getElementWidth(context)

        if (mElementWidth > 0) {
            mSpanCount = max(1, measuredWidth / mElementWidth)
            this.spanCount = mSpanCount
        }
    }

    fun hasSizedChanged(): Boolean {
        if (mBehaviour == null) {
            return false
        }

        val newSizeName: String? = mBehaviour!!.elementSizeName
        if (mSizeName != null && mSizeName != newSizeName) {
            mSizeName = newSizeName
            return true
        }
        return false
    }

    val manager: GridLayoutManager
        get() = mManager

    fun hasScrolled(): Boolean {
        return scrolled
    }

    fun setScrolled(isScrolled: Boolean) {
        scrolled = isScrolled
    }

    var spanCount: Int
        get() = mManager.spanCount
        private set(count) {
            var additionSpan = 0
            if (mBehaviour == null) {
                mManager.setSpanCount(count)
                return
            }

            mSizeName = mBehaviour!!.elementSizeName
            if (mSizeName == context.getString(R.string.card_size_normal)) {
                additionSpan = 1
            } else if (mSizeName == context.getString(R.string.card_size_small)) {
                additionSpan = 2
            }

            if (mSizeName == context.getString(R.string.card_size_huge)) {
                mManager.setSpanCount(1)
            } else {
                mManager.setSpanCount(count + additionSpan)
            }
        }

    fun setBehaviour(mBehaviour: AutoSpanBehaviour?) {
        this.mBehaviour = mBehaviour
    }

    val elementWidth: Int
        get() = mBehaviour!!.getElementWidth(context)
}
