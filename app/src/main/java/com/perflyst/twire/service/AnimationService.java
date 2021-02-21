package com.perflyst.twire.service;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.perflyst.twire.R;
import com.perflyst.twire.adapters.StreamsAdapter;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

/**
 * Created by sebof on 14-10-2015.
 */
public class AnimationService {
    private static final int ANIMATE_TOOLBAR_POSITION_DURATION = 700;

    /*
        Animations for starting the initial activity when the app is first started
     */
    public static void setActivityIconRevealAnimation(final View aIconView, final TextView aIconTextView) {
        final int ICON_VIEW_DURATION = 700;
        final int ICON_TEXT_DURATION = 300;

        // Define the translation animation
        TranslateAnimation translationAnimation = new TranslateAnimation(0, 0, -1 * 700, 0); //ToDo: Find another way to measure distance
        translationAnimation.setDuration(ICON_VIEW_DURATION);

        // Define the alpha animation
        AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
        alphaAnimation.setDuration(ICON_VIEW_DURATION);

        // Combine the animations in a set
        final AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(translationAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animationSet.setFillAfter(true);
        animationSet.setFillBefore(true);
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                aIconTextView.setVisibility(View.VISIBLE);
                aIconView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
                alphaAnimation.setDuration(ICON_TEXT_DURATION);
                alphaAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

                aIconTextView.setAlpha(1f);
                aIconTextView.startAnimation(alphaAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        aIconView.startAnimation(animationSet);
    }

    public static void setActivityToolbarCircularRevealAnimation(final Toolbar aDecorativeToolbar) {
        aDecorativeToolbar.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);

                int CIRCULAR_REVEAL_DURATION = 700;
                int cx = (aDecorativeToolbar.getLeft() + aDecorativeToolbar.getRight()) / 2;
                int cy = 0;

                // get the final radius for the clipping circle
                int finalRadius = Math.max(aDecorativeToolbar.getWidth(), aDecorativeToolbar.getHeight());

                SupportAnimator animator = ViewAnimationUtils.createCircularReveal(aDecorativeToolbar, cx, cy, 0, finalRadius);

                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.setDuration(CIRCULAR_REVEAL_DURATION);
                animator.start();
            }
        });
    }

    public static void setActivityToolbarReset(Toolbar aMainToolbar, Toolbar aDecorativeToolbar, Activity aActivity, float fromToolbarPosition, float fromMainToolbarPosition) {
        final int TOOLBAR_TRANSLATION_DURATION = 700;
        float DECORATIVE_TOOLBAR_HEIGHT = -1 * aActivity.getResources().getDimension(R.dimen.additional_toolbar_height);
        if (fromMainToolbarPosition == 0) {
            DECORATIVE_TOOLBAR_HEIGHT += aActivity.getResources().getDimension(R.dimen.main_toolbar_height);
        } else {
            Animation moveMainToolbarAnimation = new TranslateAnimation(0, 0, fromMainToolbarPosition, 0);
            moveMainToolbarAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            moveMainToolbarAnimation.setDuration(TOOLBAR_TRANSLATION_DURATION);

            aMainToolbar.startAnimation(moveMainToolbarAnimation);
        }
        float fromTranslationY = Math.max(fromToolbarPosition, DECORATIVE_TOOLBAR_HEIGHT);

        Animation moveToolbarAnimation = new TranslateAnimation(0, 0, fromTranslationY, 0);
        moveToolbarAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        moveToolbarAnimation.setDuration(TOOLBAR_TRANSLATION_DURATION);

        aDecorativeToolbar.startAnimation(moveToolbarAnimation);
    }

    public static void setActivityToolbarPosition(int duration, Toolbar aMainToolbar, Toolbar aDecorativeToolbar, Activity aActivity, float fromToolbarPosition, float toToolbarPosition, float fromMainToolbarPosition, float toMainToolbarPosition) {
        duration = Math.max(duration, 0);

        float distanceToMoveY = toToolbarPosition - fromToolbarPosition;
        float DECORATIVE_TOOLBAR_HEIGHT = -1 * aActivity.getResources().getDimension(R.dimen.additional_toolbar_height);
        float toTranslationY = Math.max(distanceToMoveY, DECORATIVE_TOOLBAR_HEIGHT);

        // We want to make sure the toolbar is as close to the final position as possible without being visible.
        // This ensures that the animation is only running when the toolbar is visible to the user.
        if (aDecorativeToolbar.getY() < DECORATIVE_TOOLBAR_HEIGHT) {
            aDecorativeToolbar.setY(DECORATIVE_TOOLBAR_HEIGHT);
            toTranslationY = (DECORATIVE_TOOLBAR_HEIGHT - toToolbarPosition) * -1;
        }

        Animation moveToolbarAnimation = new TranslateAnimation(0, 0, 0, toTranslationY);
        moveToolbarAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        moveToolbarAnimation.setDuration(duration);
        moveToolbarAnimation.setFillAfter(true);

        float toDeltaY = toMainToolbarPosition - fromMainToolbarPosition;
        Animation moveMainToolbarAnimation = new TranslateAnimation(0, 0, 0, toDeltaY);
        moveMainToolbarAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        moveMainToolbarAnimation.setDuration(duration);
        moveMainToolbarAnimation.setFillAfter(true);

        aMainToolbar.setBackgroundColor(Service.getColorAttribute(R.attr.colorPrimary, R.color.primary, aActivity));
        aMainToolbar.startAnimation(moveMainToolbarAnimation);
        aDecorativeToolbar.startAnimation(moveToolbarAnimation);
    }

    /**
     * Hides every view in a RecyclerView to simulate clearing the RecyclerView
     * returns how long the animation takes to complete.
     * Returns -1 if there is nothing to animate
     */
    public static int animateFakeClearing(int lastVisibleItemPosition, int firstVisibleItemPosition, AutoSpanRecyclerView aRecyclerView, Animation.AnimationListener animationListener, boolean includeTranslation) {
        final int DELAY_BETWEEN = 50;
        int clearingDuration = 0;

        int startPositionCol = getColumnPosFromIndex(firstVisibleItemPosition, aRecyclerView);
        int startPositionRow = getRowPosFromIndex(firstVisibleItemPosition, aRecyclerView);

        for (int i = lastVisibleItemPosition; i >= firstVisibleItemPosition; i--) {
            final View VIEW = aRecyclerView.getChildAt(lastVisibleItemPosition - i);

            int positionColumnDistance = Math.abs(getColumnPosFromIndex(i, aRecyclerView) - startPositionCol);
            int positionRowDistance = Math.abs(getRowPosFromIndex(i, aRecyclerView) - startPositionRow);
            int delay = (positionColumnDistance + positionRowDistance) * DELAY_BETWEEN + 1;

            AnimationSet mHideAnimations = AnimationService.startAlphaHideAnimation(delay, VIEW, includeTranslation);
            if (mHideAnimations == null) {
                return -1;
            }
            int hideAnimationDuration = (int) mHideAnimations.getDuration();

            if (hideAnimationDuration + delay > clearingDuration) {
                clearingDuration = hideAnimationDuration + delay;
            }

            // If the view is the last to animate, then start the intent when the animation finishes.
            if (i == lastVisibleItemPosition && animationListener != null) {
                mHideAnimations.setAnimationListener(animationListener);
            }
        }

        return clearingDuration;
    }

    /**
     * Get the Column position for a view's index in a recyclerview
     *
     * @param indexPosition The position of the view you want to know the column position of
     * @param recyclerView  The recyclerview constraining the view
     * @return The Column position
     */
    public static int getColumnPosFromIndex(int indexPosition, AutoSpanRecyclerView recyclerView) {
        return indexPosition % recyclerView.getSpanCount();
    }

    /**
     * Get the Row position for a view's index in a recyclerview
     *
     * @param indexPosition The position of the view
     * @param recyclerView  The recyclerview that contains the view
     * @return The row position
     */
    public static int getRowPosFromIndex(int indexPosition, AutoSpanRecyclerView recyclerView) {
        return (int) (Math.ceil((indexPosition + 1.0f) / recyclerView.getSpanCount()) - 1);
    }

    public static void setAdapterInsertAnimation(final View aCard, int row, int height) {
        final int ANIMATION_DURATION = 650;
        final int BASE_DELAY = 50;

        TranslateAnimation translationAnimation = new TranslateAnimation(0, 0, height, 0);

        AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);

        final AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(translationAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animationSet.setFillAfter(true);
        animationSet.setFillBefore(true);
        animationSet.setDuration(ANIMATION_DURATION + row * BASE_DELAY);

        aCard.setAnimation(animationSet);
    }

    public static void setAdapterRemoveStreamAnimation(final View aCard, int delay, final StreamsAdapter adapter, final StreamInfo streamInfo, final boolean isActualClear) {
        final int ANIMATION_DURATION = 700;

        TranslateAnimation translationAnimation = new TranslateAnimation(0, 1000, 0, 0); // ToDo: Find another way to get distance
        translationAnimation.setDuration(ANIMATION_DURATION);

        AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
        alphaAnimation.setDuration(ANIMATION_DURATION);

        final AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(translationAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animationSet.setFillAfter(true);
        animationSet.setFillBefore(false);
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isActualClear) {
                    adapter.clearNoAnimation();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        new Handler().postDelayed(() -> {
            if (aCard != null)
                aCard.startAnimation(animationSet);
        }, delay);
    }

    /*
     * Animations for fake clearing of a view in a recyclerview when transitioning to or back another MainActivity
     */

    /**
     * For the Card Views
     */
    public static void startAlphaRevealAnimation(int delay, final View VIEW, boolean includeTransition) {
        final int ANIMATION_DURATION = 300;

        final Animation mAlphaAnimation = new AlphaAnimation(0f, 1f);
        mAlphaAnimation.setDuration(ANIMATION_DURATION);
        mAlphaAnimation.setFillAfter(true);

        final AnimationSet mRevealAnimations = new AnimationSet(true);
        mRevealAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
        mRevealAnimations.addAnimation(mAlphaAnimation);
        mRevealAnimations.setFillAfter(true);

        if (includeTransition) {
            final Animation mTransitionAnimation = new TranslateAnimation(0, 0, VIEW.getHeight() / 2.0f, 0);
            mTransitionAnimation.setDuration(ANIMATION_DURATION);
            mTransitionAnimation.setFillAfter(false);

            mRevealAnimations.addAnimation(mTransitionAnimation);
        }

        new Handler().postDelayed(() -> {
            if (VIEW != null)
                VIEW.startAnimation(mRevealAnimations);
        }, delay);

    }

    private static AnimationSet startAlphaHideAnimation(final int DELAY, final View VIEW, boolean includeTransition) {
        final int ANIMATION_DURATION = 300;
        if (VIEW == null)
            return null;

        final Animation mAlphaAnimation = new AlphaAnimation(1f, 0f);
        mAlphaAnimation.setDuration(ANIMATION_DURATION);
        mAlphaAnimation.setFillAfter(true);

        final AnimationSet mHideAnimations = new AnimationSet(true);
        mHideAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
        mHideAnimations.setFillAfter(true);
        mHideAnimations.addAnimation(mAlphaAnimation);

        if (includeTransition) {
            final Animation mTransitionAnimation = new TranslateAnimation(0, 0, 0, VIEW.getHeight() / 2.0f);
            mTransitionAnimation.setDuration(ANIMATION_DURATION);
            mTransitionAnimation.setFillAfter(false);

            mHideAnimations.addAnimation(mTransitionAnimation);
        }

        new Handler().postDelayed(() -> VIEW.startAnimation(mHideAnimations), DELAY);

        return mHideAnimations;
    }

    /**
     * For the Activity Circle Icon and text
     */

    public static void startAlphaRevealAnimation(final View VIEW) {
        final int ANIMATION_DURATION = 1000;

        final Animation mShowViewAnimation = new AlphaAnimation(0f, 1f);
        mShowViewAnimation.setDuration(ANIMATION_DURATION);

        AnimationSet mAnimations = new AnimationSet(true);
        mAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimations.addAnimation(mShowViewAnimation);
        mAnimations.setFillAfter(true);

        if (VIEW != null)
            VIEW.startAnimation(mAnimations);

    }

    public static AnimationSet startAlphaHideAnimation(final View VIEW) {
        final int ANIMATION_DURATION = 350;

        final Animation mHideViewAnimation = new AlphaAnimation(1f, 0f);

        AnimationSet mAnimations = new AnimationSet(true);
        mAnimations.setDuration(ANIMATION_DURATION);
        mAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimations.addAnimation(mHideViewAnimation);
        mAnimations.setFillAfter(true);

        if (VIEW != null)
            VIEW.startAnimation(mAnimations);

        return mAnimations;
    }
}
