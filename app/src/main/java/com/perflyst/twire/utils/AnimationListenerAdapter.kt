package com.perflyst.twire.utils

import android.view.animation.Animation

/**
 * This class is for providing a middleman between anything that uses
 * Animation.AnimationListener and the Animation.AnimationListener itself.
 * It removes the need for redundant overrides and allows us to use fewer lines.
 *
 *
 * Basically it just does what [android.animation.AnimatorListenerAdapter] does but for [Animation.AnimationListener] instead.
 */
abstract class AnimationListenerAdapter : Animation.AnimationListener {
    override fun onAnimationStart(animation: Animation?) {
    }

    override fun onAnimationEnd(animation: Animation?) {
    }

    override fun onAnimationRepeat(animation: Animation?) {
    }
}
