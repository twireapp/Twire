package com.perflyst.twire.activities.setup;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.perflyst.twire.R;
import com.perflyst.twire.service.Service;
import com.rey.material.widget.ProgressView;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

public class ConfirmSetupActivity extends AppCompatActivity {
    private final int SHOW_CONTINUE_ICON_DURATION = 650;
    private final int REVEAL_ANIMATION_DURATION = 650;
    private final int REVEAL_ANIMATION_DELAY = 200;
    private final SupportAnimator transitionAnimationWhite = null;
    private boolean hasTransitioned = false;
    private ImageView mGearIcon,
            mContinueIcon;
    private ProgressView mSetupProgress;
    private TextView mLoginTextLineOne,
            mLoginTextLineTwo;
    private View mContinueFAB,
            mContinueFABShadow,
            mTransitionViewWhite;
    private FrameLayout mContinueFABContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_setup);

        RelativeLayout mLoginTextContainer = findViewById(R.id.login_text_container);
        mSetupProgress = findViewById(R.id.SetupProgress);
        mContinueFABContainer = findViewById(R.id.login_continue_circle_container);
        mGearIcon = findViewById(R.id.login_icon);
        mContinueIcon = findViewById(R.id.forward_arrow);
        mLoginTextLineOne = findViewById(R.id.login_text_line_one);
        mLoginTextLineTwo = findViewById(R.id.login_text_line_two);
        mContinueFAB = findViewById(R.id.login_continue_circle);
        mContinueFABShadow = findViewById(R.id.login_continue_circle_shadow);
        mTransitionViewWhite = findViewById(R.id.transition_view_blue);

        mContinueIcon.setVisibility(View.INVISIBLE);
        mLoginTextLineOne.setVisibility(View.INVISIBLE);
        mLoginTextLineTwo.setVisibility(View.INVISIBLE);
        mGearIcon.setVisibility(View.INVISIBLE);
        mTransitionViewWhite.setVisibility(View.INVISIBLE);

        float textPosition = (int) (2.5 * (Service.getScreenHeight(getBaseContext()) / 5));
        mLoginTextContainer.setY(textPosition);

        mContinueFABContainer.bringToFront();
        mLoginTextLineOne.bringToFront();
        mLoginTextLineTwo.bringToFront();
        Service.bringToBack(mTransitionViewWhite);

        showLogoAnimations();
        showTextLineAnimations(mLoginTextLineOne, 1);
        showTextLineAnimations(mLoginTextLineTwo, 2);

        CheckDataFetchingTask checkingTask = new CheckDataFetchingTask();
        checkingTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (transitionAnimationWhite != null && hasTransitioned) {
            showReverseTransitionAnimation();
            hasTransitioned = false;
        }
    }

    @Override
    public void onBackPressed() {
        hideAllViews().setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ConfirmSetupActivity.super.onBackPressed();
                // We don't want a transition when going back. The activities handle the animation themselves.
                overridePendingTransition(0, 0);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

    }


    private void navigateToNextActivity() {
        hasTransitioned = true;
        this.startActivity(Service.getLoggedInIntent(getBaseContext()));
        this.overridePendingTransition(0, 0);
    }

    /**
     * Animations here from and down
     */

    private AnimationSet hideAllViews() {
        if (mContinueIcon.getVisibility() == View.VISIBLE) {
            hideContinueIconAnimations();
        }
        int HIDE_VIEW_ANIMATION_DURATION = 550;
        hideViewAnimation(mGearIcon, HIDE_VIEW_ANIMATION_DURATION);
        hideViewAnimation(mLoginTextLineOne, HIDE_VIEW_ANIMATION_DURATION);
        hideViewAnimation(mLoginTextLineTwo, HIDE_VIEW_ANIMATION_DURATION);
        hideViewAnimation(mSetupProgress, HIDE_VIEW_ANIMATION_DURATION);

        return hideViewAnimation(mLoginTextLineTwo, HIDE_VIEW_ANIMATION_DURATION);
    }

    private AnimationSet hideViewAnimation(final View view, final int DURATION) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        Animation mScaleAnimation = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mScaleAnimation.setDuration(DURATION);
        mScaleAnimation.setInterpolator(new OvershootInterpolator(0.7f));

        Animation mAlphaAnimation = new AlphaAnimation(1f, 0f);
        mAlphaAnimation.setDuration(DURATION / 2);
        mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
        mAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        final AnimationSet mViewAnimations = new AnimationSet(false);
        mViewAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
        mViewAnimations.setFillBefore(true);
        mViewAnimations.setFillAfter(true);
        mViewAnimations.addAnimation(mScaleAnimation);
        mViewAnimations.addAnimation(mAlphaAnimation);

        mViewAnimations.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setLayerType(View.LAYER_TYPE_NONE, null);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        view.startAnimation(mViewAnimations);
        return mViewAnimations;
    }

    private void showTextLineAnimations(final TextView mTextLine, int lineNumber) {
        mTextLine.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        int travelDistance = (lineNumber < 3)
                ? (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                getResources().getDimension(R.dimen.welcome_text_line_three_size),
                getResources().getDisplayMetrics())
                : 0;

        float overshoot = (lineNumber == 1) ? 2f : 1f;
        final Animation mTranslationAnimation = new TranslateAnimation(0, 0, travelDistance, 0);
        mTranslationAnimation.setInterpolator(new OvershootInterpolator(overshoot));

        final Animation mAlphaAnimation = new AlphaAnimation(0f, 1f);
        mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
        mAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mTextLine.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mTextLine.setLayerType(View.LAYER_TYPE_NONE, null);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        final AnimationSet mWelcomeTextAnimations = new AnimationSet(false);
        int SHOW_TEXT_ANIMATION_DURATION = 600;
        mWelcomeTextAnimations.setDuration(SHOW_TEXT_ANIMATION_DURATION);
        mWelcomeTextAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
        mWelcomeTextAnimations.setFillBefore(true);
        mWelcomeTextAnimations.setFillAfter(true);
        mWelcomeTextAnimations.addAnimation(mAlphaAnimation);
        mWelcomeTextAnimations.addAnimation(mTranslationAnimation);

        int SHOW_TEXT_ANIMATION_BASE_DELAY = 105;
        int delay = SHOW_TEXT_ANIMATION_BASE_DELAY * (lineNumber < 3 ? lineNumber : lineNumber * 2);
        int SHOW_TEXT_ANIMATION_DELAY = 105;
        new Handler().postDelayed(() -> mTextLine.startAnimation(mWelcomeTextAnimations), delay + SHOW_TEXT_ANIMATION_DELAY);

    }

    private void showLogoAnimations() {
        mGearIcon.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        Animation mScaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        int SHOW_LOGO_ANIMATION_DURATION = 600;
        mScaleAnimation.setDuration(SHOW_LOGO_ANIMATION_DURATION);
        mScaleAnimation.setInterpolator(new OvershootInterpolator(0.7f));

        Animation mAlphaAnimation = new AlphaAnimation(0f, 1f);
        mAlphaAnimation.setDuration(SHOW_LOGO_ANIMATION_DURATION);
        mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
        mAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mGearIcon.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        RotateAnimation mRotateAnimation = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        mRotateAnimation.setInterpolator(new LinearInterpolator());
        // The time it takes for the icon to rotate 360 degrees. The Higher the slower.
        int LOGO_ROTATION_SPEED = 15 * 1000;
        mRotateAnimation.setDuration(LOGO_ROTATION_SPEED);
        mRotateAnimation.setRepeatCount(Animation.INFINITE);

        final AnimationSet mLogoAnimations = new AnimationSet(false);
        mLogoAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
        mLogoAnimations.setFillBefore(true);
        mLogoAnimations.setFillAfter(true);
        mLogoAnimations.addAnimation(mScaleAnimation);
        mLogoAnimations.addAnimation(mRotateAnimation);
        mLogoAnimations.addAnimation(mAlphaAnimation);

        mLogoAnimations.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mGearIcon.setLayerType(View.LAYER_TYPE_NONE, null);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        mGearIcon.startAnimation(mLogoAnimations);
    }

    private SupportAnimator showTransitionAnimation() {
        // Get the center for the FAB
        int cx = (int) mContinueFABContainer.getX() + mContinueFABContainer.getMeasuredHeight() / 2;
        int cy = (int) mContinueFABContainer.getY() + mContinueFABContainer.getMeasuredWidth() / 2;

        // get the final radius for the clipping circle
        int dx = Math.max(cx, mTransitionViewWhite.getWidth() - cx);
        int dy = Math.max(cy, mTransitionViewWhite.getHeight() - cy);
        float finalRadius = (float) Math.hypot(dx, dy);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mTransitionViewWhite.isAttachedToWindow();
        }

        final SupportAnimator blueTransitionAnimation =
                ViewAnimationUtils.createCircularReveal(mTransitionViewWhite, cx, cy, 0, finalRadius);
        blueTransitionAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        blueTransitionAnimation.setDuration(REVEAL_ANIMATION_DURATION);
        blueTransitionAnimation.addListener(new SupportAnimator.AnimatorListener() {
            @Override
            public void onAnimationStart() {
                //mTransitionViewWhite.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                mTransitionViewWhite.setVisibility(View.VISIBLE);
                mTransitionViewWhite.bringToFront();
                mContinueFABShadow.bringToFront();
                mContinueFAB.bringToFront();
            }

            @Override
            public void onAnimationEnd() {
                //mTransitionViewWhite.setLayerType(View.LAYER_TYPE_NONE, null);
            }

            @Override
            public void onAnimationCancel() {

            }

            @Override
            public void onAnimationRepeat() {

            }
        });

        new Handler().postDelayed(blueTransitionAnimation::start, REVEAL_ANIMATION_DELAY);

        return blueTransitionAnimation;
    }

    private void showReverseTransitionAnimation() {
        mTransitionViewWhite.setLayerType(View.LAYER_TYPE_HARDWARE, null);

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
                mTransitionViewWhite.setLayerType(View.LAYER_TYPE_NONE, null);
                mTransitionViewWhite.setLayerType(View.LAYER_TYPE_NONE, null);
                mTransitionViewWhite.setVisibility(View.INVISIBLE);
                mContinueFABContainer.setClickable(true);

                mContinueIcon.bringToFront();
                mContinueIcon.setVisibility(View.VISIBLE);
                showContinueIconAnimations();
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

    private void showContinueIconAnimations() {
        mContinueIcon.setVisibility(View.VISIBLE);
        Animation mScaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        Animation mRotateAnimation = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        mRotateAnimation.setRepeatCount(0);
        mScaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mContinueIcon.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        AnimationSet mAnimations = new AnimationSet(true);
        mAnimations.setDuration(SHOW_CONTINUE_ICON_DURATION);
        mAnimations.setFillAfter(true);
        mAnimations.setInterpolator(new OvershootInterpolator(1.5f));
        mAnimations.addAnimation(mScaleAnimation);
        mAnimations.addAnimation(mRotateAnimation);

        mContinueIcon.startAnimation(mAnimations);


    }

    private void hideContinueIconAnimations() {
        Animation mScaleAnimation = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        Animation mRotateAnimation = new RotateAnimation(
                mContinueIcon.getRotation(), 360 - mContinueIcon.getRotation(),
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        mRotateAnimation.setRepeatCount(0);
        mRotateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mContinueIcon.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        AnimationSet mAnimations = new AnimationSet(true);
        mAnimations.setDuration(SHOW_CONTINUE_ICON_DURATION);
        mAnimations.setFillAfter(true);
        mAnimations.setInterpolator(new OvershootInterpolator(1.5f));
        mAnimations.addAnimation(mScaleAnimation);
        mAnimations.addAnimation(mRotateAnimation);

        mContinueIcon.startAnimation(mAnimations);

    }

    private class CheckDataFetchingTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            while (LoginActivity.loadingFollows()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected void onPostExecute(Void voi) {
            SupportAnimator.AnimatorListener animator = new SupportAnimator.AnimatorListener() {
                @Override
                public void onAnimationEnd() {
                    navigateToNextActivity();
                }

                @Override
                public void onAnimationStart() {
                }

                @Override
                public void onAnimationCancel() {
                }

                @Override
                public void onAnimationRepeat() {
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !mTransitionViewWhite.isAttachedToWindow()) {
                animator.onAnimationEnd();
                return;
            }

            showTransitionAnimation().addListener(animator);
        }
    }


}
