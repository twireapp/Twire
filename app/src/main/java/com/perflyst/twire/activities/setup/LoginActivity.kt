package com.perflyst.twire.activities.setup

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
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
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewDatabase
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.perflyst.twire.R
import com.perflyst.twire.misc.SecretKeys
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.generalTwitchAccessToken
import com.perflyst.twire.service.Settings.generalTwitchDisplayName
import com.perflyst.twire.service.Settings.isLoggedIn
import com.perflyst.twire.service.Settings.isSetup
import com.perflyst.twire.tasks.GetFollowsFromDB
import com.perflyst.twire.tasks.HandlerUserLoginTask
import com.perflyst.twire.utils.AnimationListenerAdapter
import com.perflyst.twire.utils.Constants
import com.perflyst.twire.utils.Execute
import com.rey.material.widget.ProgressView
import com.rey.material.widget.SnackBar
import io.codetail.animation.SupportAnimator
import io.codetail.animation.ViewAnimationUtils
import timber.log.Timber
import java.lang.String
import kotlin.Boolean
import kotlin.Int
import kotlin.math.hypot
import kotlin.math.max

class LoginActivity : SetupBaseActivity() {
    private val loginUrl = "https://id.twitch.tv/oauth2/authorize" +
            "?client_id=" + SecretKeys.APPLICATION_CLIENT_ID +
            "&redirect_uri=http%3A%2F%2Flocalhost/oauth_authorizing" +
            "&response_type=token" +
            "&scope=" + String.join("%20", *Constants.TWITCH_SCOPES)
    private val showWebviewAnimationDuration = 900
    private val showSuccessIconDuration = 800
    private val revealAnimationDuration = 650
    private val revealAnimationDelay = 200
    private val showSnackbarDelay = 200
    private var isWebViewShown = false
    private var isWebViewHiding = false
    private var hasTransitioned = false
    private var transitionAnimationWhite: SupportAnimator? = null
    private var transitionAnimationBlue: SupportAnimator? = null
    private lateinit var mGearIcon: ImageView
    private lateinit var mSuccessIcon: ImageView
    private lateinit var mContinueIcon: ImageView
    private lateinit var mWebViewProgress: ProgressView
    private lateinit var loginWebView: WebView
    private lateinit var mLoginTextLineOne: TextView
    private lateinit var mLoginTextLineTwo: TextView
    private lateinit var mSuccessMessage: TextView
    private lateinit var mSkipText: TextView
    private lateinit var mContinueFAB: View
    private lateinit var mContinueFABShadow: View
    private lateinit var mSuccessCircle: View
    private lateinit var mSuccessCircleShadow: View
    private lateinit var mTransitionViewWhite: View
    private lateinit var mTransitionViewBlue: View
    private lateinit var mContinueFABContainer: FrameLayout
    private lateinit var mWebViewContainer: FrameLayout
    private lateinit var mLoginTextContainer: RelativeLayout
    private var mSnackbar: SnackBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mLoginTextContainer = findViewById<RelativeLayout>(R.id.login_text_container)
        mWebViewProgress = findViewById<ProgressView>(R.id.SetupProgress)
        mWebViewContainer = findViewById<FrameLayout>(R.id.webview_container)
        mContinueFABContainer = findViewById<FrameLayout>(R.id.login_continue_circle_container)
        mGearIcon = findViewById<ImageView>(R.id.login_icon)
        mSuccessIcon = findViewById<ImageView>(R.id.login_icon_done)
        mContinueIcon = findViewById<ImageView>(R.id.forward_arrow)
        mLoginTextLineOne = findViewById<TextView>(R.id.login_text_line_one)
        mLoginTextLineTwo = findViewById<TextView>(R.id.login_text_line_two)
        mSuccessMessage = findViewById<TextView>(R.id.login_success_message)
        mSkipText = findViewById<TextView>(R.id.skip_text)
        loginWebView = findViewById<WebView>(R.id.login_webview)
        mContinueFAB = findViewById<View>(R.id.login_continue_circle)
        mContinueFABShadow = findViewById<View>(R.id.login_continue_circle_shadow)
        mSuccessCircle = findViewById<View>(R.id.login_success_circle)
        mSuccessCircleShadow = findViewById<View>(R.id.login_success_shadow)
        mTransitionViewWhite = findViewById<View>(R.id.transition_view)
        mTransitionViewBlue = findViewById<View>(R.id.transition_view_blue)

        mSuccessMessage.visibility = View.INVISIBLE
        mContinueIcon.setVisibility(View.INVISIBLE)
        mSuccessCircleShadow.visibility = View.INVISIBLE
        mSuccessIcon.setVisibility(View.INVISIBLE)
        mSuccessCircle.visibility = View.INVISIBLE
        mLoginTextLineOne.visibility = View.INVISIBLE
        mLoginTextLineTwo.visibility = View.INVISIBLE
        loginWebView.visibility = View.INVISIBLE
        mGearIcon.setVisibility(View.INVISIBLE)
        mTransitionViewBlue.visibility = View.INVISIBLE
        mTransitionViewWhite.visibility = View.INVISIBLE
        mSkipText.visibility = View.INVISIBLE

        checkSetupType()

        val textPosition = (2.5 * (Service.getScreenHeight(this) / 5)).toInt().toFloat()
        mLoginTextContainer.y = textPosition
        mSuccessMessage.y = textPosition
        mContinueFABContainer.setOnClickListener { v: View? -> showLoginView() }

        Service.bringToBack(mTransitionViewWhite)
        Service.bringToBack(mTransitionViewBlue)
        mLoginTextLineOne.bringToFront()
        mLoginTextLineTwo.bringToFront()

        initSkipView()
        setupPrelaunchLogin()
        initSnackbar()
        initLoginView()
        showLogoAnimations(mGearIcon)
        showTextLineAnimations(mLoginTextLineOne, 1)
        showTextLineAnimations(mLoginTextLineTwo, 2)
        showTextLineAnimations(mSkipText, 2)

        val animationSet = getContinueIconAnimations(270)
        val showContinueIconDelay = 600
        animationSet.setStartOffset(showContinueIconDelay.toLong())
        animationSet.start()
    }

    private fun setupPrelaunchLogin() {
        findViewById<View?>(R.id.btn_prelaunch_login)?.setOnClickListener { v: View? ->
            val handleTask = HandlerUserLoginTask(this@LoginActivity)
            Execute.background(handleTask)
        }
    }

    /**
     * initiates the skip view.
     */
    private fun initSkipView() {
        mSkipText.setOnClickListener { v: View? -> showSkippingAnimation() }
    }

    /**
     * Checks if the login is part of setup, a relogin or token revalidation
     * Sets states correctly depending on result
     */
    private fun checkSetupType() {
        if (intent.hasExtra(getString(R.string.login_intent_part_of_setup))) {
            isPartOfSetup =
                intent.getBooleanExtra(getString(R.string.login_intent_part_of_setup), true)

            if (intent.hasExtra(getString(R.string.login_intent_token_not_valid)) && intent.getBooleanExtra(
                    getString(R.string.login_intent_token_not_valid),
                    false
                )
            ) {
                mLoginTextLineOne.setText(R.string.login_invalid_token_text_line_one)
                mLoginTextLineTwo.setText(R.string.login_invalid_token_text_line_two)
            }
        }
    }

    public override fun onResume() {
        super.onResume()

        if (transitionAnimationWhite != null && hasTransitioned) {
            showReverseTransitionAnimation()
        }
    }

    override fun onBackPressed() {
        if (isWebViewShown && !isWebViewHiding) {
            hideLoginView()
        } else {
            toTransition = false
            hideAllViews()!!.setAnimationListener(object : AnimationListenerAdapter() {
                override fun onAnimationEnd(animation: Animation?) {
                    super@LoginActivity.onBackPressed()
                    // We don't want a transition when going back. The activities handle the animation themselves.
                    overridePendingTransition(0, 0)
                }
            })
        }
    }

    fun handleLoginSuccess() {
        toTransition = true
        showSuccessAnimation()
        val transitionDelay = 2000
        Handler().postDelayed({
            if (toTransition) {
                showTransitionAnimation()
            }
        }, transitionDelay.toLong())

        mContinueFABContainer.isClickable = false

        // Gets the follows that already exist in the database. Then Checks if any of them are online.
        // THEN it retrieves the user's follows from Twitch.
        // It is better to it this way, as this will give us the opportunity to use this Activity again,
        // if the user later removes Twire Authentication from Twitch and we need the user to sign in again
        // OR if the user signs in to a new account.

        // Seb you wonderful man. - Seb from the future
        subscriptionsTask = GetFollowsFromDB(this)
        Execute.background(subscriptionsTask!!)
    }

    fun handleNoInternet() {
        if (mSnackbar!!.isShown()) {
            mSnackbar!!.dismiss()
            mSnackbar!!.clearAnimation()
        }

        if (isWebViewShown) {
            hideLoginView()
        }

        mContinueFABContainer.isClickable = false

        Handler().postDelayed({
            mSnackbar!!
                .duration(0)
                .text(R.string.login_user_no_internet)
                .actionText(R.string.login_user_no_internet_action)
                .actionClickListener { sb: SnackBar?, actionId: Int ->
                    mContinueFABContainer.isClickable = true
                    loginWebView.loadUrl(loginUrl)
                    sb!!.dismiss()
                }
                .show(this@LoginActivity)
        }, showSnackbarDelay.toLong())
    }

    private fun handleUserCancel() {
        val snackbarDuration = 8 * 1000
        mSnackbar!!
            .duration(snackbarDuration.toLong())
            .text(R.string.login_user_cancel)
            .actionText(R.string.login_user_cancel_action)
            .actionClickListener { sb: SnackBar?, actionId: Int -> sb!!.dismiss() }


        hideLoginView().animations[0]
            .setAnimationListener(object : AnimationListenerAdapter() {
                override fun onAnimationEnd(animation: Animation?) {
                    loginWebView.loadUrl(loginUrl)
                    Handler().postDelayed(
                        { mSnackbar!!.show(this@LoginActivity) },
                        showSnackbarDelay.toLong()
                    )
                }
            })
    }

    private fun showLoginView() {
        showWebViewAnimation()
        hideFABAnimation()
        hideContinueIconAnimations(mContinueIcon)
    }

    private fun hideLoginView(): AnimationSet {
        showFABAnimation()
        Handler().postDelayed(
            { getContinueIconAnimations(270) },
            (showWebviewAnimationDuration - 400).toLong()
        )
        return hideWebViewAnimation()
    }

    private fun getAccessTokenFromURL(url: kotlin.String): kotlin.String {
        val startIdentifier = "access_token"
        val endIdentifier = "&scope"

        val startIndex = url.indexOf(startIdentifier) + startIdentifier.length + 1
        val lastIndex = url.indexOf(endIdentifier)

        return url.substring(startIndex, lastIndex)
    }

    private fun initSnackbar() {
        mSnackbar = SnackBar(this)
        mSnackbar!!.applyStyle(R.style.snack_bar_style_mobile)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initLoginView() {
        val cm = CookieManager.getInstance()
        cm.removeAllCookie()
        val db = WebViewDatabase.getInstance(this)
        db.clearFormData()

        val ws = loginWebView.getSettings()
        ws.saveFormData = false
        ws.savePassword = false

        loginWebView.clearCache(true)
        ws.saveFormData = false
        ws.javaScriptEnabled = true
        ws.javaScriptCanOpenWindowsAutomatically = true
        ws.setSupportZoom(true)
        ws.setUserAgentString(
            ws.userAgentString.replaceFirst("Mobile Safari".toRegex(), "Safari")
                .replaceFirst("\\(.+?\\)".toRegex(), "(X11; Linux x86_64)")
        )

        loginWebView.setWebViewClient(
            object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: kotlin.String?,
                    failingUrl: kotlin.String
                ) {
                    if (failingUrl == loginUrl) {
                        // The user has no internet connection
                        Timber.e("The WebView failed to load URL - %s", failingUrl)
                        handleNoInternet()
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView, url: kotlin.String): Boolean {
                    Timber.d("shouldOverrideUrlLoading - %s", url)

                    // The user has successfully logged in
                    // Example:
                    // http://nrask.net/oauth_authorizing#access_token=r6z9er061caeq4sjq75ncv7goh27p8&scope=user_read+channel_read+chat_login+user_follows_edit
                    // Access Token from response:
                    // r6z9er061caeq4sjq75ncv7goh27p8
                    if (url.contains("access_token") && url.contains("oauth_authorizing")) {
                        val mAccessToken = getAccessTokenFromURL(url)
                        Timber.d("Access token received - %s", mAccessToken)
                        if (isWebViewShown) {
                            hideLoginView()
                        }

                        // set the access token here so the following request works
                        generalTwitchAccessToken = mAccessToken

                        val handleTask = HandlerUserLoginTask(this@LoginActivity)
                        Execute.background(handleTask)

                        val cm = CookieManager.getInstance()
                        cm.removeAllCookie()

                        view.clearCache(true)
                        view.clearHistory()
                        view.clearFormData()
                    } else if (url.contains("The+user+denied+you+access")) {
                        Timber.d("The user cancelled the login")
                        // The user pressed "Cancel in the webview"
                        handleUserCancel()
                        return true
                    } else if (!isPartOfSetup && url.contains("passport")) {
                        /*Timber.d("CONTAINING PASSWORD");
                            view.loadUrl(url);*/
                    }

                    return false
                }

                override fun onPageFinished(view: WebView?, url: kotlin.String?) {
                    super.onPageFinished(view, url)
                    mWebViewProgress.stop()
                }
            }
        )

        loginWebView.loadUrl(loginUrl)
    }

    private fun hideAllViews(): AnimationSet? {
        if (mContinueIcon.isVisible) {
            hideContinueIconAnimations(mContinueIcon)
        }
        val hideViewAnimationDuration = 550
        hideViewAnimation(mGearIcon, hideViewAnimationDuration)
        hideViewAnimation(mLoginTextLineOne, hideViewAnimationDuration)
        if (mSuccessMessage.visibility != View.INVISIBLE) {
            hideViewAnimation(mSuccessMessage, hideViewAnimationDuration)
        }

        if (mSuccessIcon.visibility != View.INVISIBLE) {
            hideViewAnimation(mSuccessIcon, hideViewAnimationDuration)
            hideViewAnimation(mSuccessCircle, hideViewAnimationDuration)
            hideViewAnimation(mSuccessCircleShadow, hideViewAnimationDuration)
        }

        return hideViewAnimation(mLoginTextLineTwo, hideViewAnimationDuration)
    }

    private fun navigateToNotificationActivity() {
        // Go to the login activity, with no transition.
        hasTransitioned = true
        isSetup = true
        if (loadingFollows()) {
            this.startActivity(Intent(baseContext, ConfirmSetupActivity::class.java))
        } else {
            this.startActivity(Service.getStartPageIntent(baseContext))
        }
        this.overridePendingTransition(0, 0)
    }

    private fun showSkippingAnimation() {
        // Get the center for the FAB
        val cx =
            mContinueFABContainer.x.toInt() + mContinueFABContainer.measuredHeight / 2
        val cy =
            mContinueFABContainer.y.toInt() + mContinueFABContainer.measuredWidth / 2

        // get the final radius for the clipping circle
        val dx = max(cx, mTransitionViewWhite.width - cx)
        val dy = max(cy, mTransitionViewWhite.height - cy)
        val finalRadius = hypot(dx.toDouble(), dy.toDouble()).toFloat()

        mTransitionViewBlue.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        mTransitionViewWhite.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val whiteTransitionAnimation =
            ViewAnimationUtils.createCircularReveal(mTransitionViewWhite, cx, cy, 0f, finalRadius)
        whiteTransitionAnimation.interpolator = AccelerateDecelerateInterpolator()
        whiteTransitionAnimation.duration = revealAnimationDuration.toLong()
        whiteTransitionAnimation.addListener(object : SupportAnimator.AnimatorListener {
            override fun onAnimationStart() {
                mTransitionViewWhite.bringToFront()
                mTransitionViewWhite.visibility = View.VISIBLE
                mContinueFABContainer.isClickable = false
                if (mContinueIcon.isVisible) {
                    hideContinueIconAnimations(mContinueIcon)
                }
            }

            override fun onAnimationEnd() {
                hasTransitioned = true
                transitionAnimationWhite = whiteTransitionAnimation
                isSetup = true
                isLoggedIn = false
                val intent = Service.getStartPageIntent(baseContext)
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
            }

            override fun onAnimationCancel() {
            }

            override fun onAnimationRepeat() {
            }
        })

        whiteTransitionAnimation.start()
    }

    /**
     * Animations only here from and down
     */
    private fun showTransitionAnimation() {
        // Get the center for the FAB
        val cx =
            mContinueFABContainer.x.toInt() + mContinueFABContainer.measuredHeight / 2
        val cy =
            mContinueFABContainer.y.toInt() + mContinueFABContainer.measuredWidth / 2

        // get the final radius for the clipping circle
        val dx = max(cx, mTransitionViewWhite.width - cx)
        val dy = max(cy, mTransitionViewWhite.height - cy)
        val finalRadius = hypot(dx.toDouble(), dy.toDouble()).toFloat()

        //mTransitionViewBlue.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        //mTransitionViewWhite.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        val whiteTransitionAnimation =
            ViewAnimationUtils.createCircularReveal(mTransitionViewWhite, cx, cy, 0f, finalRadius)
        whiteTransitionAnimation.interpolator = AccelerateDecelerateInterpolator()
        whiteTransitionAnimation.duration = revealAnimationDuration.toLong()
        whiteTransitionAnimation.addListener(object : SupportAnimator.AnimatorListener {
            override fun onAnimationStart() {
                mTransitionViewWhite.bringToFront()
                mTransitionViewWhite.visibility = View.VISIBLE
                mContinueFABContainer.isClickable = false
                if (mContinueIcon.isVisible) {
                    hideContinueIconAnimations(mContinueIcon)
                }
            }

            override fun onAnimationEnd() {
                transitionAnimationWhite = whiteTransitionAnimation
            }

            override fun onAnimationCancel() {
            }

            override fun onAnimationRepeat() {
            }
        })


        val blueTransitionAnimation =
            ViewAnimationUtils.createCircularReveal(mTransitionViewBlue, cx, cy, 0f, finalRadius)
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
                mTransitionViewBlue.setLayerType(View.LAYER_TYPE_NONE, null)
                mTransitionViewWhite.setLayerType(View.LAYER_TYPE_NONE, null)
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
            Timber.d("Navigating to NotificationActivity")
            navigateToNotificationActivity()
        }, (revealAnimationDelay + revealAnimationDuration).toLong())
    }

    private fun showReverseTransitionAnimation() {
        mTransitionViewWhite.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        if (transitionAnimationBlue != null) {
            mTransitionViewBlue.setLayerType(View.LAYER_TYPE_HARDWARE, null)
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
        }

        val whiteReversed = transitionAnimationWhite!!.reverse()
        whiteReversed.interpolator = AccelerateDecelerateInterpolator()
        whiteReversed.addListener(object : SupportAnimator.AnimatorListener {
            override fun onAnimationStart() {
                mTransitionViewWhite.visibility = View.VISIBLE
                mTransitionViewWhite.bringToFront()
            }

            override fun onAnimationEnd() {
                Service.bringToBack(mTransitionViewWhite)
                mTransitionViewBlue.setLayerType(View.LAYER_TYPE_NONE, null)
                mTransitionViewWhite.setLayerType(View.LAYER_TYPE_NONE, null)
                mTransitionViewWhite.visibility = View.INVISIBLE
                mContinueFABContainer.isClickable = true
                mContinueFABContainer.setOnClickListener { v: View? -> showTransitionAnimation() }

                mContinueIcon.bringToFront()
                mContinueIcon.setVisibility(View.VISIBLE)
                getContinueIconAnimations(360).start()
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

    private fun showSuccessAnimation() {
        Timber.d("Showing Success Animation")
        mSuccessMessage.text =
            getString(R.string.login_on_success_message, generalTwitchDisplayName)
        val mCircleShadowAnimations = getSuccessAnimation(mSuccessCircleShadow)
        val mIconAnimations = getSuccessAnimation(mSuccessIcon)
        val mCircleAnimations = getSuccessAnimation(mSuccessCircle)

        Handler().postDelayed({
            mSuccessCircleShadow.startAnimation(mCircleShadowAnimations)
            mSuccessIcon.startAnimation(mIconAnimations)
            showTextLineAnimations(mSuccessMessage, 1)
        }, 150)
        mSuccessCircle.startAnimation(mCircleAnimations)
        hideContinueIconAnimations(mContinueIcon)
        val hideInstructionsDuration = 500
        hideViewAnimation(mLoginTextContainer, hideInstructionsDuration)
    }

    private fun getContinueIconAnimations(toDegree: Int): AnimationSet {
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
            (toDegree - 360).toFloat(), toDegree.toFloat(),
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


        return mAnimations
    }

    private fun getSuccessAnimation(view: View): AnimationSet {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val mEndScale = 1.1f
        val mScaleAnimation: Animation = ScaleAnimation(
            0f,
            mEndScale,
            0f,
            mEndScale,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        mScaleAnimation.setDuration(showSuccessIconDuration.toLong())
        mScaleAnimation.interpolator = OvershootInterpolator(3f)

        val mAlphaAnimation: Animation = AlphaAnimation(0f, 1f)
        mAlphaAnimation.setDuration(showSuccessIconDuration.toLong())
        mAlphaAnimation.interpolator = DecelerateInterpolator()
        mAlphaAnimation.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationStart(animation: Animation?) {
                view.visibility = View.VISIBLE
            }
        })

        val mLogoAnimations = AnimationSet(false)
        mLogoAnimations.interpolator = AccelerateDecelerateInterpolator()
        mLogoAnimations.setFillBefore(true)
        mLogoAnimations.setFillAfter(true)
        mLogoAnimations.addAnimation(mScaleAnimation)
        mLogoAnimations.addAnimation(mAlphaAnimation)
        mLogoAnimations.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                view.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        })

        return mLogoAnimations
    }

    private fun showWebViewAnimation() {
        loginWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        isWebViewShown = true
        loginWebView.visibility = View.VISIBLE
        loginWebView.bringToFront()
        mWebViewProgress.bringToFront()

        val mTranslationAnimation: Animation = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 1f,
            Animation.RELATIVE_TO_PARENT, 0f
        )

        mTranslationAnimation.fillAfter = true
        mTranslationAnimation.interpolator = OvershootInterpolator(1f)

        val mAnimations = AnimationSet(false)
        mAnimations.addAnimation(mTranslationAnimation)
        mAnimations.setDuration(showWebviewAnimationDuration.toLong())
        mAnimations.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                loginWebView.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        })

        mWebViewContainer.startAnimation(mAnimations)
    }

    private fun hideWebViewAnimation(): AnimationSet {
        loginWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val mTranslationAnimation: Animation = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 1f
        )

        mTranslationAnimation.fillAfter = true
        mTranslationAnimation.interpolator = OvershootInterpolator(1f)

        val mAnimations = AnimationSet(false)
        mAnimations.addAnimation(mTranslationAnimation)
        mAnimations.setDuration(showWebviewAnimationDuration.toLong())
        mAnimations.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationStart(animation: Animation?) {
                isWebViewHiding = true
            }

            override fun onAnimationEnd(animation: Animation?) {
                loginWebView.setLayerType(View.LAYER_TYPE_NONE, null)
                isWebViewHiding = false
                isWebViewShown = false
                loginWebView.visibility = View.INVISIBLE
                loginWebView.translationY = 1f
            }
        })
        mWebViewContainer.startAnimation(mAnimations)
        return mAnimations
    }

    private fun hideFABAnimation() {
        mContinueFAB.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        mContinueFABShadow.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val mTranslationAnimation: Animation =
            TranslateAnimation(0f, 0f, 0f, (Service.getScreenHeight(this) * -1).toFloat())
        mTranslationAnimation.fillAfter = true
        mTranslationAnimation.interpolator = OvershootInterpolator(1f)

        val mAnimations = AnimationSet(false)
        mAnimations.setFillAfter(true)
        mAnimations.addAnimation(mTranslationAnimation)
        mAnimations.setDuration(showWebviewAnimationDuration.toLong())
        mAnimations.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationStart(animation: Animation?) {
                mContinueFABContainer.isClickable = false
            }

            override fun onAnimationEnd(animation: Animation?) {
                mContinueFAB.setLayerType(View.LAYER_TYPE_NONE, null)
                mContinueFABShadow.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        })

        mContinueFABContainer.startAnimation(mAnimations)
    }

    private fun showFABAnimation() {
        mContinueFAB.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        mContinueFABShadow.setLayerType(View.LAYER_TYPE_HARDWARE, null)


        val mTranslationAnimation: Animation =
            TranslateAnimation(0f, 0f, (-1 * Service.getScreenHeight(this)).toFloat(), 0f)
        mTranslationAnimation.fillAfter = true
        mTranslationAnimation.interpolator = OvershootInterpolator(0.7f)

        val mAnimations = AnimationSet(false)
        mAnimations.setFillAfter(true)
        mAnimations.addAnimation(mTranslationAnimation)
        mAnimations.setDuration(showWebviewAnimationDuration.toLong())
        mAnimations.setAnimationListener(object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                mContinueFAB.setLayerType(View.LAYER_TYPE_NONE, null)
                mContinueFABShadow.setLayerType(View.LAYER_TYPE_NONE, null)
                mContinueFABContainer.isClickable = true
            }
        })

        mContinueFABContainer.startAnimation(mAnimations)
    }

    companion object {
        private var subscriptionsTask: GetFollowsFromDB? = null
        private var toTransition = false
        private var isPartOfSetup = true
        fun loadingFollows(): Boolean {
            return subscriptionsTask == null || !subscriptionsTask!!.isFinished
        }
    }
}
