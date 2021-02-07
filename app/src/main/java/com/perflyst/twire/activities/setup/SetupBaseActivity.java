package com.perflyst.twire.activities.setup;

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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.perflyst.twire.R;
import com.perflyst.twire.utils.AnimationListenerAdapter;

public class SetupBaseActivity extends AppCompatActivity {
    final int SHOW_CONTINUE_ICON_DURATION = 650;

    protected AnimationSet hideViewAnimation(final View view, final int DURATION) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        final Animation mScaleAnimation = new ScaleAnimation(1, 0, 1, 0,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mScaleAnimation.setDuration(DURATION);
        mScaleAnimation.setInterpolator(new OvershootInterpolator(0.7f));

        final Animation mAlphaAnimation = new AlphaAnimation(1f, 0f);
        mAlphaAnimation.setDuration(DURATION / 2);
        mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
        mAlphaAnimation.setAnimationListener(new AnimationListenerAdapter() {
            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.INVISIBLE);
            }
        });

        final AnimationSet mViewAnimations = new AnimationSet(false);
        mViewAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
        mViewAnimations.setFillBefore(true);
        mViewAnimations.setFillAfter(true);
        mViewAnimations.addAnimation(mScaleAnimation);
        mViewAnimations.addAnimation(mAlphaAnimation);
        mViewAnimations.setAnimationListener(new AnimationListenerAdapter() {
            @Override
            public void onAnimationEnd(Animation animation) {
                view.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });

        view.startAnimation(mViewAnimations);
        return mViewAnimations;
    }

    protected void showTextLineAnimations(final TextView mTextLine, final int lineNumber) {
        mTextLine.setLayerType(View.LAYER_TYPE_HARDWARE, null);

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
                mTextLine.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mTextLine.setLayerType(View.LAYER_TYPE_NONE, null);
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

    protected void showLogoAnimations(final ImageView mGearIcon) {
        mGearIcon.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        final Animation mScaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        int SHOW_LOGO_ANIMATION_DURATION = 600;
        mScaleAnimation.setDuration(SHOW_LOGO_ANIMATION_DURATION);
        mScaleAnimation.setInterpolator(new OvershootInterpolator(0.7f));

        final Animation mAlphaAnimation = new AlphaAnimation(0f, 1f);
        mAlphaAnimation.setDuration(SHOW_LOGO_ANIMATION_DURATION);
        mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
        mAlphaAnimation.setAnimationListener(new AnimationListenerAdapter() {
            @Override
            public void onAnimationStart(Animation animation) {
                mGearIcon.setVisibility(View.VISIBLE);
            }
        });

        final RotateAnimation mRotateAnimation = new RotateAnimation(
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
        mLogoAnimations.setAnimationListener(new AnimationListenerAdapter() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mGearIcon.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });

        mGearIcon.startAnimation(mLogoAnimations);
    }

    protected void hideContinueIconAnimations(final ImageView mContinueIcon) {
        final Animation mScaleAnimation = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        final Animation mRotateAnimation = new RotateAnimation(
                mContinueIcon.getRotation(), 360 - mContinueIcon.getRotation(),
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        mRotateAnimation.setRepeatCount(0);
        mRotateAnimation.setAnimationListener(new AnimationListenerAdapter() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mContinueIcon.setVisibility(View.INVISIBLE);
            }
        });
        final AnimationSet mAnimations = new AnimationSet(true);
        mAnimations.setDuration(SHOW_CONTINUE_ICON_DURATION);
        mAnimations.setFillAfter(true);
        mAnimations.setInterpolator(new OvershootInterpolator(1.5f));
        mAnimations.addAnimation(mScaleAnimation);
        mAnimations.addAnimation(mRotateAnimation);

        mContinueIcon.startAnimation(mAnimations);

    }
}
