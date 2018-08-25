package com.sebastianrask.bettersubscription.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.billing.IabBroadcastReceiver;
import com.sebastianrask.bettersubscription.billing.IabHelper;
import com.sebastianrask.bettersubscription.billing.IabResult;
import com.sebastianrask.bettersubscription.billing.Inventory;
import com.sebastianrask.bettersubscription.billing.Purchase;
import com.sebastianrask.bettersubscription.misc.SecretKeys;
import com.sebastianrask.bettersubscription.service.Service;
import com.sebastianrask.bettersubscription.service.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

public class DonationActivity extends ThemeActivity {
	private String LOG_TAG = getClass().getSimpleName();
	private LinearLayout mainContentLayout, donationAmountLayout;
	private FrameLayout fakeToolbar;
	private Toolbar mainToolbar;
	private TextView remindMeLater, currentPromptView, nextPromptView, donationAmountTextView, gotoPlaystoreLink;
	private FloatingActionButton mFab;
	private View donationNavigateLeft, donationNavigateRight;
	private SupportAnimator revealTransition;
	private Settings settings;

	private final int PAYMENT_STEP = 3;
	private final int STEPS = 10;

	private int currentPrompt = 1;
	private int donationStep = 2;
	private int animationDuration = 600;
	private boolean purchaseFinished = false, donationFlowEnded = false, delayPrompt = false;

	public final String[] SKUS = new String[] {
		"donation_steps_one",
		"donation_steps_two",
		"donation_steps_three",
		"donation_steps_four",
		"donation_steps_five",
		"donation_steps_six",
		"donation_steps_seven",
		"donation_steps_eight",
		"donation_steps_nine",
		"donation_steps_ten"
	};

	private HashMap<String, String> skuToPriceMap;

	private static final int RC_REQUEST = 10001; // (arbitrary) request code for the purchase flow
	private IabHelper mBillingHelper;
	private IabBroadcastReceiver mBroadcastReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_donation);

		fakeToolbar = (FrameLayout) findViewById(R.id.additional_toolbar);
		mainContentLayout = (LinearLayout) findViewById(R.id.main_layout);
		donationAmountLayout = (LinearLayout) findViewById(R.id.donation_amount_layout);
		mainToolbar = (Toolbar) findViewById(R.id.main_toolbar);
		mFab = (FloatingActionButton) findViewById(R.id.fab);
		currentPromptView = (TextView) findViewById(R.id.current_prompt);
		nextPromptView = (TextView) findViewById(R.id.next_prompt);
		donationAmountTextView = (TextView) findViewById(R.id.donateAmount);
		gotoPlaystoreLink = (TextView) findViewById(R.id.goto_playstore);
		donationNavigateLeft = findViewById(R.id.navigate_left_layout);
		donationNavigateRight = findViewById(R.id.navigate_right_layout);
		remindMeLater = (TextView) findViewById(R.id.remind_later);

		initOnClickListeners();
		initHiddenElements();
		initToolbarPosition();
		initBillingHelper();
		updateDonationAmountText();

		Window mWindow = getWindow();
		mWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		// Go straight to the payment step if the user started donation flow through settings
		boolean isFlowUserStarted = getIntent().hasExtra(getString(R.string.donation_flow_is_user_started));
		if (isFlowUserStarted) {
			remindMeLater.setVisibility(View.GONE);
			currentPrompt = PAYMENT_STEP;
			currentPromptView.setText(getString(R.string.donation_how_much));
			donationAmountLayout.animate().alpha(1f).setDuration(animationDuration).setInterpolator(new AccelerateDecelerateInterpolator()).start();
		}

		settings = new Settings(getBaseContext());

		mainContentLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
				v.removeOnLayoutChangeListener(this);
				playInitAnimation();
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		overridePendingTransition(0, 0);
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		if (!donationFlowEnded) {
			onRemindMeLaterClicked(null);
		} else {
			super.onBackPressed();
		}
	}

	public void onRemindMeLaterClicked(View v) {
		trackEvent(R.string.category_click, R.string.action_not_now);

		// Remind again after 1 hour
		settings.setUsageTimeBeforeDonation(settings.getUsageTimeInApp() + (1000 * 60 * 60));

		if (revealTransition != null) {
			quitWithAnimation();
		} else {
			super.onBackPressed();
		}
	}

	private void handleFabClick(View v) {
		boolean showNextView = true,
				isQuitting = checkQuitting();

		TextView viewToHide = currentPromptView,
				 viewToShow = nextPromptView;

		if (currentPrompt % 2 == 0) {
			viewToHide = nextPromptView;
			viewToShow = currentPromptView;
		}
		currentPrompt++; // Do not move this line lower

		if (currentPrompt == PAYMENT_STEP) {
			settings.setDonationPromptShown(true);
			donationAmountLayout.animate().alpha(1f).setDuration(animationDuration).setInterpolator(new AccelerateDecelerateInterpolator()).start();
		} else if (currentPrompt > PAYMENT_STEP) {
			donationAmountLayout.animate().alpha(0f).setDuration(animationDuration).setInterpolator(new AccelerateDecelerateInterpolator()).start();
			if (donationStep == 0) {
				if (!isQuitting) {
					showPlaystoreLink();
				}
				donationFlowEnded = true;
			} else {
				showNextView = purchaseFinished;
				if (!purchaseFinished) {
					showDonationPrompt();
				} else {
					donationFlowEnded = true;
				}
			}
		}

		if (showNextView && !isQuitting) {
			hidePromptText(viewToHide);
			showPromptText(viewToShow);
		}

		delayNextClick();
	}

	private boolean checkQuitting() {
		if (donationFlowEnded) {
			if (revealTransition != null) {
				gotoPlaystoreLink.animate()
						.translationX(-gotoPlaystoreLink.getWidth())
						.setStartDelay(0)
						.setDuration(400)
						.start();

				quitWithAnimation();

			} else {
				DonationActivity.super.onBackPressed();
			}
			return true;
		}
		return false;
	}

	private void quitWithAnimation() {
		SupportAnimator animator = revealTransition.reverse();
		if (animator == null) {
			super.onBackPressed();
			return;
		}
		animator.setDuration(1000);
		animator.addListener(new SupportAnimator.AnimatorListener() {
			@Override
			public void onAnimationStart() {}

			@Override
			public void onAnimationEnd() {
				mainContentLayout.setVisibility(View.INVISIBLE);
				finish();
			}

			@Override
			public void onAnimationCancel() {}

			@Override
			public void onAnimationRepeat() {}
		});
		animator.start();
	}

	private void hidePromptText(TextView v) {
		Interpolator interpolator = new AccelerateDecelerateInterpolator();
		v.animate().alpha(0f).translationY(v.getHeight() * -1).setDuration(animationDuration).setInterpolator(interpolator).start();
	}

	private void showPromptText(TextView v) {
		if (currentPrompt == 2) {
			v.setText(getString(R.string.donation_prompt_three));
		} else if (currentPrompt == PAYMENT_STEP) {
			v.setText(getString(R.string.donation_prompt_four));
		} else if (currentPrompt > PAYMENT_STEP) {
			if (donationStep == 0) {
				v.setText(getString(R.string.donation_prompt_nothing));
			} else {
				v.setText(getString(R.string.donation_prompt_donated));
			}
		}


		Interpolator interpolator = new AccelerateDecelerateInterpolator();
		v.setTranslationY((v.getHeight()));
		v.animate().alpha(1f).translationY(0).setDuration(animationDuration).setInterpolator(interpolator).start();
	}

	private void showDonationPrompt() {
		try {
			purchaseDonation(getSKUFromDonationStep(donationStep), this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showPlaystoreLink() {
		View shownView = currentPromptView;
		if (nextPromptView.getAlpha() == 1f) {
			shownView = nextPromptView;
		}

		gotoPlaystoreLink.setY(shownView.getY() + shownView.getHeight() + gotoPlaystoreLink.getHeight());
		gotoPlaystoreLink.setTranslationX(gotoPlaystoreLink.getWidth() * -1);
		gotoPlaystoreLink.animate().alpha(1f).translationX(0).setStartDelay(animationDuration).setDuration(animationDuration).setInterpolator(new OvershootInterpolator()).start();
	}

	private void updateDonationAmountText() {
		String price = getString(R.string.donation_amount, donationStep);
		if (donationStep == 0) {
			price = getString(R.string.donation_step_zero);
		} else if (skuToPriceMap != null && skuToPriceMap.containsKey(getSKUFromDonationStep(donationStep))) {
			price = skuToPriceMap.get(getSKUFromDonationStep(donationStep));
		}

		donationAmountTextView.setText(price);
	}

	private String getSKUFromDonationStep(int donationStep) {
		return SKUS[donationStep - 1];
	}

	private void delayNextClick() {
		mFab.setClickable(false);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				mFab.setClickable(true);
			}
		}, animationDuration);
	}

	private void initOnClickListeners() {
		mFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				handleFabClick(v);
			}
		});
		donationNavigateLeft.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				donationStep--;
				// Cannot go lower than 0
				if (donationStep < 0) {
					donationStep = 0;
				}
				updateDonationAmountText();
			}
		});

		donationNavigateRight.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				donationStep++;
				if (donationStep > STEPS) {
					donationStep = STEPS;
				}
				updateDonationAmountText();
			}
		});

		gotoPlaystoreLink.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(Service.getPlayStoreIntent());
				trackEvent(R.string.category_click, R.string.action_playstore);
			}
		});
	}

	private void initHiddenElements() {
		nextPromptView.setAlpha(0f);
		donationAmountLayout.setAlpha(0f);
		gotoPlaystoreLink.setAlpha(0f);
	}

	private void initToolbarPosition() {
		float mainToolbarPositionY = getIntent().getFloatExtra(getString(R.string.main_toolbar_position_y), -1);
		float toolbarPositionY = getIntent().getFloatExtra(getString(R.string.decorative_toolbar_position_y), -1);

		if (mainToolbarPositionY != -1) {
			mainToolbar.setTranslationY(mainToolbarPositionY);
			fakeToolbar.setTranslationY(toolbarPositionY);
		} else {
			mainToolbar.setVisibility(View.GONE);
			fakeToolbar.setVisibility(View.GONE);
		}

	}

	private void playInitAnimation() {
		DisplayMetrics dm = getResources().getDisplayMetrics();
		// Get the center for the FAB
		int cx = dm.widthPixels / 2;
		int cy = 0;

		float finalRadius = dm.heightPixels;

		revealTransition = ViewAnimationUtils.createCircularReveal(mainContentLayout, cx, cy, 0, finalRadius);
		revealTransition.setInterpolator(new AccelerateDecelerateInterpolator());
		revealTransition.setDuration(800);
		revealTransition.addListener(new SupportAnimator.AnimatorListener() {
			@Override
			public void onAnimationStart() {
				mainContentLayout.setVisibility(View.VISIBLE);
			}
			public void onAnimationEnd() {}
			public void onAnimationCancel() {}
			public void onAnimationRepeat() {}
		});
		revealTransition.start();
	}

	public void purchaseFlowFinished(boolean isSuccess) {
		if (isSuccess) {
			purchaseFinished = true;
		} else {
			// Reset flow
			try {
				currentPromptView.setAlpha(0f);
				nextPromptView.setAlpha(0f);

				donationStep = 1;
				updateDonationAmountText();

				mBillingHelper.dispose();
				initBillingHelper();
			} catch (IabHelper.IabAsyncInProgressException e) {
				e.printStackTrace();
			}
			currentPrompt = 2;
		}

		mFab.performClick();
	}

	public void purchaseDonation(String SKU, Activity activity) throws IabHelper.IabAsyncInProgressException {
		mBillingHelper.launchPurchaseFlow(activity, SKU, RC_REQUEST,
				new IabHelper.OnIabPurchaseFinishedListener() {
					@Override
					public void onIabPurchaseFinished(IabResult result, Purchase info) {
						Log.d(LOG_TAG, "Finished");
						purchaseFlowFinished(result.isSuccess());

						String purchaseToken = "inapp:"+getPackageName()+":android.test.purchased";
						//mBillingHelper.consumeToken(purchaseToken, DonationActivity.this);

						if (result.isSuccess()) {
							try {
								mBillingHelper.consumeAsync(info, new IabHelper.OnConsumeFinishedListener() {
									@Override
									public void onConsumeFinished(Purchase purchase, IabResult result) {

									}
								});
							} catch (IabHelper.IabAsyncInProgressException e) {
								e.printStackTrace();
							}
						}
					}
				}, "");
	}

	private void initBillingHelper() {
		Log.d(LOG_TAG, "Creating IAB helper.");
		mBillingHelper = new IabHelper(this, SecretKeys.IN_APP_PURCHASE_BASE64_KEY);

		// enable debug logging (for a production application, you should set this to false).
		mBillingHelper.enableDebugLogging(true);


		Log.d(LOG_TAG, "Starting setup.");
		mBillingHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				Log.d(LOG_TAG, "Setup finished.");

				if (!result.isSuccess()) {
					// Oh noes, there was a problem.
					Log.e(LOG_TAG, "Problem setting up in-app billing: " + result);
					return;
				}

				// Have we been disposed of in the meantime? If so, quit.
				if (mBillingHelper == null) return;

				// Important: Dynamically register for broadcast messages about updated purchases.
				// We register the receiver here instead of as a <receiver> in the Manifest
				// because we always call getPurchases() at startup, so therefore we can ignore
				// any broadcasts sent while the app isn't running.
				// Note: registering this listener in an Activity is a bad idea, but is done here
				// because this is a SAMPLE. Regardless, the receiver must be registered after
				// IabHelper is setup, but before first call to getPurchases().
				mBroadcastReceiver = new IabBroadcastReceiver(new IabBroadcastReceiver.IabBroadcastListener() {
					@Override
					public void receivedBroadcast() {
						// Received a broadcast notification that the inventory of items has changed
						Log.d(LOG_TAG, "Received broadcast notification. Querying inventory.");
						try {
							mBillingHelper.queryInventoryAsync(mGotInventoryListener);
						} catch (IabHelper.IabAsyncInProgressException e) {
							Log.e(LOG_TAG, "Error querying inventory. Another async operation in progress.");
						}
					}
				});
				IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
				registerReceiver(mBroadcastReceiver, broadcastFilter);

				// IAB is fully set up. Now, let's get an inventory of stuff we own.
				Log.d(LOG_TAG, "Setup successful. Querying inventory.");
				try {
					mBillingHelper.queryInventoryAsync(mGotInventoryListener);
				} catch (IabHelper.IabAsyncInProgressException e) {
					Log.e(LOG_TAG, "Error querying inventory. Another async operation in progress.");
				}

				try {
					Bundle bundle = mBillingHelper.getDonationDetails(
							new ArrayList<>(Arrays.asList(SKUS))
					);

					initPrices(bundle);
				} catch (RemoteException | JSONException e) {
					Log.e(LOG_TAG, "FAILED TO GET DONATION DETAILS");
					e.printStackTrace();
				}
			}
		});
	}

	private void initPrices(Bundle skuDetails) throws JSONException {
		int response = skuDetails.getInt(IabHelper.RESPONSE_CODE);
		Log.d("HELPERDETAILS", "REPONSE CODE: " + response);
		if (response == 0) {
			ArrayList<String> responseList = skuDetails.getStringArrayList(IabHelper.RESPONSE_GET_SKU_DETAILS_LIST);

			if (responseList != null) {
				skuToPriceMap = new HashMap<>();
				for (String thisResponse : responseList) {
					JSONObject object = new JSONObject(thisResponse);
					String sku = object.getString("productId");
					String price = object.getString("price");

					skuToPriceMap.put(sku, price);
				}
				updateDonationAmountText();
			}
		}
	}

	// Listener that's called when we finish querying the items and subscriptions we own
	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			// Have we been disposed of in the meantime? If so, quit.
			if (mBillingHelper == null) return;

			// Is it a failure?
			if (result.isFailure()) {
				Log.e(LOG_TAG,"Failed to query inventory: " + result);
				return;
			}

			Log.d(LOG_TAG, "Query inventory was successful.");
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();

		try {
			if (mBroadcastReceiver != null) {
				unregisterReceiver(mBroadcastReceiver);
			}

			Log.d(LOG_TAG, "Destroying helper.");
			if (mBillingHelper != null) {
				mBillingHelper.disposeWhenFinished();
				mBillingHelper = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(LOG_TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

		// Pass on the activity result to the helper for handling
		if (!mBillingHelper.handleActivityResult(requestCode, resultCode, data)) {
			super.onActivityResult(requestCode, resultCode, data);
			purchaseFlowFinished(resultCode == 0);
		}
	}
}
