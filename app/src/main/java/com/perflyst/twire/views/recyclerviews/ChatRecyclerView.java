package com.perflyst.twire.views.recyclerviews;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.R;

/**
 * Created by Sebastian Rask on 30-03-2016.
 */
public class ChatRecyclerView extends RecyclerView {
    private String LOG_TAG = getClass().getSimpleName();
    private int amountScrolled = 0;

    public ChatRecyclerView(Context context) {
        super(context);
        setScrolledListener();
    }

    public ChatRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScrolledListener();
    }

    public ChatRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setScrolledListener();
    }

    public boolean isScrolled() {
        float min = -1 * getContext().getResources().getDimension(R.dimen.chat_message_text_sie);
        return amountScrolled < min;
    }

    @Override
    public void scrollToPosition(int position) {
        super.scrollToPosition(position);
        amountScrolled = 0;
    }

    private void setScrolledListener() {
        this.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                amountScrolled += dy;
            }
        });
    }
}
