package com.perflyst.twire.misc;

import android.animation.ArgbEvaluator;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.R;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;

/**
 * Created by Sebastian on 06-08-2015.
 */
public class UniversalOnScrollListener extends RecyclerView.OnScrollListener {
    private final String LOG_TAG;
    private final boolean isMainActivity;
    private final AppCompatActivity mActivity;
    private final Toolbar mMainToolbar;
    private final Toolbar mDecorativeToolbar;
    private final View mToolbarShadow;
    private final View mIconCircle;
    private final TextView mIconText;
    // variables to control main toolbar shadow
    private float minAmountToScroll, minAmountToolbarHide;
    private int amountScrolled;
    private boolean isToolbarTransparent;
    private ValueAnimator mFadeToolbarAnimation;

    private Animation mShowShadowAnimation;
    private Animation mFadeShadowAnimation;

    public UniversalOnScrollListener(AppCompatActivity mActivity, Toolbar mMainToolbar, Toolbar mDecorativeToolbar, View mToolbarShadow, View mIconCircle, TextView mIconText, String LOG_TAG, boolean isMainActivity) {
        this.LOG_TAG = LOG_TAG;
        this.mActivity = mActivity;
        this.mMainToolbar = mMainToolbar;
        this.mDecorativeToolbar = mDecorativeToolbar;
        this.mToolbarShadow = mToolbarShadow;
        this.mIconCircle = mIconCircle;
        this.mIconText = mIconText;

        this.isToolbarTransparent = true;
        this.isMainActivity = isMainActivity;
        this.amountScrolled = 0;
        if (isMainActivity) {
            this.minAmountToScroll = mActivity.getBaseContext().getResources().getDimension(R.dimen.stream_card_first_top_margin) - mActivity.getBaseContext().getResources().getDimension(R.dimen.main_toolbar_height);
            this.minAmountToolbarHide = mActivity.getBaseContext().getResources().getDimension(R.dimen.stream_card_first_top_margin);
        }

        this.initAnimations();
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        if (isMainActivity) {
            float toolbarTranslationY = mMainToolbar.getTranslationY(); // Save the Y value to a variable to prevent "getting" it multiple times
            int TOOLBAR_HEIGHT = (int) mActivity.getResources().getDimension(R.dimen.main_toolbar_height);
            if (mActivity.getSupportActionBar() != null) {
                TOOLBAR_HEIGHT = mActivity.getSupportActionBar().getHeight();
            }

            // If the toolbar is not fully drawn when the scrolling state is idle - animate to be either fully shown or fully hidden.
            // But don't run the animation if the amountScrolled amount is less that the toolbar height
            // Also animate the shadow the length and direction
            if (amountScrolled > mActivity.getResources().getDimension(R.dimen.additional_toolbar_height)) {
                if (newState == 0 && toolbarTranslationY > -TOOLBAR_HEIGHT && toolbarTranslationY < -(TOOLBAR_HEIGHT / 2.0f) && amountScrolled > TOOLBAR_HEIGHT) {
                    // Hide animation
                    mToolbarShadow.animate().translationY(-TOOLBAR_HEIGHT).setInterpolator(new AccelerateInterpolator()).start();
                    mMainToolbar.animate().translationY(-TOOLBAR_HEIGHT).setInterpolator(new AccelerateInterpolator()).start();
                } else if (newState == 0 && toolbarTranslationY > -(TOOLBAR_HEIGHT / 2.0f) && toolbarTranslationY < 0 && amountScrolled > TOOLBAR_HEIGHT) {
                    // Show animation
                    mToolbarShadow.animate().translationY(0).setInterpolator(new AccelerateInterpolator()).start();
                    mMainToolbar.animate().translationY(0).setInterpolator(new AccelerateInterpolator()).start();
                }
            }
        }
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (isMainActivity) {
            int TOOLBAR_HEIGHT = (int) mActivity.getResources().getDimension(R.dimen.main_toolbar_height);
            if (mActivity.getSupportActionBar() != null) {
                TOOLBAR_HEIGHT = mActivity.getSupportActionBar().getHeight();
            }

            final double SLOW_SCROLL_RATE = 0.75;
            //  Save the Y values to a variable to prevent "getting" it multiple times
            float decoToolbarTranslationY = mDecorativeToolbar.getTranslationY();
            float toolbarTranslationY = mMainToolbar.getTranslationY();
            float shadowViewTranslationY = mToolbarShadow.getTranslationY();
            float iconViewTranslationY = mIconCircle.getTranslationY();
            float iconTextViewTranslationY = mIconText.getTranslationY();

            // If the user scrolls up and the toolbar is currently showing
            // Subtract the scroll amount from the Y-position of the toolbar and shadow
            if (dy > 0) {
                amountScrolled += dy;

                // Always Scroll the decorate toolbar slower than the main one.
                mDecorativeToolbar.setTranslationY(decoToolbarTranslationY - (dy * (float) SLOW_SCROLL_RATE));
                mIconCircle.setTranslationY(iconViewTranslationY - dy);
                mIconText.setTranslationY(iconTextViewTranslationY - dy);

                if (amountScrolled < minAmountToScroll) {
                    // Update the transparency of the activity icon
                    if (mIconCircle.getAlpha() >= 0) {
                        float newAlphaHex = 255 - (mIconCircle.getTranslationY() * -1);
                        float newAlphaFloat = newAlphaHex / 255;
                        if (newAlphaFloat >= 0) {
                            mIconCircle.setAlpha(newAlphaFloat);
                        }
                    }
                } else {
                    // Scroll up the toolbar and the shadow
                    if (toolbarTranslationY > -TOOLBAR_HEIGHT) {
                        mMainToolbar.setTranslationY(toolbarTranslationY - dy);
                        mToolbarShadow.setTranslationY(shadowViewTranslationY - dy);
                    }
                    if (mIconCircle.getAlpha() > 0) {
                        mIconCircle.setAlpha(0);
                    }
                }
            }

            // If the user scrolls down
            if (dy < 0) {
                amountScrolled += dy;

                // Always scroll for these Views. But Scroll the decorative toolbar slower than the others
                mDecorativeToolbar.setTranslationY(decoToolbarTranslationY - (dy * (float) SLOW_SCROLL_RATE));
                mIconCircle.setTranslationY(iconViewTranslationY - dy);
                mIconText.setTranslationY(iconTextViewTranslationY - dy);

                if (amountScrolled < minAmountToolbarHide) {
                    if (!isToolbarTransparent) {
                        isToolbarTransparent = true;

                        mFadeToolbarAnimation.start();
                        mToolbarShadow.startAnimation(mFadeShadowAnimation);
                    }
                } else if (amountScrolled > minAmountToolbarHide + mActivity.getBaseContext().getResources().getDimension(R.dimen.main_toolbar_height)) {
                    if (isToolbarTransparent) {
                        // If the toolbar and its shadow is currently transparent/hidden,
                        // then show it but increasing Alpha on the shadow and settings the color of the toolbar

                        isToolbarTransparent = false;

                        mMainToolbar.setBackgroundColor(Service.getColorAttribute(R.attr.colorPrimary, R.color.primary, mActivity));
                        mToolbarShadow.startAnimation(mShowShadowAnimation);
                    }
                }

                if (amountScrolled < minAmountToScroll) {
                    // If the toolbar and its shadow is not currently transparent hide them
                    // by animating the alpha value of the shadow the animating the color of the toolbar


                    if (mIconCircle.getAlpha() >= 0) {
                        float newAlphaHex = 255 - (mIconCircle.getTranslationY() * -1);
                        float newAlphaFloat = newAlphaHex / 255;
                        if (newAlphaFloat >= 0) {
                            mIconCircle.setAlpha(newAlphaFloat);
                        }
                    }

                    // Make sure the toolbar is half-way showed or hid if the user scrolls fast down while the toolbar is naturally getting hidden
                    mToolbarShadow.setTranslationY(0);
                    mMainToolbar.setTranslationY(0);

                } else {


                    // As long as the toolbar is not fully shown - Subtract the scroll amount from the Y-position
                    if (toolbarTranslationY < 0) {
                        // Make sure the toolbar can't get further down than intended
                        if (toolbarTranslationY - dy > 0) {
                            mMainToolbar.setTranslationY(0);
                        } else {
                            mMainToolbar.setTranslationY(toolbarTranslationY - dy);
                        }
                    }

                    // Do the same for the shadow
                    if (shadowViewTranslationY < 0) {
                        if (shadowViewTranslationY - dy > 0)
                            mToolbarShadow.setTranslationY(0);
                        else
                            mToolbarShadow.setTranslationY(shadowViewTranslationY - dy);
                    }
                }

                // Make sure that these Views don't get scrolled too far down if the user scrolls really fast

                if (decoToolbarTranslationY >= 0)
                    mDecorativeToolbar.setTranslationY(0);

                if (iconViewTranslationY >= 0)
                    mIconCircle.setTranslationY(0);

                if (iconTextViewTranslationY >= 0)
                    mIconText.setTranslationY(0);
            }


            if (recyclerView instanceof AutoSpanRecyclerView) {
                ((AutoSpanRecyclerView) recyclerView).setScrolled(amountScrolled != 0);
            }

            if (amountScrolled < 0) {
                amountScrolled = 0;
                Log.w(LOG_TAG, "RESETTING SCROLLED");
            }
        }
    }

    public int getAmountScrolled() {
        return amountScrolled;
    }

    private void initAnimations() {
        if (isMainActivity) {
            // Show and fade durations for the animations below.
            // We want the Toolbar and its shadow to show and hide in the same matter of time.
            final int SHOW_DURATION = 200; // Millis
            final int FADE_DURATION = 400; // Millis
            final int SHADOW_FADE_DURATION = 200;

            // Animations for showing and hiding the toolbar.
            // The animation changes the color of the toolbar from a transparent blue to the full blue.
            // Currently only the fade animation is being used.
            int colorTo = Service.getColorAttribute(R.attr.colorPrimary, R.color.primary, mActivity);
            Integer colorFrom = Color.argb(0, Color.red(colorTo), Color.green(colorTo), Color.blue(colorTo));

            // Show Animation
            ValueAnimator mShowToolbarAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            mShowToolbarAnimation.addUpdateListener(animator -> mMainToolbar.setBackgroundColor((Integer) animator.getAnimatedValue()));
            mShowToolbarAnimation.setDuration(SHOW_DURATION);

            // Fade Animation
            mFadeToolbarAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorTo, colorFrom);
            mFadeToolbarAnimation.addUpdateListener(animator -> mMainToolbar.setBackgroundColor((Integer) animator.getAnimatedValue()));
            mFadeToolbarAnimation.setDuration(FADE_DURATION);

            final float ICON_TEXT_ORIGINAL_POS = mIconText.getTranslationX();
            ValueAnimator mIconTextToOriginalPos = ValueAnimator.ofObject(new FloatEvaluator(), -175, ICON_TEXT_ORIGINAL_POS);
            mIconTextToOriginalPos.addUpdateListener(animation -> mIconText.setTranslationX((Float) animation.getAnimatedValue()));
            mIconTextToOriginalPos.setDuration(1000);


            // Animations for showing and fading the shadow of the toolbar.
            // The animation changes the alpha value of the view.
            // Be sure to use floats and the hard values 0-255 doesn't work as intended
            mShowShadowAnimation = new AlphaAnimation(0.0f, 1.0f);
            mShowShadowAnimation.setDuration(SHOW_DURATION);
            mShowShadowAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mToolbarShadow.setAlpha(1f);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            mFadeShadowAnimation = new AlphaAnimation(1.0f, 0.0f);
            mFadeShadowAnimation.setDuration(SHADOW_FADE_DURATION);
            mFadeShadowAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mToolbarShadow.setAlpha(0f);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
    }
}
