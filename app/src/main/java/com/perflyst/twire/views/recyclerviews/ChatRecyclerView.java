package com.perflyst.twire.views.recyclerviews;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.R;

/**
 * Created by Sebastian Rask on 30-03-2016.
 */
public class ChatRecyclerView extends RecyclerView {
    private int amountScrolled = 0;

    private TextView chatPaused;
    private boolean lastScrolled = false;

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
        float min = -1 * getContext().getResources().getDimension(R.dimen.chat_message_text_size);
        return amountScrolled < min;
    }

    public void setChatPaused(TextView chatPaused) {
        this.chatPaused = chatPaused;
        chatPaused.setOnClickListener((v) -> smoothScrollToPosition(this.getAdapter().getItemCount() - 1));
    }

    @Override
    public void scrollToPosition(int position) {
        super.scrollToPosition(position);
        amountScrolled = 0;
    }

    private void setScrolledListener() {
        this.addOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                amountScrolled += dy;

                if (chatPaused == null) return;

                boolean scrolled = isScrolled();
                if (scrolled != lastScrolled) {
                    chatPaused.animate().alpha(scrolled ? 1 : 0).start();
                    lastScrolled = scrolled;
                }
            }
        });
    }
}
