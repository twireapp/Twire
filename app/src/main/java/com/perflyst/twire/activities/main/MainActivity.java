package com.perflyst.twire.activities.main;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.perflyst.twire.BuildConfig;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.adapters.StreamsAdapter;
import com.perflyst.twire.fragments.ChangelogDialogFragment;
import com.perflyst.twire.fragments.NavigationDrawerFragment;
import com.perflyst.twire.misc.TooltipWindow;
import com.perflyst.twire.misc.UniversalOnScrollListener;
import com.perflyst.twire.model.MainElement;
import com.perflyst.twire.service.AnimationService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.tasks.ScrollToStartPositionTask;
import com.perflyst.twire.utils.AnimationListenerAdapter;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.rey.material.widget.ProgressView;

import butterknife.BindView;
import butterknife.ButterKnife;


public abstract class MainActivity<E extends Comparable<E> & MainElement> extends ThemeActivity {
    private static final String FIRST_VISIBLE_ELEMENT_POSITION = "firstVisibleElementPosition";
    protected final String LOG_TAG = getClass().getSimpleName();

    @BindView(R.id.followed_channels_drawer_layout)
    protected DrawerLayout mDrawerLayout;

    @BindView(R.id.progress_view)
    protected ProgressView mProgressView;

    @BindView(R.id.swipe_container)
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    @BindView(R.id.main_list)
    protected AutoSpanRecyclerView mRecyclerView;

    @BindView(R.id.toolbar_shadow)
    protected View mToolbarShadow;

    @BindView(R.id.icon_container)
    protected View mCircleIconWrapper;

    @BindView(R.id.txt_title)
    protected TextView mTitleView;

    @BindView(R.id.error_view)
    protected TextView mErrorView;

    @BindView(R.id.emote_error_view)
    protected TextView mErrorEmoteView;

    @BindView(R.id.img_icon)
    protected ImageView mIcon;

    @BindView(R.id.main_toolbar)
    protected Toolbar mMainToolbar;

    @BindView(R.id.main_decorative_toolbar)
    protected Toolbar mDecorativeToolbar;
    protected MainActivityAdapter<E, ?> mAdapter;
    protected NavigationDrawerFragment mDrawerFragment;
    protected UniversalOnScrollListener mScrollListener;
    protected Settings settings;
    protected TooltipWindow mTooltip;
    // The position of the toolbars for the activity that started the transition to this activity
    private float fromToolbarPosition,
            fromMainToolbarPosition;
    private boolean isTransitioned = false;

    /**
     * Refreshes the content of the activity
     */
    public abstract void refreshElements();

    /**
     * Construct the adapter used for this activity's list
     */
    protected abstract MainActivityAdapter<E, ?> constructAdapter(AutoSpanRecyclerView recyclerView);

    /**
     * Get the drawable resource int used to represent this activity
     *
     * @return the resource int
     */
    protected abstract int getActivityIconRes();

    /***
     * Get the string resource int used for the title of this activity
     * @return the resource int
     */
    protected abstract int getActivityTitleRes();

    /***
     * Construct the AutoSpanBehaviour used for this main activity's AutoSpanRecyclerView
     */
    protected abstract AutoSpanBehaviour constructSpanBehaviour();

    /**
     * Allows the child class to specialize the functionality in the onCreate method, although it is not required.
     */
    protected void customizeActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.drawer_fragment);
        settings = new Settings(getBaseContext());

        initErrorView();
        initTitleAndIcon();

        setSupportActionBar(mMainToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");
        mMainToolbar.setPadding(0, 0, Service.dpToPixels(getBaseContext(), 5), 0); // to make sure the cast icon is aligned 16 dp from the right edge.
        mMainToolbar.bringToFront();
        mMainToolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
        mToolbarShadow.bringToFront();
        mToolbarShadow.setAlpha(0f);
        mTitleView.bringToFront();

        // Setup Drawer Fragment
        mDrawerFragment.setUp(findViewById(R.id.followed_channels_drawer_layout), mMainToolbar);

        // Set up the RecyclerView
        mRecyclerView.setBehaviour(constructSpanBehaviour());
        mAdapter = constructAdapter(mRecyclerView);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(null); // We want to implement our own animations
        mRecyclerView.setHasFixedSize(true);
        mScrollListener = new UniversalOnScrollListener(this, mMainToolbar, mDecorativeToolbar, mToolbarShadow, mCircleIconWrapper, mTitleView, LOG_TAG, true);
        mRecyclerView.addOnScrollListener(mScrollListener);

        // Only animate when the view is first started, not when screen rotates
        if (savedInstance == null) {
            mTitleView.setAlpha(0f);
            initActivityAnimation();
        }

        Service.increaseNavigationDrawerEdge(mDrawerLayout);

        checkForTip();
        checkForUpdate();
        customizeActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkIsBackFromMainActivity();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            // Check if the user has changed any size or style setting.
            if (!checkElementStyleChange()) {
                checkElementSizeChange();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mTooltip != null && mTooltip.isTooltipShown()) {
            mTooltip.dismissTooltip();
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        boolean isFromOtherMain = handleBackPressed();

        // If this activity was not started from another main activity, then just use the usual onBackPressed.
        if (!isFromOtherMain) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_activity, menu);

        return true;
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mAdapter.getItemCount() >= 0) {
            mRecyclerView.scrollToPosition(0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        int firstVisibleElement = mRecyclerView.getManager().findFirstCompletelyVisibleItemPosition() == 0
                ? mRecyclerView.getManager().findFirstVisibleItemPosition()
                : mRecyclerView.getManager().findFirstCompletelyVisibleItemPosition();

        outState.putInt(FIRST_VISIBLE_ELEMENT_POSITION, firstVisibleElement);
        super.onSaveInstanceState(outState);
    }

    /**
     * Initializes the error view, but does NOT find the view with ID's
     */
    protected void initErrorView() {
        if (mErrorView != null && mErrorEmoteView != null) {
            mErrorEmoteView.setText(Service.getErrorEmote());
            mErrorEmoteView.setAlpha(0f);
            mErrorView.setAlpha(0f);
        } else {
            throw new IllegalStateException("You need to find the views before you can use them");
        }
    }

    /***
     * Set the title and icon that is used to identify this activity and the content of its list
     */
    protected void initTitleAndIcon() {
        mIcon.setImageResource(getActivityIconRes());
        //mIcon.setImageDrawable(getResources().getDrawable());
        mTitleView.setText(getString(getActivityTitleRes()));
    }

    /**
     * Scrolls to the top of the recyclerview. When the position is reached refreshElements() is called
     */
    public void scrollToTopAndRefresh() {
        ScrollToStartPositionTask scrollTask = new ScrollToStartPositionTask(() -> {
            if (mRecyclerView != null && mAdapter != null) {
                refreshElements();
            }
        }, mRecyclerView, mScrollListener);
        scrollTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Shows the error views with an alpha animation
     */
    public void showErrorView() {
        if (mErrorView != null && mErrorEmoteView != null) {
            mErrorEmoteView.setVisibility(View.VISIBLE);
            mErrorView.setVisibility(View.VISIBLE);

            mErrorEmoteView.animate().alpha(1f).start();
            mErrorView.animate().alpha(1f).start();
        }
    }

    /**
     * Hide the error views with an alpha animation
     */
    public void hideErrorView() {
        if (mErrorView != null && mErrorEmoteView != null) {
            mErrorEmoteView.animate().alpha(0f).start();
            mErrorView.animate().alpha(0f).start();
        }
    }

    /**
     * Check if usability Tips should be shown to the user
     */
    private void checkForTip() {
        if (!settings.isTipsShown()) {
            try {
                mTooltip = new TooltipWindow(this, TooltipWindow.POSITION_TO_RIGHT);
                mMainToolbar.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        v.removeOnLayoutChangeListener(this);

                        if (!mTooltip.isTooltipShown()) {
                            final View anchor = Service.getNavButtonView(mMainToolbar);
                            if (anchor != null) {
                                anchor.addOnLayoutChangeListener(
                                        (v1, left1, top1, right1, bottom1,
                                         oldLeft1, oldTop1, oldRight1, oldBottom1) ->
                                                mTooltip.showToolTip(anchor, getString(R.string.tip_navigate)));
                            }
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to Show ToolTip");
            }

        }
    }

    private void checkForUpdate() {
        int versionCode = BuildConfig.VERSION_CODE;

        if (settings.getLastVersionCode() != versionCode && settings.getShowChangelogs()) {
            new ChangelogDialogFragment().show(getSupportFragmentManager(), "ChangelogDialog");
        }
    }

    /**
     * Checks if the user has changed the element style of this adapter type.
     * If it has Update the adapter element style and refresh the elements.
     */
    public boolean checkElementStyleChange() {
        String currentAdapterStyle = mAdapter.getElementStyle();
        String actualStyle = mAdapter.initElementStyle();

        if (!currentAdapterStyle.equals(actualStyle)) {
            mAdapter.setElementStyle(mAdapter.initElementStyle());
            scrollToTopAndRefresh();
            return true;
        } else {
            return false;
        }
    }

    public void checkElementSizeChange() {
        if (mRecyclerView.hasSizedChanged()) {
            scrollToTopAndRefresh();
        }
    }


    /**
     * Returns the activity's recyclerview.
     *
     * @return The Recyclerview
     */
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    /**
     * Decides which animation to run based the intent that started the activity.
     */
    public void initActivityAnimation() {
        Intent intent = this.getIntent();
        fromToolbarPosition = intent.getFloatExtra(
                this.getResources().getString(R.string.decorative_toolbar_position_y), -1
        );

        fromMainToolbarPosition = intent.getFloatExtra(
                this.getResources().getString(R.string.main_toolbar_position_y), -1
        );

        // If the position is equal to the default value,
        // then the intent was not put into from another MainActivity
        if (fromToolbarPosition != -1) {
            AnimationService.setActivityToolbarReset(mMainToolbar, mDecorativeToolbar, this, fromToolbarPosition, fromMainToolbarPosition);
        } else {
            AnimationService.setActivityToolbarCircularRevealAnimation(mDecorativeToolbar);
        }

        AnimationService.setActivityIconRevealAnimation(mCircleIconWrapper, mTitleView);
    }

    /**
     * Starts the transition animation to another Main Activity. The method takes an intent where the final result activity has been set.
     * The method puts extra necessary information on the intent before it is started.
     *
     * @param aIntent Intent containing the destination Activity
     */

    public void transitionToOtherMainActivity(final Intent aIntent) {
        hideErrorView();
        GridLayoutManager manager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        if (manager == null) return;
        final int firstVisibleItemPosition = manager.findFirstVisibleItemPosition();
        final int lastVisibleItemPosition = manager.findLastVisibleItemPosition();

        aIntent.putExtra(
                this.getResources().getString(R.string.decorative_toolbar_position_y),
                mDecorativeToolbar.getTranslationY()
        );

        aIntent.putExtra(
                this.getResources().getString(R.string.main_toolbar_position_y),
                mMainToolbar.getTranslationY()
        );

        Animation.AnimationListener animationListener = new AnimationListenerAdapter() {
            @Override
            public void onAnimationEnd(Animation animation) {
                isTransitioned = true;
                startActivity(aIntent, null);
            }
        };

        AnimationService.startAlphaHideAnimation(mCircleIconWrapper);
        AnimationSet alphaHideAnimation = AnimationService.startAlphaHideAnimation(mTitleView);
        if (mRecyclerView.getAdapter() != null && mRecyclerView.getAdapter().getItemCount() != 0) {
            AnimationService.animateFakeClearing(lastVisibleItemPosition, firstVisibleItemPosition, mRecyclerView, animationListener, mAdapter instanceof StreamsAdapter);
        } else {
            alphaHideAnimation.setAnimationListener(animationListener);
        }
    }

    /**
     * Checks if the user started this activity by pressing the back button on another main activity.
     * If so it runs the show animation for the activity's icon, text and visual elements.
     */
    public void checkIsBackFromMainActivity() {
        if (isTransitioned) {
            GridLayoutManager manager = (GridLayoutManager) mRecyclerView.getLayoutManager();
            if (manager == null) return;
            final int DELAY_BETWEEN = 50;
            int firstVisibleItemPosition = manager.findFirstVisibleItemPosition();
            int lastVisibleItemPosition = manager.findLastVisibleItemPosition();

            int startPositionCol = AnimationService.getColumnPosFromIndex(firstVisibleItemPosition, mRecyclerView);
            int startPositionRow = AnimationService.getRowPosFromIndex(firstVisibleItemPosition, mRecyclerView);

            // Show the Activity Icon and Text
            AnimationService.startAlphaRevealAnimation(mCircleIconWrapper);
            AnimationService.startAlphaRevealAnimation(mTitleView);

            // Fake fill the RecyclerViews with children again
            for (int i = firstVisibleItemPosition; i <= lastVisibleItemPosition; i++) {
                final View mView = mRecyclerView.getChildAt(i - firstVisibleItemPosition);

                int positionColumnDistance = Math.abs(AnimationService.getColumnPosFromIndex(i, mRecyclerView) - startPositionCol);
                int positionRowDistance = Math.abs(AnimationService.getRowPosFromIndex(i, mRecyclerView) - startPositionRow);
                int delay = (positionColumnDistance + positionRowDistance) * DELAY_BETWEEN;

                //int delay = (i - firstVisibleItemPosition) * DELAY_BETWEEN;
                if (mView != null) {
                    AnimationService.startAlphaRevealAnimation(delay, mView, mAdapter instanceof StreamsAdapter);
                }
            }
            isTransitioned = false;
        }
    }

    /**
     * Starts appropriate animations if the activity has been started by another main activity. When the animations end super.onBackPressed() is called.
     * Returns true if that activity has been started through another main activity, else return false;
     */
    public boolean handleBackPressed() {
        if (fromToolbarPosition != -1) {
            Animation.AnimationListener animationListener = new AnimationListenerAdapter() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    try {
                        MainActivity.super.onBackPressed();
                        overridePendingTransition(0, 0);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            };

            // Animate the Activity Icon and text away
            AnimationService.startAlphaHideAnimation(mCircleIconWrapper);
            AnimationSet alphaHideAnimation = AnimationService.startAlphaHideAnimation(mTitleView);

            GridLayoutManager manager = (GridLayoutManager) mRecyclerView.getLayoutManager();
            final int firstVisibleItemPosition = manager.findFirstVisibleItemPosition();
            final int lastVisibleItemPosition = manager.findLastVisibleItemPosition();
            int duration = (int) alphaHideAnimation.getDuration();
            if (mRecyclerView.getAdapter() != null && mRecyclerView.getAdapter().getItemCount() != 0) {
                duration = AnimationService.animateFakeClearing(lastVisibleItemPosition, firstVisibleItemPosition, mRecyclerView, animationListener, mAdapter instanceof StreamsAdapter);
            } else {
                alphaHideAnimation.setAnimationListener(animationListener);
            }
            AnimationService.setActivityToolbarPosition(
                    duration,
                    mMainToolbar,
                    mDecorativeToolbar,
                    this,
                    mDecorativeToolbar.getTranslationY(),
                    fromToolbarPosition,
                    mMainToolbar.getTranslationY(),
                    fromMainToolbarPosition
            );

            return true;
        }
        return false;
    }
}
