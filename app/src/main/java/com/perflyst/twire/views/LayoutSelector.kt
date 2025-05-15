package com.perflyst.twire.views

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.annotation.ArrayRes
import androidx.annotation.AttrRes
import androidx.annotation.DimenRes
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatRadioButton
import com.perflyst.twire.R
import com.perflyst.twire.service.Service.getColorAttribute

/**
 * Created by Sebastian Rask on 13-05-2016.
 */
class LayoutSelector(
    @field:LayoutRes @param:LayoutRes private val previewLayout: Int,
    @ArrayRes choices: Int,
    private val selectCallback: OnLayoutSelected,
    private val activity: Activity
) {
    private val layoutTitles: Array<String?> = activity.resources.getStringArray(choices)
    private var layoutSelectorView: View? = null

    @AttrRes
    private var textColor = -1

    @DimenRes
    private var previewMaxHeightRes = -1
    private var selectedLayoutIndex = -1
    private var selectedLayoutTitle: String? = null

    private fun init() {
        layoutSelectorView =
            activity.layoutInflater.inflate(R.layout.stream_layout_preview, null)
        val rg = layoutSelectorView!!.findViewById<RadioGroup>(R.id.layouts_radiogroup)
        val previewWrapper = layoutSelectorView!!.findViewById<FrameLayout>(R.id.preview_wrapper)

        if (previewMaxHeightRes != -1) {
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                activity.resources.getDimension(previewMaxHeightRes).toInt()
            )
            previewWrapper.setLayoutParams(lp)
            //previewWrapper.setMinimumHeight((int) activity.getResources().getDimension(previewMaxHeightRes));
        }

        val preview = layoutSelectorView!!.findViewById<ViewStub>(R.id.layout_stub)
        preview.layoutResource = previewLayout
        val inflated = preview.inflate()

        for (i in layoutTitles.indices) {
            val layoutTitle = layoutTitles[i]

            val radioButton = AppCompatRadioButton(activity)
            radioButton.text = layoutTitle

            val finalI = i
            radioButton.setOnClickListener { v: View? ->
                selectCallback.onSelected(
                    layoutTitle,
                    finalI,
                    inflated
                )
            }

            if (textColor != -1) {
                radioButton.setTextColor(getColorAttribute(textColor, R.color.black_text, activity))

                val colorStateList = ColorStateList(
                    arrayOf<IntArray?>(
                        intArrayOf(-android.R.attr.state_checked),
                        intArrayOf(android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        Color.GRAY,  //Disabled
                        getColorAttribute(
                            androidx.appcompat.R.attr.colorAccent,
                            R.color.accent,
                            activity
                        ),  //Enabled
                    )
                )
                radioButton.setButtonTintList(colorStateList)
            }


            radioButton.setLayoutParams(
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,  // Width
                    activity.resources.getDimension(R.dimen.layout_selector_height)
                        .toInt() // Height
                )
            )


            rg.addView(radioButton, i)


            if (selectedLayoutIndex != -1 && selectedLayoutIndex == i || selectedLayoutTitle != null && selectedLayoutTitle == layoutTitle) {
                radioButton.performClick()
            }
        }
    }

    fun setTextColorAttr(@AttrRes textAppearanceResource: Int): LayoutSelector {
        textColor = textAppearanceResource
        return this
    }

    fun setSelectedLayoutIndex(selectedLayoutIndex: Int): LayoutSelector {
        this.selectedLayoutIndex = selectedLayoutIndex
        return this
    }

    fun setSelectedLayoutTitle(selectedLayoutTitle: String?): LayoutSelector {
        this.selectedLayoutTitle = selectedLayoutTitle
        return this
    }

    fun setPreviewMaxHeightRes(previewMaxHeightRes: Int): LayoutSelector {
        this.previewMaxHeightRes = previewMaxHeightRes
        return this
    }

    fun build(): View {
        init()
        return layoutSelectorView!!
    }

    fun interface OnLayoutSelected {
        fun onSelected(title: String?, index: Int, previewView: View?)
    }
}
