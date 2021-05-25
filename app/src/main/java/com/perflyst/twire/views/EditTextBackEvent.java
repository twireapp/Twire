package com.perflyst.twire.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.appcompat.widget.AppCompatEditText;

/**
 * Created by Sebastian Rask Jepsen on 28/07/16.
 */
public class EditTextBackEvent extends AppCompatEditText {

    private EditTextImeBackListener mOnImeBack;

    public EditTextBackEvent(Context context) {
        super(context);
    }

    public EditTextBackEvent(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextBackEvent(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK &&
                event.getAction() == KeyEvent.ACTION_UP) {
            if (mOnImeBack != null)
                mOnImeBack.onImeBack(this, this.getText().toString());
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void setOnEditTextImeBackListener(EditTextImeBackListener listener) {
        mOnImeBack = listener;
    }

    public interface EditTextImeBackListener {
        void onImeBack(EditTextBackEvent ctrl, String text);
    }
}


