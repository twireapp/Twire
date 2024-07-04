package com.perflyst.twire.activities.stream;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.fragments.ChatFragment;
import com.perflyst.twire.fragments.StreamFragment;
import com.perflyst.twire.service.Settings;

import java.util.List;
import java.util.Set;

import timber.log.Timber;

public abstract class StreamActivity extends ThemeActivity implements StreamFragment.StreamFragmentListener {
    public StreamFragment mStreamFragment;
    public ChatFragment mChatFragment;
    private boolean mBackstackLost;
    private boolean onStopCalled;
    private int initialOrientation;

    protected abstract int getLayoutResource();

    protected abstract int getVideoContainerResource();

    protected abstract Bundle getStreamArguments();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResource());

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.black));
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        initialOrientation = getResources().getConfiguration().orientation;

        if (savedInstanceState == null) {
            FragmentManager fm = getSupportFragmentManager();

            getWindow().setEnterTransition(constructTransitions());
            getWindow().setReturnTransition(constructTransitions());

            // If the Fragment is non-null, then it is currently being
            // retained across a configuration change.
            if (mChatFragment == null) {
                mChatFragment = ChatFragment.getInstance(getStreamArguments());
                fm.beginTransaction().replace(R.id.chat_fragment, mChatFragment).commit();
            }

            if (mStreamFragment == null) {
                mStreamFragment = StreamFragment.newInstance(getStreamArguments());
                fm.beginTransaction().replace(getVideoContainerResource(), mStreamFragment, getString(R.string.stream_fragment_tag)).commit();
            }
        }

        updateOrientation();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateOrientation();
    }

    protected void resetStream() {
        FragmentManager fm = getSupportFragmentManager();
        mStreamFragment = StreamFragment.newInstance(getStreamArguments());
        fm.beginTransaction().replace(getVideoContainerResource(), mStreamFragment).commit();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (mChatFragment == null || !mChatFragment.notifyBackPressed()) {
            return;
        }

        // Eww >(
        if (mStreamFragment != null) {
            boolean isCurrentlyLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            boolean wasInitiallyLandscape = initialOrientation == Configuration.ORIENTATION_LANDSCAPE;
            if (isCurrentlyLandscape && !wasInitiallyLandscape) {
                mStreamFragment.toggleFullscreen();
            } else if (mStreamFragment.chatOnlyViewVisible) {
                this.finish();
                this.overrideTransition();
            } else {
                super.onBackPressed();
                try {
                    mStreamFragment.backPressed();
                } catch (NullPointerException e) {
                    Timber.e(e);
                }
                this.overrideTransition();
            }
        } else {
            super.onBackPressed();
            this.overrideTransition();
        }
    }

    @Override
    @RequiresApi(24)
    public void onUserLeaveHint() {
        super.onUserLeaveHint();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return;
        }

        if (mStreamFragment.getPlayWhenReady() && getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            enterPictureInPictureMode();
        }
    }

    private TransitionSet constructTransitions() {
        int[] slideTargets = {R.id.ChatRecyclerView, R.id.chat_input, R.id.chat_input_divider};

        Transition slideTransition = new Slide(Gravity.BOTTOM);
        Transition fadeTransition = new Fade();

        for (int slideTarget : slideTargets) {
            slideTransition.addTarget(slideTarget);
            fadeTransition.excludeTarget(slideTarget, true);
        }

        TransitionSet set = new TransitionSet();
        set.addTransition(slideTransition);
        set.addTransition(fadeTransition);
        return set;
    }

    private void overrideTransition() {
        this.overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_bottom_anim);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stream, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {// Call the super method as we also want the user to go all the way back to last mActivity if the user is in full screen mode
            if (mStreamFragment != null) {
                if (!mStreamFragment.isVideoInterfaceShowing()) {
                    return false;
                }

                if (mStreamFragment.chatOnlyViewVisible) {
                    finish();
                } else {
                    super.onBackPressed();
                    mStreamFragment.backPressed();
                }
                overrideTransition();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        if (fragment instanceof StreamFragment) {
            StreamFragment streamFragment = (StreamFragment) fragment;
            streamFragment.streamFragmentCallback = this;
        }

        if (mChatFragment == null && fragment instanceof ChatFragment)
            mChatFragment = (ChatFragment) fragment;

        if (mStreamFragment == null && fragment instanceof StreamFragment)
            mStreamFragment = (StreamFragment) fragment;
    }

    @Override
    public void onSeek() {
        mChatFragment.clearMessages();
    }

    @Override
    public void refreshLayout() {
        updateOrientation();
    }

    public View getMainContentLayout() {
        return findViewById(R.id.main_content);
    }

    void updateOrientation() {
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        View chat = findViewById(R.id.chat_fragment);
        if (landscape) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) findViewById(R.id.chat_landscape_fragment).getLayoutParams();
            lp.width = (int) (StreamFragment.getScreenRect(this).height() * (Settings.getChatLandscapeWidth() / 100.0));
            Timber.d("TARGET WIDTH: %s", lp.width);
            chat.setLayoutParams(lp);
        } else {
            chat.setLayoutParams(findViewById(R.id.chat_placement_wrapper).getLayoutParams());
        }

        ViewGroup.LayoutParams layoutParams = findViewById(getVideoContainerResource()).getLayoutParams();
        layoutParams.height = landscape ? MATCH_PARENT : WRAP_CONTENT;
    }

    @Override
    public void onStop() {
        super.onStop();
        onStopCalled = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        onStopCalled = false;
    }

    @Override
    public void onPictureInPictureModeChanged(boolean enabled, Configuration newConfig) {
        super.onPictureInPictureModeChanged(enabled, newConfig);
        mBackstackLost |= enabled;

        if (!enabled && onStopCalled) {
            finish();
        }
    }

    @Override
    public void finish() {
        if (mBackstackLost) {
            navToLauncherTask(getApplicationContext());
            finishAndRemoveTask();
        } else {
            super.finish();
        }
    }

    public void navToLauncherTask(@NonNull Context appContext) {
        ActivityManager activityManager = ContextCompat.getSystemService(appContext, ActivityManager.class);
        // iterate app tasks available and navigate to launcher task (browse task)
        if (activityManager != null) {
            final List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
            for (ActivityManager.AppTask task : appTasks) {
                final Intent baseIntent = task.getTaskInfo().baseIntent;
                final Set<String> categories = baseIntent.getCategories();
                if (categories != null && categories.contains(Intent.CATEGORY_LAUNCHER)) {
                    task.moveToFront();
                    return;
                }
            }
        }
    }
}
