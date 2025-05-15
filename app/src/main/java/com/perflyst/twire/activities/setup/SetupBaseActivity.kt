package com.perflyst.twire.activities.setup

import android.os.Handler
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.perflyst.twire.R
import com.perflyst.twire.utils.AnimationListenerAdapter

open class SetupBaseActivity : AppCompatActivity() {
    val showContinueIconDuration: Int = 650

    protected fun hideViewAnimation(view: View, duration: Int): AnimationSet {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val mScaleAnimation: Animation = ScaleAnimation(
            1f, 0f, 1f, 0f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        )
        mScaleAnimation.setDuration(duration.toLong())
        mScaleAnimation.interpolator = OvershootInterpolator(0.7f)

        val mAlphaAnimation: Animation = AlphaAnimation(1f, 0f)
        mAlphaAnimation.setDuration((duration / 2).toLong())
        mAlphaAnimation.interpolator = DecelerateInterpolator()
        mAlphaAnimation.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                view.visibility = View.INVISIBLE
            }
        })

        val mViewAnimations = AnimationSet(false)
        mViewAnimations.interpolator = AccelerateDecelerateInterpolator()
        mViewAnimations.setFillBefore(true)
        mViewAnimations.setFillAfter(true)
        mViewAnimations.addAnimation(mScaleAnimation)
        mViewAnimations.addAnimation(mAlphaAnimation)
        mViewAnimations.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                view.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        })

        view.startAnimation(mViewAnimations)
        return mViewAnimations
    }

    protected fun showTextLineAnimations(mTextLine: TextView, lineNumber: Int) {
        mTextLine.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val travelDistance = if (lineNumber < 3) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            getResources().getDimension(R.dimen.welcome_text_line_three_size),
            getResources().displayMetrics
        ).toInt() else
            0

        val overshoot = if (lineNumber == 1) 2f else 1f
        val mTranslationAnimation: Animation =
            TranslateAnimation(0f, 0f, travelDistance.toFloat(), 0f)
        mTranslationAnimation.interpolator = OvershootInterpolator(overshoot)

        val mAlphaAnimation: Animation = AlphaAnimation(0f, 1f)
        mAlphaAnimation.interpolator = DecelerateInterpolator()
        mAlphaAnimation.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationStart(animation: Animation?) {
                mTextLine.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {
                mTextLine.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        })

        val mWelcomeTextAnimations = AnimationSet(false)
        val showTextAnimationDuration = 600
        mWelcomeTextAnimations.setDuration(showTextAnimationDuration.toLong())
        mWelcomeTextAnimations.interpolator = AccelerateDecelerateInterpolator()
        mWelcomeTextAnimations.setFillBefore(true)
        mWelcomeTextAnimations.setFillAfter(true)
        mWelcomeTextAnimations.addAnimation(mAlphaAnimation)
        mWelcomeTextAnimations.addAnimation(mTranslationAnimation)

        val showTextAnimationBaseDelay = 105
        val delay =
            showTextAnimationBaseDelay * (if (lineNumber < 3) lineNumber else lineNumber * 2)
        val showTextAnimationDelay = 105
        Handler().postDelayed(
            { mTextLine.startAnimation(mWelcomeTextAnimations) },
            (delay + showTextAnimationDelay).toLong()
        )
    }

    protected fun showLogoAnimations(mGearIcon: ImageView) {
        mGearIcon.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val mScaleAnimation: Animation = ScaleAnimation(
            0f,
            1f,
            0f,
            1f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        val showLogoAnimationDuration = 600
        mScaleAnimation.setDuration(showLogoAnimationDuration.toLong())
        mScaleAnimation.interpolator = OvershootInterpolator(0.7f)

        val mAlphaAnimation: Animation = AlphaAnimation(0f, 1f)
        mAlphaAnimation.setDuration(showLogoAnimationDuration.toLong())
        mAlphaAnimation.interpolator = DecelerateInterpolator()
        mAlphaAnimation.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationStart(animation: Animation?) {
                mGearIcon.setVisibility(View.VISIBLE)
            }
        })

        val mRotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        mRotateAnimation.interpolator = LinearInterpolator()
        // The time it takes for the icon to rotate 360 degrees. The Higher the slower.
        val logoRotationSpeed = 15 * 1000
        mRotateAnimation.setDuration(logoRotationSpeed.toLong())
        mRotateAnimation.setRepeatCount(Animation.INFINITE)

        val mLogoAnimations = AnimationSet(false)
        mLogoAnimations.interpolator = AccelerateDecelerateInterpolator()
        mLogoAnimations.setFillBefore(true)
        mLogoAnimations.setFillAfter(true)
        mLogoAnimations.addAnimation(mScaleAnimation)
        mLogoAnimations.addAnimation(mRotateAnimation)
        mLogoAnimations.addAnimation(mAlphaAnimation)
        mLogoAnimations.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                mGearIcon.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        })

        mGearIcon.startAnimation(mLogoAnimations)
    }

    protected fun hideContinueIconAnimations(mContinueIcon: ImageView) {
        val mScaleAnimation: Animation = ScaleAnimation(
            1f,
            0f,
            1f,
            0f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        val mRotateAnimation: Animation = RotateAnimation(
            mContinueIcon.rotation, 360 - mContinueIcon.rotation,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        mRotateAnimation.setRepeatCount(0)
        mRotateAnimation.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                mContinueIcon.setVisibility(View.INVISIBLE)
            }
        })
        val mAnimations = AnimationSet(true)
        mAnimations.setDuration(showContinueIconDuration.toLong())
        mAnimations.setFillAfter(true)
        mAnimations.interpolator = OvershootInterpolator(1.5f)
        mAnimations.addAnimation(mScaleAnimation)
        mAnimations.addAnimation(mRotateAnimation)

        mContinueIcon.startAnimation(mAnimations)
    }
}
