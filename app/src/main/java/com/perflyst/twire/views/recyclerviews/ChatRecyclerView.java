package com.perflyst.twire.views.recyclerviews;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
        return amountScrolled > 1;
    }

    public void setChatPaused(TextView chatPaused) {
        this.chatPaused = chatPaused;
        chatPaused.setOnClickListener((v) -> smoothScrollToPosition(this.getAdapter().getItemCount() - 1));
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!isScrolled() && newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
            scrollToPosition(getAdapter().getItemCount() - 1);
    }

    private void setScrolledListener() {
        this.addOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
                amountScrolled = layoutManager.getItemCount() - layoutManager.findLastCompletelyVisibleItemPosition() - 1;

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
