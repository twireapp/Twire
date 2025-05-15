package com.perflyst.twire.misc

import android.animation.ArgbEvaluator
import android.animation.FloatEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.perflyst.twire.R
import com.perflyst.twire.service.Service
import com.perflyst.twire.utils.AnimationListenerAdapter
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import timber.log.Timber

/**
 * Created by Sebastian on 06-08-2015.
 */
open class UniversalOnScrollListener(
    private val mActivity: AppCompatActivity?,
    private val mMainToolbar: Toolbar?,
    private val mDecorativeToolbar: Toolbar?,
    private val mToolbarShadow: View?,
    private val mIconCircle: View?,
    private val mIconText: TextView?,
    private val isMainActivity: Boolean
) : RecyclerView.OnScrollListener() {
    // variables to control main toolbar shadow
    private var minAmountToScroll = 0f
    private var minAmountToolbarHide = 0f
    var amountScrolled: Int = 0
        private set
    private var isToolbarTransparent = true
    private var mFadeToolbarAnimation: ValueAnimator? = null

    private var mShowShadowAnimation: Animation? = null
    private var mFadeShadowAnimation: Animation? = null

    init {
        if (isMainActivity) {
            this.minAmountToScroll = mActivity!!.baseContext.resources
                .getDimension(R.dimen.stream_card_first_top_margin) - mActivity.baseContext
                .resources.getDimension(R.dimen.main_toolbar_height)
            this.minAmountToolbarHide = mActivity.baseContext.resources
                .getDimension(R.dimen.stream_card_first_top_margin)
        }

        this.initAnimations()
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (isMainActivity) {
            val toolbarTranslationY =
                mMainToolbar!!.translationY // Save the Y value to a variable to prevent "getting" it multiple times
            var TOOLBAR_HEIGHT =
                mActivity!!.getResources().getDimension(R.dimen.main_toolbar_height).toInt()
            if (mActivity.supportActionBar != null) {
                TOOLBAR_HEIGHT = mActivity.supportActionBar!!.height
            }

            // If the toolbar is not fully drawn when the scrolling state is idle - animate to be either fully shown or fully hidden.
            // But don't run the animation if the amountScrolled amount is less that the toolbar height
            // Also animate the shadow the length and direction
            if (amountScrolled > mActivity.getResources()
                    .getDimension(R.dimen.additional_toolbar_height)
            ) {
                if (newState == 0 && toolbarTranslationY > -TOOLBAR_HEIGHT && toolbarTranslationY < -(TOOLBAR_HEIGHT / 2.0f) && amountScrolled > TOOLBAR_HEIGHT) {
                    // Hide animation
                    mToolbarShadow!!.animate().translationY(-TOOLBAR_HEIGHT.toFloat())
                        .setInterpolator(AccelerateInterpolator()).start()
                    mMainToolbar.animate().translationY(-TOOLBAR_HEIGHT.toFloat())
                        .setInterpolator(AccelerateInterpolator()).start()
                } else if (newState == 0 && toolbarTranslationY > -(TOOLBAR_HEIGHT / 2.0f) && toolbarTranslationY < 0 && amountScrolled > TOOLBAR_HEIGHT) {
                    // Show animation
                    mToolbarShadow!!.animate().translationY(0f)
                        .setInterpolator(AccelerateInterpolator()).start()
                    mMainToolbar.animate().translationY(0f)
                        .setInterpolator(AccelerateInterpolator()).start()
                }
            }
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (isMainActivity) {
            var TOOLBAR_HEIGHT =
                mActivity!!.getResources().getDimension(R.dimen.main_toolbar_height).toInt()
            if (mActivity.supportActionBar != null) {
                TOOLBAR_HEIGHT = mActivity.supportActionBar!!.height
            }

            val SLOW_SCROLL_RATE = 0.75
            //  Save the Y values to a variable to prevent "getting" it multiple times
            val decoToolbarTranslationY = mDecorativeToolbar!!.translationY
            val toolbarTranslationY = mMainToolbar!!.translationY
            val shadowViewTranslationY = mToolbarShadow!!.translationY
            val iconViewTranslationY = mIconCircle!!.translationY
            val iconTextViewTranslationY = mIconText!!.translationY

            // If the user scrolls up and the toolbar is currently showing
            // Subtract the scroll amount from the Y-position of the toolbar and shadow
            if (dy > 0) {
                amountScrolled += dy

                // Always Scroll the decorate toolbar slower than the main one.
                mDecorativeToolbar.translationY =
                    decoToolbarTranslationY - dy * SLOW_SCROLL_RATE.toFloat()
                mIconCircle.translationY = iconViewTranslationY - dy
                mIconText.translationY = iconTextViewTranslationY - dy

                if (amountScrolled < minAmountToScroll) {
                    // Update the transparency of the activity icon
                    if (mIconCircle.alpha >= 0) {
                        val newAlphaHex = 255 - mIconCircle.translationY * -1
                        val newAlphaFloat = newAlphaHex / 255
                        if (newAlphaFloat >= 0) {
                            mIconCircle.setAlpha(newAlphaFloat)
                        }
                    }
                } else {
                    // Scroll up the toolbar and the shadow
                    if (toolbarTranslationY > -TOOLBAR_HEIGHT) {
                        mMainToolbar.translationY = toolbarTranslationY - dy
                        mToolbarShadow.translationY = shadowViewTranslationY - dy
                    }
                    if (mIconCircle.alpha > 0) {
                        mIconCircle.setAlpha(0f)
                    }
                }
            }

            // If the user scrolls down
            if (dy < 0) {
                amountScrolled += dy

                // Always scroll for these Views. But Scroll the decorative toolbar slower than the others
                mDecorativeToolbar.translationY =
                    decoToolbarTranslationY - dy * SLOW_SCROLL_RATE.toFloat()
                mIconCircle.translationY = iconViewTranslationY - dy
                mIconText.translationY = iconTextViewTranslationY - dy

                if (amountScrolled < minAmountToolbarHide) {
                    if (!isToolbarTransparent) {
                        isToolbarTransparent = true

                        mFadeToolbarAnimation!!.start()
                        mToolbarShadow.startAnimation(mFadeShadowAnimation)
                    }
                } else if (amountScrolled > minAmountToolbarHide + mActivity.baseContext
                        .resources.getDimension(R.dimen.main_toolbar_height)
                ) {
                    if (isToolbarTransparent) {
                        // If the toolbar and its shadow is currently transparent/hidden,
                        // then show it but increasing Alpha on the shadow and settings the color of the toolbar

                        isToolbarTransparent = false

                        mMainToolbar.setBackgroundColor(
                            Service.getColorAttribute(
                                androidx.appcompat.R.attr.colorPrimary,
                                R.color.primary,
                                mActivity
                            )
                        )
                        mToolbarShadow.startAnimation(mShowShadowAnimation)
                    }
                }

                if (amountScrolled < minAmountToScroll) {
                    // If the toolbar and its shadow is not currently transparent hide them
                    // by animating the alpha value of the shadow the animating the color of the toolbar


                    if (mIconCircle.alpha >= 0) {
                        val newAlphaHex = 255 - mIconCircle.translationY * -1
                        val newAlphaFloat = newAlphaHex / 255
                        if (newAlphaFloat >= 0) {
                            mIconCircle.setAlpha(newAlphaFloat)
                        }
                    }

                    // Make sure the toolbar is half-way showed or hid if the user scrolls fast down while the toolbar is naturally getting hidden
                    mToolbarShadow.translationY = 0f
                    mMainToolbar.translationY = 0f
                } else {
                    // As long as the toolbar is not fully shown - Subtract the scroll amount from the Y-position
                    if (toolbarTranslationY < 0) {
                        // Make sure the toolbar can't get further down than intended
                        if (toolbarTranslationY - dy > 0) {
                            mMainToolbar.translationY = 0f
                        } else {
                            mMainToolbar.translationY = toolbarTranslationY - dy
                        }
                    }

                    // Do the same for the shadow
                    if (shadowViewTranslationY < 0) {
                        if (shadowViewTranslationY - dy > 0) mToolbarShadow.translationY = 0f
                        else mToolbarShadow.translationY = shadowViewTranslationY - dy
                    }
                }

                // Make sure that these Views don't get scrolled too far down if the user scrolls really fast
                if (decoToolbarTranslationY >= 0) mDecorativeToolbar.translationY = 0f

                if (iconViewTranslationY >= 0) mIconCircle.translationY = 0f

                if (iconTextViewTranslationY >= 0) mIconText.translationY = 0f
            }


            if (recyclerView is AutoSpanRecyclerView) {
                recyclerView.setScrolled(amountScrolled != 0)
            }

            if (amountScrolled < 0) {
                amountScrolled = 0
                Timber.w("RESETTING SCROLLED")
            }
        }
    }

    private fun initAnimations() {
        if (isMainActivity) {
            // Show and fade durations for the animations below.
            // We want the Toolbar and its shadow to show and hide in the same matter of time.
            val SHOW_DURATION = 200 // Millis
            val FADE_DURATION = 400 // Millis
            val SHADOW_FADE_DURATION = 200

            // Animations for showing and hiding the toolbar.
            // The animation changes the color of the toolbar from a transparent blue to the full blue.
            // Currently only the fade animation is being used.
            val colorTo = Service.getColorAttribute(
                androidx.appcompat.R.attr.colorPrimary,
                R.color.primary,
                mActivity!!
            )
            val colorFrom =
                Color.argb(0, Color.red(colorTo), Color.green(colorTo), Color.blue(colorTo))

            // Show Animation
            val mShowToolbarAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
            mShowToolbarAnimation.addUpdateListener { animator: ValueAnimator? ->
                mMainToolbar!!.setBackgroundColor(
                    (animator!!.getAnimatedValue() as Int?)!!
                )
            }
            mShowToolbarAnimation.setDuration(SHOW_DURATION.toLong())

            // Fade Animation
            mFadeToolbarAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorTo, colorFrom)
            mFadeToolbarAnimation!!.addUpdateListener { animator: ValueAnimator? ->
                mMainToolbar!!.setBackgroundColor(
                    (animator!!.getAnimatedValue() as Int?)!!
                )
            }
            mFadeToolbarAnimation!!.setDuration(FADE_DURATION.toLong())

            val ICON_TEXT_ORIGINAL_POS = mIconText!!.translationX
            val mIconTextToOriginalPos =
                ValueAnimator.ofObject(FloatEvaluator(), -175, ICON_TEXT_ORIGINAL_POS)
            mIconTextToOriginalPos.addUpdateListener { animation: ValueAnimator? ->
                mIconText.translationX = (animation!!.getAnimatedValue() as Float?)!!
            }
            mIconTextToOriginalPos.setDuration(1000)


            // Animations for showing and fading the shadow of the toolbar.
            // The animation changes the alpha value of the view.
            // Be sure to use floats and the hard values 0-255 doesn't work as intended
            mShowShadowAnimation = AlphaAnimation(0.0f, 1.0f)
            mShowShadowAnimation!!.setDuration(SHOW_DURATION.toLong())
            mShowShadowAnimation!!.setAnimationListener(object : AnimationListenerAdapter() {
                override fun onAnimationEnd(animation: Animation?) {
                    mToolbarShadow!!.setAlpha(1f)
                }
            })

            mFadeShadowAnimation = AlphaAnimation(1.0f, 0.0f)
            mFadeShadowAnimation!!.setDuration(SHADOW_FADE_DURATION.toLong())
            mFadeShadowAnimation!!.setAnimationListener(object : AnimationListenerAdapter() {
                override fun onAnimationEnd(animation: Animation?) {
                    mToolbarShadow!!.setAlpha(0f)
                }
            })
        }
    }
}
