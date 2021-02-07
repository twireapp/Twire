package com.perflyst.twire.activities.setup;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.perflyst.twire.R;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.utils.AnimationListenerAdapter;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;


public class WelcomeActivity extends SetupBaseActivity {
    final int REVEAL_ANIMATION_DURATION = 650;
    final int REVEAL_ANIMATION_DELAY = 200;
    final int ANIMATIONS_START_DELAY = 500;
    final int LOGO_ANIMATION_DURATION = 1000;
    final int LOGO_Container_ANIMATION_DURATION = 1750;
    final int WELCOME_TEXT_ANIMATION_DURATION = 900;
    final int WELCOME_TEXT_ANIMATION_BASE_DELAY = 175;
    final int CONTINUE_FAB_ANIMATION_DURATION = 750;
    private final String LOG_TAG = getClass().getSimpleName();
    private boolean hasTransitioned = false;
    private SupportAnimator transitionAnimationWhite = null;
    private SupportAnimator transitionAnimationBlue = null;
    private TextView mWelcomeTextLineOne,
            mWelcomeTextLineTwo;
    private ImageView mLogo,
            mContinueIcon;
    private View mLogoCenter,
            mContinueFAB,
            mContinueFABShadow,
            mTransitionViewWhite,
            mTransitionViewBlue;
    private FrameLayout mLogoContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        RelativeLayout mWelcomeText = findViewById(R.id.welcome_text);
        mWelcomeTextLineOne = findViewById(R.id.welcome_text_line_one);
        mWelcomeTextLineTwo = findViewById(R.id.welcome_text_line_two);

        mLogo = findViewById(R.id.welcome_icon);
        mContinueIcon = findViewById(R.id.forward_arrow);
        mLogoContainer = findViewById(R.id.welcome_icon_layout);
        mLogoCenter = findViewById(R.id.welcome_icon_center);
        mContinueFAB = findViewById(R.id.continue_circle);
        mContinueFABShadow = findViewById(R.id.welcome_continue_circle_shadow);
        mTransitionViewWhite = findViewById(R.id.transition_view);
        mTransitionViewBlue = findViewById(R.id.transition_view_blue);

        mTransitionViewBlue.setVisibility(View.INVISIBLE);
        mTransitionViewWhite.setVisibility(View.INVISIBLE);
        mWelcomeTextLineOne.setVisibility(View.INVISIBLE);
        mWelcomeTextLineTwo.setVisibility(View.INVISIBLE);
        mLogo.setVisibility(View.INVISIBLE);
        mLogoContainer.setVisibility(View.INVISIBLE);
        mContinueFAB.setVisibility(View.INVISIBLE);
        mContinueIcon.setVisibility(View.INVISIBLE);


        Service.bringToBack(mTransitionViewWhite);
        Service.bringToBack(mTransitionViewBlue);

        // Change the position of the WelcomeText. Doing it this way is more dynamic, instead of a fixed
        // DP length from the bottom
        int yPosition = (int) (2.5 * (Service.getScreenHeight(this) / 5));
        mWelcomeText.setY(yPosition);

        // Start the animations. Make sure the animations that in the correct order,
        // by adding Animation Listeners that start the next animation on animation end.
        new Handler().postDelayed(() -> {
            startLogoContainerAnimations().setAnimationListener(new AnimationListenerAdapter() {
                @Override
                public void onAnimationStart(Animation animation) {
                    mLogoContainer.setVisibility(View.VISIBLE);
                }
            });
            startLogoOuterAnimations().setAnimationListener(new AnimationListenerAdapter() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    startWelcomeTextLineAnimations(mWelcomeTextLineOne, 1);
                    startWelcomeTextLineAnimations(mWelcomeTextLineTwo, 2).setAnimationListener(new AnimationListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            startContinueFABAnimations();
                        }
                    });
                }
            });
        }, ANIMATIONS_START_DELAY);


        mContinueFAB.setOnClickListener(v -> {
            // Get the center for the FAB
            int cx = (int) mContinueFAB.getX() + mContinueFAB.getMeasuredHeight() / 2;
            int cy = (int) mContinueFAB.getY() + mContinueFAB.getMeasuredWidth() / 2;

            // get the final radius for the clipping circle
            int dx = Math.max(cx, mTransitionViewWhite.getWidth() - cx);
            int dy = Math.max(cy, mTransitionViewWhite.getHeight() - cy);
            float finalRadius = (float) Math.hypot(dx, dy);

            final SupportAnimator whiteTransitionAnimation =
                    ViewAnimationUtils.createCircularReveal(mTransitionViewWhite, cx, cy, 0, finalRadius);
            whiteTransitionAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            whiteTransitionAnimation.setDuration(REVEAL_ANIMATION_DURATION);
            whiteTransitionAnimation.addListener(new SupportAnimator.AnimatorListener() {
                @Override
                public void onAnimationStart() {
                    mTransitionViewWhite.bringToFront();
                    mTransitionViewWhite.setVisibility(View.VISIBLE);
                    mContinueFAB.setClickable(false);
                    startHideContinueIconAnimations();
                }

                @Override
                public void onAnimationEnd() {
                    transitionAnimationWhite = whiteTransitionAnimation;
                }

                @Override
                public void onAnimationCancel() {
                    onAnimationEnd();
                }

                @Override
                public void onAnimationRepeat() {

                }
            });


            final SupportAnimator blueTransitionAnimation =
                    ViewAnimationUtils.createCircularReveal(mTransitionViewBlue, cx, cy, 0, finalRadius);
            blueTransitionAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            blueTransitionAnimation.setDuration(REVEAL_ANIMATION_DURATION);
            blueTransitionAnimation.addListener(new SupportAnimator.AnimatorListener() {
                @Override
                public void onAnimationStart() {
                    mTransitionViewBlue.setVisibility(View.VISIBLE);
                    mTransitionViewBlue.bringToFront();
                    mContinueFABShadow.bringToFront();
                    mContinueFAB.bringToFront();
                }

                @Override
                public void onAnimationEnd() {
                    transitionAnimationBlue = blueTransitionAnimation;
                }

                @Override
                public void onAnimationCancel() {
                    onAnimationEnd();
                }

                @Override
                public void onAnimationRepeat() {

                }
            });

            whiteTransitionAnimation.start();
            blueTransitionAnimation.setStartDelay(REVEAL_ANIMATION_DELAY);
            blueTransitionAnimation.start();

            new Handler().postDelayed(() -> {
                Log.d(LOG_TAG, "Navigating To Login Activity");
                navigateToLoginActivity();
            }, REVEAL_ANIMATION_DELAY + REVEAL_ANIMATION_DURATION);

        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // The user has returned from the login screen. Lol wtf?
        if (transitionAnimationWhite != null && hasTransitioned) {
            final SupportAnimator blueReversed = transitionAnimationBlue.reverse();
            blueReversed.setInterpolator(new AccelerateDecelerateInterpolator());
            blueReversed.addListener(new SupportAnimator.AnimatorListener() {
                @Override
                public void onAnimationStart() {
                    mTransitionViewBlue.setVisibility(View.VISIBLE);
                    mTransitionViewBlue.bringToFront();
                }

                @Override
                public void onAnimationEnd() {
                    Service.bringToBack(mTransitionViewBlue);
                    mTransitionViewBlue.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationCancel() {

                }

                @Override
                public void onAnimationRepeat() {

                }
            });
            blueReversed.setDuration(REVEAL_ANIMATION_DURATION);
            blueReversed.start();

            final SupportAnimator whiteReversed = transitionAnimationWhite.reverse();
            whiteReversed.setInterpolator(new AccelerateDecelerateInterpolator());
            whiteReversed.addListener(new SupportAnimator.AnimatorListener() {
                @Override
                public void onAnimationStart() {
                    mTransitionViewWhite.setVisibility(View.VISIBLE);
                    mTransitionViewWhite.bringToFront();
                }

                @Override
                public void onAnimationEnd() {
                    Service.bringToBack(mTransitionViewWhite);
                    mTransitionViewWhite.setVisibility(View.INVISIBLE);
                    mContinueFAB.setClickable(true);
                    startShowContinueIconAnimations();
                }

                @Override
                public void onAnimationCancel() {

                }

                @Override
                public void onAnimationRepeat() {

                }
            });
            whiteReversed.setDuration(REVEAL_ANIMATION_DURATION);
            new Handler().postDelayed(whiteReversed::start, REVEAL_ANIMATION_DELAY);
            hasTransitioned = false;
        }
    }

    private void navigateToLoginActivity() {
        // Go to the login activity, with no transition.
        hasTransitioned = true;
        Intent loginActivityIntent = new Intent(getBaseContext(), LoginActivity.class);
        loginActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(loginActivityIntent, null);
    }

    /**
     * Animations for the views in this activity
     */

    private AnimationSet startLogoContainerAnimations() {
        mLogoCenter.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        AnimationSet mInitLogoAnimations = new AnimationSet(true);
        TranslateAnimation trans = new TranslateAnimation(0, 0, Service.getScreenHeight(this), 0);

        mInitLogoAnimations.setDuration(LOGO_Container_ANIMATION_DURATION);
        mInitLogoAnimations.setFillAfter(true);
        mInitLogoAnimations.setInterpolator(new OvershootInterpolator(0.7f));
        mInitLogoAnimations.addAnimation(trans);
        mInitLogoAnimations.setAnimationListener(new AnimationListenerAdapter() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mLogoCenter.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });

        mLogoContainer.startAnimation(mInitLogoAnimations);
        return mInitLogoAnimations;
    }

    private AnimationSet startLogoOuterAnimations() {
        mLogo.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        Animation mScaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mScaleAnimation.setDuration(LOGO_ANIMATION_DURATION);
        mScaleAnimation.setInterpolator(new OvershootInterpolator(0.7f));

        Animation mAlphaAnimation = new AlphaAnimation(0f, 1f);
        mAlphaAnimation.setDuration(LOGO_ANIMATION_DURATION);
        mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
        mAlphaAnimation.setAnimationListener(new AnimationListenerAdapter() {
            @Override
            public void onAnimationStart(Animation animation) {
                mLogo.setVisibility(View.VISIBLE);
            }
        });

        RotateAnimation mRotateAnimation = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        mRotateAnimation.setInterpolator(new DecelerateInterpolator());
        mRotateAnimation.setDuration(LOGO_ANIMATION_DURATION);
        mRotateAnimation.setRepeatCount(0);

        final AnimationSet mLogoAnimations = new AnimationSet(false);
        mLogoAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
        mLogoAnimations.setFillBefore(true);
        mLogoAnimations.setFillAfter(true);
        mLogoAnimations.addAnimation(mScaleAnimation);
        mLogoAnimations.addAnimation(mRotateAnimation);
        mLogoAnimations.addAnimation(mAlphaAnimation);

        mLogoAnimations.setAnimationListener(new AnimationListenerAdapter() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mLogo.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });

        new Handler().postDelayed(() -> mLogo.startAnimation(mLogoAnimations), LOGO_Container_ANIMATION_DURATION - LOGO_ANIMATION_DURATION);

        return mLogoAnimations;
    }

    private AnimationSet startWelcomeTextLineAnimations(final TextView mWelcomeTextLine, int lineNumber) {
        mWelcomeTextLine.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        int travelDistance = lineNumber < 3
                ? (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                getResources().getDimension(R.dimen.welcome_text_line_three_size),
                getResources().getDisplayMetrics())
                : 0;

        float overshoot = lineNumber == 1 ? 2f : 1f;
        final Animation mTranslationAnimation = new TranslateAnimation(0, 0, travelDistance, 0);
        mTranslationAnimation.setInterpolator(new OvershootInterpolator(overshoot));

        final Animation mAlphaAnimation = new AlphaAnimation(0f, 1f);
        mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
        mAlphaAnimation.setAnimationListener(new AnimationListenerAdapter() {
            @Override
            public void onAnimationStart(Animation animation) {
                mWelcomeTextLine.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mWelcomeTextLine.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });


        final AnimationSet mWelcomeTextAnimations = new AnimationSet(false);
        mWelcomeTextAnimations.setDuration(WELCOME_TEXT_ANIMATION_DURATION);
        mWelcomeTextAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
        mWelcomeTextAnimations.setFillBefore(true);
        mWelcomeTextAnimations.setFillAfter(true);
        mWelcomeTextAnimations.addAnimation(mAlphaAnimation);
        mWelcomeTextAnimations.addAnimation(mTranslationAnimation);

        int delay = WELCOME_TEXT_ANIMATION_BASE_DELAY * (lineNumber < 3 ? lineNumber : lineNumber * 2);
        new Handler().postDelayed(() -> mWelcomeTextLine.startAnimation(mWelcomeTextAnimations), delay);

        return mWelcomeTextAnimations;
    }

    private void startContinueFABAnimations() {
        mContinueFAB.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mContinueFABShadow.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        int travelDistance = Service.dpToPixels(getBaseContext(), getResources().getDimension(R.dimen.welcome_continue_circle_diameter));

        final Animation mTranslationAnimation = new TranslateAnimation(0, 0, travelDistance, 0);

        final AnimationSet mContinueFABAnimations = new AnimationSet(true);
        mContinueFABAnimations.setDuration(CONTINUE_FAB_ANIMATION_DURATION);
        mContinueFABAnimations.setInterpolator(new OvershootInterpolator(1f));
        mContinueFABAnimations.addAnimation(mTranslationAnimation);
        mContinueFABAnimations.setAnimationListener(new AnimationListenerAdapter() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Start running the show animation for the FAB icon a third into this animation
                mContinueFAB.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    mContinueIcon.setVisibility(View.VISIBLE);
                    startShowContinueIconAnimations();
                }, CONTINUE_FAB_ANIMATION_DURATION / 3);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mContinueFAB.setLayerType(View.LAYER_TYPE_NONE, null);
                mContinueFABShadow.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });

        mContinueFAB.startAnimation(mContinueFABAnimations);
        mContinueFABShadow.startAnimation(mContinueFABAnimations);
    }

    private void startShowContinueIconAnimations() {
        final Animation mScaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        startContinueIconAnimations(mScaleAnimation);
    }

    private void startHideContinueIconAnimations() {
        final Animation mScaleAnimation = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        startContinueIconAnimations(mScaleAnimation);
    }

    private void startContinueIconAnimations(final Animation mScaleAnimation) {
        final Animation mRotateAnimation = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        mRotateAnimation.setRepeatCount(0);

        final AnimationSet mAnimations = new AnimationSet(true);
        mAnimations.setDuration(REVEAL_ANIMATION_DURATION);
        mAnimations.setFillAfter(true);
        mAnimations.setInterpolator(new OvershootInterpolator(1.5f));
        mAnimations.addAnimation(mScaleAnimation);
        mAnimations.addAnimation(mRotateAnimation);

        mContinueIcon.startAnimation(mAnimations);
    }
}
