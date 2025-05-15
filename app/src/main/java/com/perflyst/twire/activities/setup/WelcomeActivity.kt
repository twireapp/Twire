package com.perflyst.twire.activities.setup

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.perflyst.twire.R
import com.perflyst.twire.service.Service
import com.perflyst.twire.utils.AnimationListenerAdapter
import io.codetail.animation.SupportAnimator
import io.codetail.animation.ViewAnimationUtils
import timber.log.Timber
import kotlin.math.hypot
import kotlin.math.max

class WelcomeActivity : SetupBaseActivity() {
    val revealAnimationDuration: Int = 650
    val revealAnimationDelay: Int = 200
    val animationsStartDelay: Int = 500
    val logoAnimationDuration: Int = 1000
    val logoContainerAnimationDuration: Int = 1750
    val welcomeTextAnimationDuration: Int = 900
    val welcomeTextAnimationBaseDelay: Int = 175
    val continueFabAnimationDuration: Int = 750
    private var hasTransitioned = false
    private var transitionAnimationWhite: SupportAnimator? = null
    private var transitionAnimationBlue: SupportAnimator? = null
    private lateinit var mWelcomeTextLineOne: TextView
    private lateinit var mWelcomeTextLineTwo: TextView
    private lateinit var mLogo: ImageView
    private lateinit var mContinueIcon: ImageView
    private lateinit var mLogoCenter: View
    private lateinit var mContinueFAB: View
    private lateinit var mContinueFABShadow: View
    private lateinit var mTransitionViewWhite: View
    private lateinit var mTransitionViewBlue: View
    private lateinit var mLogoContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val mWelcomeText = findViewById<RelativeLayout>(R.id.welcome_text)
        mWelcomeTextLineOne = findViewById(R.id.welcome_text_line_one)
        mWelcomeTextLineTwo = findViewById(R.id.welcome_text_line_two)

        mLogo = findViewById(R.id.welcome_icon)
        mContinueIcon = findViewById(R.id.forward_arrow)
        mLogoContainer = findViewById(R.id.welcome_icon_layout)
        mLogoCenter = findViewById(R.id.welcome_icon_center)
        mContinueFAB = findViewById(R.id.continue_circle)
        mContinueFABShadow = findViewById(R.id.welcome_continue_circle_shadow)
        mTransitionViewWhite = findViewById(R.id.transition_view)
        mTransitionViewBlue = findViewById(R.id.transition_view_blue)

        mTransitionViewBlue.visibility = View.INVISIBLE
        mTransitionViewWhite.visibility = View.INVISIBLE
        mWelcomeTextLineOne.visibility = View.INVISIBLE
        mWelcomeTextLineTwo.visibility = View.INVISIBLE
        mLogo.setVisibility(View.INVISIBLE)
        mLogoContainer.visibility = View.INVISIBLE
        mContinueFAB.visibility = View.INVISIBLE
        mContinueIcon.setVisibility(View.INVISIBLE)


        Service.bringToBack(mTransitionViewWhite)
        Service.bringToBack(mTransitionViewBlue)

        // Change the position of the WelcomeText. Doing it this way is more dynamic, instead of a fixed
        // DP length from the bottom
        val yPosition = (2.5 * (Service.getScreenHeight(this) / 5)).toInt()
        mWelcomeText.y = yPosition.toFloat()

        // Start the animations. Make sure the animations that in the correct order,
        // by adding Animation Listeners that start the next animation on animation end.
        Handler().postDelayed({
            startLogoContainerAnimations().setAnimationListener(object :
                AnimationListenerAdapter() {
                override fun onAnimationStart(animation: Animation?) {
                    mLogoContainer.visibility = View.VISIBLE
                }
            })
            startLogoOuterAnimations().setAnimationListener(object : AnimationListenerAdapter() {
                override fun onAnimationEnd(animation: Animation?) {
                    startWelcomeTextLineAnimations(mWelcomeTextLineOne, 1)
                    startWelcomeTextLineAnimations(mWelcomeTextLineTwo, 2).setAnimationListener(
                        object : AnimationListenerAdapter() {
                            override fun onAnimationEnd(animation: Animation?) {
                                startContinueFABAnimations()
                            }
                        })
                }
            })
        }, animationsStartDelay.toLong())


        mContinueFAB.setOnClickListener { v: View? ->
            // Get the center for the FAB
            val cx = mContinueFAB.x.toInt() + mContinueFAB.measuredHeight / 2
            val cy = mContinueFAB.y.toInt() + mContinueFAB.measuredWidth / 2

            // get the final radius for the clipping circle
            val dx = max(cx, mTransitionViewWhite.width - cx)
            val dy = max(cy, mTransitionViewWhite.height - cy)
            val finalRadius = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            val whiteTransitionAnimation =
                ViewAnimationUtils.createCircularReveal(
                    mTransitionViewWhite,
                    cx,
                    cy,
                    0f,
                    finalRadius
                )
            whiteTransitionAnimation.interpolator = AccelerateDecelerateInterpolator()
            whiteTransitionAnimation.duration = revealAnimationDuration.toLong()
            whiteTransitionAnimation.addListener(object : SupportAnimator.AnimatorListener {
                override fun onAnimationStart() {
                    mTransitionViewWhite.bringToFront()
                    mTransitionViewWhite.visibility = View.VISIBLE
                    mContinueFAB.isClickable = false
                    startHideContinueIconAnimations()
                }

                override fun onAnimationEnd() {
                    transitionAnimationWhite = whiteTransitionAnimation
                }

                override fun onAnimationCancel() {
                    onAnimationEnd()
                }

                override fun onAnimationRepeat() {
                }
            })


            val blueTransitionAnimation =
                ViewAnimationUtils.createCircularReveal(
                    mTransitionViewBlue,
                    cx,
                    cy,
                    0f,
                    finalRadius
                )
            blueTransitionAnimation.interpolator = AccelerateDecelerateInterpolator()
            blueTransitionAnimation.duration = revealAnimationDuration.toLong()
            blueTransitionAnimation.addListener(object : SupportAnimator.AnimatorListener {
                override fun onAnimationStart() {
                    mTransitionViewBlue.visibility = View.VISIBLE
                    mTransitionViewBlue.bringToFront()
                    mContinueFABShadow.bringToFront()
                    mContinueFAB.bringToFront()
                }

                override fun onAnimationEnd() {
                    transitionAnimationBlue = blueTransitionAnimation
                }

                override fun onAnimationCancel() {
                    onAnimationEnd()
                }

                override fun onAnimationRepeat() {
                }
            })

            whiteTransitionAnimation.start()
            blueTransitionAnimation.startDelay = revealAnimationDelay.toLong()
            blueTransitionAnimation.start()
            Handler().postDelayed({
                Timber.d("Navigating To Login Activity")
                navigateToLoginActivity()
            }, (revealAnimationDelay + revealAnimationDuration).toLong())
        }
    }

    public override fun onResume() {
        super.onResume()
        // The user has returned from the login screen. Lol wtf?
        if (transitionAnimationWhite != null && hasTransitioned) {
            val blueReversed = transitionAnimationBlue!!.reverse()
            blueReversed.interpolator = AccelerateDecelerateInterpolator()
            blueReversed.addListener(object : SupportAnimator.AnimatorListener {
                override fun onAnimationStart() {
                    mTransitionViewBlue.visibility = View.VISIBLE
                    mTransitionViewBlue.bringToFront()
                }

                override fun onAnimationEnd() {
                    Service.bringToBack(mTransitionViewBlue)
                    mTransitionViewBlue.visibility = View.INVISIBLE
                }

                override fun onAnimationCancel() {
                }

                override fun onAnimationRepeat() {
                }
            })
            blueReversed.duration = revealAnimationDuration.toLong()
            blueReversed.start()

            val whiteReversed = transitionAnimationWhite!!.reverse()
            whiteReversed.interpolator = AccelerateDecelerateInterpolator()
            whiteReversed.addListener(object : SupportAnimator.AnimatorListener {
                override fun onAnimationStart() {
                    mTransitionViewWhite.visibility = View.VISIBLE
                    mTransitionViewWhite.bringToFront()
                }

                override fun onAnimationEnd() {
                    Service.bringToBack(mTransitionViewWhite)
                    mTransitionViewWhite.visibility = View.INVISIBLE
                    mContinueFAB.isClickable = true
                    startShowContinueIconAnimations()
                }

                override fun onAnimationCancel() {
                }

                override fun onAnimationRepeat() {
                }
            })
            whiteReversed.duration = revealAnimationDuration.toLong()
            Handler().postDelayed(
                { whiteReversed.start() },
                revealAnimationDelay.toLong()
            )
            hasTransitioned = false
        }
    }

    private fun navigateToLoginActivity() {
        // Go to the login activity, with no transition.
        hasTransitioned = true
        val loginActivityIntent = Intent(baseContext, LoginActivity::class.java)
        loginActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(loginActivityIntent, null)
    }

    /**
     * Animations for the views in this activity
     */
    private fun startLogoContainerAnimations(): AnimationSet {
        mLogoCenter.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val mInitLogoAnimations = AnimationSet(true)
        val trans = TranslateAnimation(0f, 0f, Service.getScreenHeight(this).toFloat(), 0f)

        mInitLogoAnimations.setDuration(logoContainerAnimationDuration.toLong())
        mInitLogoAnimations.setFillAfter(true)
        mInitLogoAnimations.interpolator = OvershootInterpolator(0.7f)
        mInitLogoAnimations.addAnimation(trans)
        mInitLogoAnimations.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                mLogoCenter.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        })

        mLogoContainer.startAnimation(mInitLogoAnimations)
        return mInitLogoAnimations
    }

    private fun startLogoOuterAnimations(): AnimationSet {
        mLogo.setLayerType(View.LAYER_TYPE_HARDWARE, null)

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
        mScaleAnimation.setDuration(logoAnimationDuration.toLong())
        mScaleAnimation.interpolator = OvershootInterpolator(0.7f)

        val mAlphaAnimation: Animation = AlphaAnimation(0f, 1f)
        mAlphaAnimation.setDuration(logoAnimationDuration.toLong())
        mAlphaAnimation.interpolator = DecelerateInterpolator()
        mAlphaAnimation.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationStart(animation: Animation?) {
                mLogo.setVisibility(View.VISIBLE)
            }
        })

        val mRotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        mRotateAnimation.interpolator = DecelerateInterpolator()
        mRotateAnimation.setDuration(logoAnimationDuration.toLong())
        mRotateAnimation.setRepeatCount(0)

        val mLogoAnimations = AnimationSet(false)
        mLogoAnimations.interpolator = AccelerateDecelerateInterpolator()
        mLogoAnimations.setFillBefore(true)
        mLogoAnimations.setFillAfter(true)
        mLogoAnimations.addAnimation(mScaleAnimation)
        mLogoAnimations.addAnimation(mRotateAnimation)
        mLogoAnimations.addAnimation(mAlphaAnimation)

        mLogoAnimations.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                mLogo.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        })

        Handler().postDelayed(
            { mLogo.startAnimation(mLogoAnimations) },
            (logoContainerAnimationDuration - logoAnimationDuration).toLong()
        )

        return mLogoAnimations
    }

    private fun startWelcomeTextLineAnimations(
        mWelcomeTextLine: TextView,
        lineNumber: Int
    ): AnimationSet {
        mWelcomeTextLine.setLayerType(View.LAYER_TYPE_HARDWARE, null)

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
                mWelcomeTextLine.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {
                mWelcomeTextLine.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        })


        val mWelcomeTextAnimations = AnimationSet(false)
        mWelcomeTextAnimations.setDuration(welcomeTextAnimationDuration.toLong())
        mWelcomeTextAnimations.interpolator = AccelerateDecelerateInterpolator()
        mWelcomeTextAnimations.setFillBefore(true)
        mWelcomeTextAnimations.setFillAfter(true)
        mWelcomeTextAnimations.addAnimation(mAlphaAnimation)
        mWelcomeTextAnimations.addAnimation(mTranslationAnimation)

        val delay =
            welcomeTextAnimationBaseDelay * (if (lineNumber < 3) lineNumber else lineNumber * 2)
        Handler().postDelayed(
            { mWelcomeTextLine.startAnimation(mWelcomeTextAnimations) },
            delay.toLong()
        )

        return mWelcomeTextAnimations
    }

    private fun startContinueFABAnimations() {
        mContinueFAB.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        mContinueFABShadow.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val travelDistance = Service.dpToPixels(
            baseContext,
            getResources().getDimension(R.dimen.welcome_continue_circle_diameter)
        )

        val mTranslationAnimation: Animation =
            TranslateAnimation(0f, 0f, travelDistance.toFloat(), 0f)

        val mContinueFABAnimations = AnimationSet(true)
        mContinueFABAnimations.setDuration(continueFabAnimationDuration.toLong())
        mContinueFABAnimations.interpolator = OvershootInterpolator(1f)
        mContinueFABAnimations.addAnimation(mTranslationAnimation)
        mContinueFABAnimations.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationStart(animation: Animation?) {
                // Start running the show animation for the FAB icon a third into this animation
                mContinueFAB.visibility = View.VISIBLE
                Handler().postDelayed({
                    mContinueIcon.setVisibility(View.VISIBLE)
                    startShowContinueIconAnimations()
                }, (continueFabAnimationDuration / 3).toLong())
            }

            override fun onAnimationEnd(animation: Animation?) {
                mContinueFAB.setLayerType(View.LAYER_TYPE_NONE, null)
                mContinueFABShadow.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        })

        mContinueFAB.startAnimation(mContinueFABAnimations)
        mContinueFABShadow.startAnimation(mContinueFABAnimations)
    }

    private fun startShowContinueIconAnimations() {
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
        startContinueIconAnimations(mScaleAnimation)
    }

    private fun startHideContinueIconAnimations() {
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
        startContinueIconAnimations(mScaleAnimation)
    }

    private fun startContinueIconAnimations(mScaleAnimation: Animation?) {
        val mRotateAnimation: Animation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        mRotateAnimation.setRepeatCount(0)

        val mAnimations = AnimationSet(true)
        mAnimations.setDuration(revealAnimationDuration.toLong())
        mAnimations.setFillAfter(true)
        mAnimations.interpolator = OvershootInterpolator(1.5f)
        mAnimations.addAnimation(mScaleAnimation)
        mAnimations.addAnimation(mRotateAnimation)

        mContinueIcon.startAnimation(mAnimations)
    }
}
