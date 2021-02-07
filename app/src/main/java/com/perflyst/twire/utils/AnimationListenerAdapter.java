package com.perflyst.twire.utils;

import android.animation.AnimatorListenerAdapter;
import android.view.animation.Animation;

/**
 * This class is for providing a middleman between anything that uses
 * Animation.AnimationListener and the Animation.AnimationListener itself.
 * It removes the need for redundant overrides and allows us to use fewer lines.
 * <p>
 * Basically it just does what {@link AnimatorListenerAdapter} does but for {@link Animation.AnimationListener} instead.
 */
public abstract class AnimationListenerAdapter implements Animation.AnimationListener {
    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }
}
