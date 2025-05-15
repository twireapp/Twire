package com.perflyst.twire.service

import android.app.Activity
import android.os.Handler
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.perflyst.twire.R
import com.perflyst.twire.adapters.StreamsAdapter
import com.perflyst.twire.model.StreamInfo
import com.perflyst.twire.utils.AnimationListenerAdapter
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import io.codetail.animation.ViewAnimationUtils
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Created by sebof on 14-10-2015.
 */
object AnimationService {
    private const val ANIMATE_TOOLBAR_POSITION_DURATION = 700

    /*
        Animations for starting the initial activity when the app is first started
     */
    fun setActivityIconRevealAnimation(aIconView: View, aIconTextView: TextView) {
        val ICON_VIEW_DURATION = 700
        val ICON_TEXT_DURATION = 300

        // Define the translation animation
        val translationAnimation = TranslateAnimation(
            0f,
            0f,
            (-1 * 700).toFloat(),
            0f
        ) //ToDo: Find another way to measure distance
        translationAnimation.setDuration(ICON_VIEW_DURATION.toLong())

        // Define the alpha animation
        val alphaAnimation = AlphaAnimation(0f, 1f)
        alphaAnimation.setDuration(ICON_VIEW_DURATION.toLong())

        // Combine the animations in a set
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(translationAnimation)
        animationSet.addAnimation(alphaAnimation)
        animationSet.interpolator = AccelerateDecelerateInterpolator()
        animationSet.setFillAfter(true)
        animationSet.setFillBefore(true)
        animationSet.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationStart(animation: Animation?) {
                aIconTextView.visibility = View.VISIBLE
                aIconView.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {
                val alphaAnimation = AlphaAnimation(0f, 1f)
                alphaAnimation.setDuration(ICON_TEXT_DURATION.toLong())
                alphaAnimation.interpolator = AccelerateDecelerateInterpolator()

                aIconTextView.setAlpha(1f)
                aIconTextView.startAnimation(alphaAnimation)
            }
        })

        aIconView.startAnimation(animationSet)
    }

    fun setActivityToolbarCircularRevealAnimation(aDecorativeToolbar: Toolbar) {
        aDecorativeToolbar.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                v.removeOnLayoutChangeListener(this)

                val CIRCULAR_REVEAL_DURATION = 700
                val cx = (aDecorativeToolbar.left + aDecorativeToolbar.right) / 2
                val cy = 0

                // get the final radius for the clipping circle
                val finalRadius = max(aDecorativeToolbar.width, aDecorativeToolbar.height)

                val animator = ViewAnimationUtils.createCircularReveal(
                    aDecorativeToolbar,
                    cx,
                    cy,
                    0f,
                    finalRadius.toFloat()
                )

                animator.interpolator = AccelerateDecelerateInterpolator()
                animator.duration = CIRCULAR_REVEAL_DURATION.toLong()
                animator.start()
            }
        })
    }

    fun setActivityToolbarReset(
        aMainToolbar: Toolbar,
        aDecorativeToolbar: Toolbar,
        aActivity: Activity,
        fromToolbarPosition: Float,
        fromMainToolbarPosition: Float
    ) {
        val TOOLBAR_TRANSLATION_DURATION = 700
        var DECORATIVE_TOOLBAR_HEIGHT =
            -1 * aActivity.resources.getDimension(R.dimen.additional_toolbar_height)
        if (fromMainToolbarPosition == 0f) {
            DECORATIVE_TOOLBAR_HEIGHT += aActivity.resources
                .getDimension(R.dimen.main_toolbar_height)
        } else {
            val moveMainToolbarAnimation: Animation =
                TranslateAnimation(0f, 0f, fromMainToolbarPosition, 0f)
            moveMainToolbarAnimation.interpolator = AccelerateDecelerateInterpolator()
            moveMainToolbarAnimation.setDuration(TOOLBAR_TRANSLATION_DURATION.toLong())

            aMainToolbar.startAnimation(moveMainToolbarAnimation)
        }
        val fromTranslationY = max(fromToolbarPosition, DECORATIVE_TOOLBAR_HEIGHT)

        val moveToolbarAnimation: Animation = TranslateAnimation(0f, 0f, fromTranslationY, 0f)
        moveToolbarAnimation.interpolator = AccelerateDecelerateInterpolator()
        moveToolbarAnimation.setDuration(TOOLBAR_TRANSLATION_DURATION.toLong())

        aDecorativeToolbar.startAnimation(moveToolbarAnimation)
    }

    fun setActivityToolbarPosition(
        duration: Int,
        aMainToolbar: Toolbar,
        aDecorativeToolbar: Toolbar,
        aActivity: Activity,
        fromToolbarPosition: Float,
        toToolbarPosition: Float,
        fromMainToolbarPosition: Float,
        toMainToolbarPosition: Float
    ) {
        var duration = duration
        duration = max(duration, 0)

        val distanceToMoveY = toToolbarPosition - fromToolbarPosition
        val DECORATIVE_TOOLBAR_HEIGHT =
            -1 * aActivity.resources.getDimension(R.dimen.additional_toolbar_height)
        var toTranslationY = max(distanceToMoveY, DECORATIVE_TOOLBAR_HEIGHT)

        // We want to make sure the toolbar is as close to the final position as possible without being visible.
        // This ensures that the animation is only running when the toolbar is visible to the user.
        if (aDecorativeToolbar.y < DECORATIVE_TOOLBAR_HEIGHT) {
            aDecorativeToolbar.y = DECORATIVE_TOOLBAR_HEIGHT
            toTranslationY = (DECORATIVE_TOOLBAR_HEIGHT - toToolbarPosition) * -1
        }

        val moveToolbarAnimation: Animation = TranslateAnimation(0f, 0f, 0f, toTranslationY)
        moveToolbarAnimation.interpolator = AccelerateDecelerateInterpolator()
        moveToolbarAnimation.setDuration(duration.toLong())
        moveToolbarAnimation.fillAfter = true

        val toDeltaY = toMainToolbarPosition - fromMainToolbarPosition
        val moveMainToolbarAnimation: Animation = TranslateAnimation(0f, 0f, 0f, toDeltaY)
        moveMainToolbarAnimation.interpolator = AccelerateDecelerateInterpolator()
        moveMainToolbarAnimation.setDuration(duration.toLong())
        moveMainToolbarAnimation.fillAfter = true

        aMainToolbar.setBackgroundColor(
            Service.getColorAttribute(
                androidx.appcompat.R.attr.colorPrimary,
                R.color.primary,
                aActivity
            )
        )
        aMainToolbar.startAnimation(moveMainToolbarAnimation)
        aDecorativeToolbar.startAnimation(moveToolbarAnimation)
    }

    /**
     * Hides every view in a RecyclerView to simulate clearing the RecyclerView
     * returns how long the animation takes to complete.
     * Returns -1 if there is nothing to animate
     */
    fun animateFakeClearing(
        lastVisibleItemPosition: Int,
        firstVisibleItemPosition: Int,
        aRecyclerView: AutoSpanRecyclerView,
        animationListener: Animation.AnimationListener?,
        includeTranslation: Boolean
    ): Int {
        val DELAY_BETWEEN = 50
        var clearingDuration = 0

        val startPositionCol = getColumnPosFromIndex(firstVisibleItemPosition, aRecyclerView)
        val startPositionRow = getRowPosFromIndex(firstVisibleItemPosition, aRecyclerView)

        for (i in lastVisibleItemPosition downTo firstVisibleItemPosition) {
            val VIEW = aRecyclerView.getChildAt(lastVisibleItemPosition - i)

            val positionColumnDistance =
                abs(getColumnPosFromIndex(i, aRecyclerView) - startPositionCol)
            val positionRowDistance = abs(getRowPosFromIndex(i, aRecyclerView) - startPositionRow)
            val delay = (positionColumnDistance + positionRowDistance) * DELAY_BETWEEN + 1

            val mHideAnimations = startAlphaHideAnimation(delay, VIEW, includeTranslation)
            if (mHideAnimations == null) {
                return -1
            }
            val hideAnimationDuration = mHideAnimations.getDuration().toInt()

            if (hideAnimationDuration + delay > clearingDuration) {
                clearingDuration = hideAnimationDuration + delay
            }

            // If the view is the last to animate, then start the intent when the animation finishes.
            if (i == lastVisibleItemPosition && animationListener != null) {
                mHideAnimations.setAnimationListener(animationListener)
            }
        }

        return clearingDuration
    }

    /**
     * Get the Column position for a view's index in a recyclerview
     *
     * @param indexPosition The position of the view you want to know the column position of
     * @param recyclerView  The recyclerview constraining the view
     * @return The Column position
     */
    fun getColumnPosFromIndex(indexPosition: Int, recyclerView: AutoSpanRecyclerView): Int {
        return indexPosition % recyclerView.spanCount
    }

    /**
     * Get the Row position for a view's index in a recyclerview
     *
     * @param indexPosition The position of the view
     * @param recyclerView  The recyclerview that contains the view
     * @return The row position
     */
    fun getRowPosFromIndex(indexPosition: Int, recyclerView: AutoSpanRecyclerView): Int {
        return (ceil(((indexPosition + 1.0f) / recyclerView.spanCount).toDouble()) - 1).toInt()
    }

    fun setAdapterInsertAnimation(aCard: View, row: Int, height: Int) {
        val ANIMATION_DURATION = 650
        val BASE_DELAY = 50

        val translationAnimation = TranslateAnimation(0f, 0f, height.toFloat(), 0f)

        val alphaAnimation = AlphaAnimation(0f, 1f)

        val animationSet = AnimationSet(true)
        animationSet.addAnimation(translationAnimation)
        animationSet.addAnimation(alphaAnimation)
        animationSet.interpolator = AccelerateDecelerateInterpolator()
        animationSet.setFillAfter(true)
        animationSet.setFillBefore(true)
        animationSet.setDuration(ANIMATION_DURATION + row.toLong() * BASE_DELAY)

        aCard.setAnimation(animationSet)
    }

    fun setAdapterRemoveStreamAnimation(
        aCard: View?,
        delay: Int,
        adapter: StreamsAdapter,
        streamInfo: StreamInfo?,
        isActualClear: Boolean
    ) {
        val ANIMATION_DURATION = 700

        val translationAnimation =
            TranslateAnimation(0f, 1000f, 0f, 0f) // ToDo: Find another way to get distance
        translationAnimation.setDuration(ANIMATION_DURATION.toLong())

        val alphaAnimation = AlphaAnimation(0f, 1f)
        alphaAnimation.setDuration(ANIMATION_DURATION.toLong())

        val animationSet = AnimationSet(true)
        animationSet.addAnimation(translationAnimation)
        animationSet.addAnimation(alphaAnimation)
        animationSet.interpolator = AccelerateDecelerateInterpolator()
        animationSet.setFillAfter(true)
        animationSet.setFillBefore(false)
        animationSet.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                if (isActualClear) {
                    adapter.clearNoAnimation()
                }
            }
        })


        Handler().postDelayed({
            aCard?.startAnimation(animationSet)
        }, delay.toLong())
    }

    /*
     * Animations for fake clearing of a view in a recyclerview when transitioning to or back another MainActivity
     */
    /**
     * For the Card Views
     */
    fun startAlphaRevealAnimation(delay: Int, VIEW: View?, includeTransition: Boolean) {
        val ANIMATION_DURATION = 300

        val mAlphaAnimation: Animation = AlphaAnimation(0f, 1f)
        mAlphaAnimation.setDuration(ANIMATION_DURATION.toLong())
        mAlphaAnimation.fillAfter = true

        val mRevealAnimations = AnimationSet(true)
        mRevealAnimations.interpolator = AccelerateDecelerateInterpolator()
        mRevealAnimations.addAnimation(mAlphaAnimation)
        mRevealAnimations.setFillAfter(true)

        if (includeTransition) {
            val mTransitionAnimation: Animation =
                TranslateAnimation(0f, 0f, VIEW!!.height / 2.0f, 0f)
            mTransitionAnimation.setDuration(ANIMATION_DURATION.toLong())
            mTransitionAnimation.fillAfter = false

            mRevealAnimations.addAnimation(mTransitionAnimation)
        }

        Handler().postDelayed({
            VIEW?.startAnimation(mRevealAnimations)
        }, delay.toLong())
    }

    private fun startAlphaHideAnimation(
        DELAY: Int,
        VIEW: View?,
        includeTransition: Boolean
    ): AnimationSet? {
        val ANIMATION_DURATION = 300
        if (VIEW == null) return null

        val mAlphaAnimation: Animation = AlphaAnimation(1f, 0f)
        mAlphaAnimation.setDuration(ANIMATION_DURATION.toLong())
        mAlphaAnimation.fillAfter = true

        val mHideAnimations = AnimationSet(true)
        mHideAnimations.interpolator = AccelerateDecelerateInterpolator()
        mHideAnimations.setFillAfter(true)
        mHideAnimations.addAnimation(mAlphaAnimation)

        if (includeTransition) {
            val mTransitionAnimation: Animation =
                TranslateAnimation(0f, 0f, 0f, VIEW.height / 2.0f)
            mTransitionAnimation.setDuration(ANIMATION_DURATION.toLong())
            mTransitionAnimation.fillAfter = false

            mHideAnimations.addAnimation(mTransitionAnimation)
        }

        Handler().postDelayed({ VIEW.startAnimation(mHideAnimations) }, DELAY.toLong())

        return mHideAnimations
    }

    /**
     * For the Activity Circle Icon and text
     */
    fun startAlphaRevealAnimation(VIEW: View?) {
        val ANIMATION_DURATION = 1000

        val mShowViewAnimation: Animation = AlphaAnimation(0f, 1f)
        mShowViewAnimation.setDuration(ANIMATION_DURATION.toLong())

        val mAnimations = AnimationSet(true)
        mAnimations.interpolator = AccelerateDecelerateInterpolator()
        mAnimations.addAnimation(mShowViewAnimation)
        mAnimations.setFillAfter(true)

        VIEW?.startAnimation(mAnimations)
    }

    fun startAlphaHideAnimation(VIEW: View?): AnimationSet {
        val ANIMATION_DURATION = 350

        val mHideViewAnimation: Animation = AlphaAnimation(1f, 0f)

        val mAnimations = AnimationSet(true)
        mAnimations.setDuration(ANIMATION_DURATION.toLong())
        mAnimations.interpolator = AccelerateDecelerateInterpolator()
        mAnimations.addAnimation(mHideViewAnimation)
        mAnimations.setFillAfter(true)

        if (VIEW != null) VIEW.startAnimation(mAnimations)

        return mAnimations
    }
}
