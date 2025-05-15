package com.perflyst.twire.views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatEditText

/**
 * Created by Sebastian Rask Jepsen on 28/07/16.
 */
class EditTextBackEvent : AppCompatEditText {
    private var mOnImeBack: EditTextImeBackListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK &&
            event.action == KeyEvent.ACTION_UP
        ) {
            if (mOnImeBack != null) mOnImeBack!!.onImeBack(this, this.getText().toString())
        }
        return super.onKeyPreIme(keyCode, event)
    }

    fun setOnEditTextImeBackListener(listener: EditTextImeBackListener?) {
        mOnImeBack = listener
    }

    fun interface EditTextImeBackListener {
        fun onImeBack(ctrl: EditTextBackEvent?, text: String?)
    }
}


