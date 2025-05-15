package com.perflyst.twire.activities.setup

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.OvershootInterpolator
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.perflyst.twire.R
import com.perflyst.twire.service.Service
import com.perflyst.twire.utils.AnimationListenerAdapter
import com.perflyst.twire.utils.Execute
import com.rey.material.widget.ProgressView
import io.codetail.animation.SupportAnimator
import io.codetail.animation.ViewAnimationUtils
import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.math.hypot
import kotlin.math.max

class ConfirmSetupActivity : SetupBaseActivity() {
    private val revealAnimationDuration = 650
    private val revealAnimationDelay = 200
    var transitionAnimationWhite: SupportAnimator? = null
    private var hasTransitioned = false
    private lateinit var mGearIcon: ImageView
    private lateinit var mContinueIcon: ImageView
    private lateinit var mSetupProgress: ProgressView
    private lateinit var mLoginTextLineOne: TextView
    private lateinit var mLoginTextLineTwo: TextView
    private lateinit var mContinueFAB: View
    private lateinit var mContinueFABShadow: View
    private lateinit var mTransitionViewWhite: View
    private lateinit var mContinueFABContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_setup)

        val mLoginTextContainer = findViewById<RelativeLayout>(R.id.login_text_container)
        mSetupProgress = findViewById(R.id.SetupProgress)
        mContinueFABContainer = findViewById(R.id.login_continue_circle_container)
        mGearIcon = findViewById(R.id.login_icon)
        mContinueIcon = findViewById(R.id.forward_arrow)
        mLoginTextLineOne = findViewById(R.id.login_text_line_one)
        mLoginTextLineTwo = findViewById(R.id.login_text_line_two)
        mContinueFAB = findViewById(R.id.login_continue_circle)
        mContinueFABShadow = findViewById(R.id.login_continue_circle_shadow)
        mTransitionViewWhite = findViewById(R.id.transition_view_blue)

        mContinueIcon.setVisibility(View.INVISIBLE)
        mLoginTextLineOne.visibility = View.INVISIBLE
        mLoginTextLineTwo.visibility = View.INVISIBLE
        mGearIcon.setVisibility(View.INVISIBLE)
        mTransitionViewWhite.visibility = View.INVISIBLE

        val textPosition = (2.5 * (Service.getScreenHeight(baseContext) / 5)).toInt().toFloat()
        mLoginTextContainer.y = textPosition

        mContinueFABContainer.bringToFront()
        mLoginTextLineOne.bringToFront()
        mLoginTextLineTwo.bringToFront()
        Service.bringToBack(mTransitionViewWhite)

        showLogoAnimations(mGearIcon)
        showTextLineAnimations(mLoginTextLineOne, 1)
        showTextLineAnimations(mLoginTextLineTwo, 2)

        val checkingTask = CheckDataFetchingTask(this)
        Execute.background(checkingTask)
    }

    public override fun onResume() {
        super.onResume()
        if (transitionAnimationWhite != null && hasTransitioned) {
            showReverseTransitionAnimation()
            hasTransitioned = false
        }
    }

    override fun onBackPressed() {
        hideAllViews()!!.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                super@ConfirmSetupActivity.onBackPressed()
                // We don't want a transition when going back. The activities handle the animation themselves.
                overridePendingTransition(0, 0)
            }
        })
    }


    private fun navigateToNextActivity() {
        hasTransitioned = true
        this.startActivity(Service.getStartPageIntent(baseContext))
        this.overridePendingTransition(0, 0)
    }

    /**
     * Animations here from and down
     */
    private fun hideAllViews(): AnimationSet? {
        if (mContinueIcon.isVisible) {
            hideContinueIconAnimations(mContinueIcon)
        }
        val hideViewAnimationDuration = 550
        hideViewAnimation(mGearIcon, hideViewAnimationDuration)
        hideViewAnimation(mLoginTextLineOne, hideViewAnimationDuration)
        hideViewAnimation(mLoginTextLineTwo, hideViewAnimationDuration)
        hideViewAnimation(mSetupProgress, hideViewAnimationDuration)

        return hideViewAnimation(mLoginTextLineTwo, hideViewAnimationDuration)
    }

    private fun showTransitionAnimation(): SupportAnimator {
        // Get the center for the FAB
        val cx =
            mContinueFABContainer.x.toInt() + mContinueFABContainer.measuredHeight / 2
        val cy =
            mContinueFABContainer.y.toInt() + mContinueFABContainer.measuredWidth / 2

        // get the final radius for the clipping circle
        val dx = max(cx, mTransitionViewWhite.width - cx)
        val dy = max(cy, mTransitionViewWhite.height - cy)
        val finalRadius = hypot(dx.toDouble(), dy.toDouble()).toFloat()

        mTransitionViewWhite.isAttachedToWindow

        val blueTransitionAnimation =
            ViewAnimationUtils.createCircularReveal(mTransitionViewWhite, cx, cy, 0f, finalRadius)
        blueTransitionAnimation.interpolator = AccelerateDecelerateInterpolator()
        blueTransitionAnimation.duration = revealAnimationDuration.toLong()
        blueTransitionAnimation.addListener(object : SupportAnimator.AnimatorListener {
            override fun onAnimationStart() {
                //mTransitionViewWhite.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                mTransitionViewWhite.visibility = View.VISIBLE
                mTransitionViewWhite.bringToFront()
                mContinueFABShadow.bringToFront()
                mContinueFAB.bringToFront()
            }

            override fun onAnimationEnd() {
                //mTransitionViewWhite.setLayerType(View.LAYER_TYPE_NONE, null);
            }

            override fun onAnimationCancel() {
            }

            override fun onAnimationRepeat() {
            }
        })

        Handler().postDelayed(
            { blueTransitionAnimation.start() },
            revealAnimationDelay.toLong()
        )

        return blueTransitionAnimation
    }

    private fun showReverseTransitionAnimation() {
        mTransitionViewWhite.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val whiteReversed = transitionAnimationWhite!!.reverse()
        whiteReversed.interpolator = AccelerateDecelerateInterpolator()
        whiteReversed.addListener(object : SupportAnimator.AnimatorListener {
            override fun onAnimationStart() {
                mTransitionViewWhite.visibility = View.VISIBLE
                mTransitionViewWhite.bringToFront()
            }

            override fun onAnimationEnd() {
                Service.bringToBack(mTransitionViewWhite)
                mTransitionViewWhite.setLayerType(View.LAYER_TYPE_NONE, null)
                mTransitionViewWhite.setLayerType(View.LAYER_TYPE_NONE, null)
                mTransitionViewWhite.visibility = View.INVISIBLE
                mContinueFABContainer.isClickable = true

                mContinueIcon.bringToFront()
                mContinueIcon.setVisibility(View.VISIBLE)
                showContinueIconAnimations()
            }

            override fun onAnimationCancel() {
            }

            override fun onAnimationRepeat() {
            }
        })
        whiteReversed.duration = revealAnimationDuration.toLong()
        Handler().postDelayed({ whiteReversed.start() }, revealAnimationDelay.toLong())
        hasTransitioned = false
    }

    private fun showContinueIconAnimations() {
        mContinueIcon.setVisibility(View.VISIBLE)
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
        val mRotateAnimation: Animation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        mRotateAnimation.setRepeatCount(0)
        mScaleAnimation.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationStart(animation: Animation?) {
                mContinueIcon.setVisibility(View.VISIBLE)
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

    private class CheckDataFetchingTask(activity: ConfirmSetupActivity?) : Runnable {
        private val activity: WeakReference<ConfirmSetupActivity?> =
            WeakReference<ConfirmSetupActivity?>(activity)

        override fun run() {
            while (LoginActivity.Companion.loadingFollows()) {
                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    Timber.e(e)
                }
            }

            val animator: SupportAnimator.AnimatorListener =
                object : SupportAnimator.AnimatorListener {
                    override fun onAnimationEnd() {
                        activity.get()!!.navigateToNextActivity()
                    }

                    override fun onAnimationStart() {
                    }

                    override fun onAnimationCancel() {
                    }

                    override fun onAnimationRepeat() {
                    }
                }

            if (!activity.get()!!.mTransitionViewWhite.isAttachedToWindow) {
                animator.onAnimationEnd()
                return
            }

            Execute.ui {
                activity.get()!!.showTransitionAnimation().addListener(animator)
            }
        }
    }
}
