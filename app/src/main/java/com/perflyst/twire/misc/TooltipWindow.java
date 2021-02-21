package com.perflyst.twire.misc;

/*
 * Created by Sebastian Rask on 10-05-2016.
 */

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.perflyst.twire.R;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;


public class TooltipWindow {

    public static final int POSITION_TO_RIGHT = 0;
    public static final int POSITION_BOTTOM = 1;
    private static final int MSG_DISMISS_TOOLTIP = 100;
    private final int REVEAL_DURATION = 500;
    private final PopupWindow tipWindow;
    private final View contentView;
    private final TextView mTipText;
    private final ImageView mNavLeftArrow;
    private final ImageView mNavUpArrow;
    private final LinearLayout mainLayout;
    private final int position;
    private SupportAnimator revealTransition;
    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_DISMISS_TOOLTIP) {
                if (tipWindow != null && tipWindow.isShowing()) {
                    if (revealTransition != null) {
                        SupportAnimator hideAnim = revealTransition.reverse();
                        if (hideAnim != null) {
                            hideAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                            hideAnim.setDuration(REVEAL_DURATION);
                            hideAnim.addListener(new SupportAnimator.AnimatorListener() {
                                @Override
                                public void onAnimationStart() {

                                }

                                @Override
                                public void onAnimationEnd() {
                                    if (tipWindow != null) {
                                        tipWindow.dismiss();
                                    }
                                }

                                @Override
                                public void onAnimationCancel() {

                                }

                                @Override
                                public void onAnimationRepeat() {

                                }
                            });
                            hideAnim.start();
                        } else {
                            if (tipWindow != null) {
                                tipWindow.dismiss();
                            }
                        }


                    } else {
                        tipWindow.dismiss();
                    }
                }
            }
        }
    };

    public TooltipWindow(Context ctx, int aPosition) {
        tipWindow = new PopupWindow(ctx);
        position = aPosition;

        contentView = LayoutInflater.from(ctx).inflate(R.layout.tooltip_layout, null);
        mTipText = contentView.findViewById(R.id.tooltip_text);
        mNavLeftArrow = contentView.findViewById(R.id.tooltip_nav_left);
        mNavUpArrow = contentView.findViewById(R.id.tooltip_nav_up);
        mainLayout = contentView.findViewById(R.id.main_layout);

    }

    public void showToolTip(View anchor, String tipText) {
        mTipText.setText(tipText);

        tipWindow.setHeight(LayoutParams.WRAP_CONTENT);
        tipWindow.setWidth(LayoutParams.WRAP_CONTENT);

        tipWindow.setOutsideTouchable(true);
        tipWindow.setTouchable(true);

        tipWindow.setBackgroundDrawable(new BitmapDrawable());

        tipWindow.setContentView(contentView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tipWindow.setElevation(1f);
        }

        int[] screen_pos = new int[2];
        anchor.getLocationOnScreen(screen_pos);

        // Get rect for anchor view
        Rect anchor_rect = new Rect(screen_pos[0], screen_pos[1], screen_pos[0]
                + anchor.getWidth(), screen_pos[1] + anchor.getHeight());

        // Call view measure to calculate how big your view should be.
        contentView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        int position_x = 0;
        int position_y = 0;
        if (position == POSITION_TO_RIGHT) {
            position_x = anchor_rect.right;
            position_y = (int) anchor.getY() + anchor.getHeight() / 2;
        } else if (position == POSITION_BOTTOM) {
            mNavUpArrow.setVisibility(View.VISIBLE);
            mNavLeftArrow.setVisibility(View.GONE);
            mTipText.forceLayout();

            position_x = anchor_rect.centerX() - (contentView.getMeasuredWidth() / 2);//anchor_rect.centerX() - (contentView.getWidth() / 2);
            position_y = anchor_rect.bottom - (anchor_rect.height() / 2);
        }

        tipWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, position_x,
                position_y);


        contentView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            revealTransition = ViewAnimationUtils.createCircularReveal(mainLayout, (int) mainLayout.getX(), (int) (mainLayout.getY() + mainLayout.getHeight() / 2), 0, mainLayout.getWidth());
            revealTransition.setInterpolator(new AccelerateDecelerateInterpolator());
            revealTransition.setDuration(REVEAL_DURATION);
            revealTransition.addListener(new SupportAnimator.AnimatorListener() {
                @Override
                public void onAnimationStart() {
                    mainLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd() {

                }

                @Override
                public void onAnimationCancel() {

                }

                @Override
                public void onAnimationRepeat() {

                }
            });
            revealTransition.start();
        });

        int hideDelay = 1000 * 5;
        handler.sendEmptyMessageDelayed(MSG_DISMISS_TOOLTIP, hideDelay);
    }

    public boolean isTooltipShown() {
        return tipWindow != null && tipWindow.isShowing();
    }

    public void dismissTooltip() {
        if (tipWindow != null && tipWindow.isShowing())
            tipWindow.dismiss();
    }

}
