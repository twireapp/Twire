package com.sebastianrask.bettersubscription.misc;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import androidx.annotation.DrawableRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.activities.DonationActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

/**
 * Created by Sebastian Rask on 02-03-2017.
 */

public class UpdateDialogHandler extends RecyclerView.Adapter<UpdateDialogHandler.FeatureViewHolder> {
	private List<String> newFeatures;
	private ViewGroup mParent;
	private LayoutInflater mInflater;
	private UpdateDialogHandler.Delegate mDelegate;
	private View mMainContainer, mDonationContainer, mFadeView, mTitleContainer, mActionContainer, mSuperContainer;

	public UpdateDialogHandler(ViewGroup mParent, LayoutInflater inflater) {
		this.mParent = mParent;
		this.mInflater = inflater;

		this.newFeatures = new ArrayList<>();
	}

	public void registerDelegate(UpdateDialogHandler.Delegate delegate) {
		mDelegate = delegate;
	}

	public void show() {
		View dialogContainer = mInflater.inflate(R.layout.dialog_new_update, mParent);
		mMainContainer = dialogContainer.findViewById(R.id.main_container);
		mDonationContainer = dialogContainer.findViewById(R.id.donation_container);
		mFadeView = dialogContainer.findViewById(R.id.faded_background);
		mTitleContainer = dialogContainer.findViewById(R.id.title_container);
		mActionContainer = dialogContainer.findViewById(R.id.action_container);
		mSuperContainer = dialogContainer.findViewById(R.id.super_container);

		setupRecyclerView((RecyclerView) dialogContainer.findViewById(R.id.whats_new_recyclerview));
		setRandomEmote((ImageView) dialogContainer.findViewById(R.id.img_emote));

		dialogContainer.findViewById(R.id.negative_action).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mDelegate != null) {
					mDelegate.onClickShare();
				}
				dismiss();
				share();
			}
		});

		dialogContainer.findViewById(R.id.positive_action).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mDelegate != null) {
					mDelegate.onClickGotIt();
				}
				dismiss();
			}
		});

		dialogContainer.findViewById(R.id.donation_view).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent intent = new Intent(mParent.getContext(), DonationActivity.class);
				intent.putExtra(mParent.getContext().getString(R.string.donation_flow_is_user_started), true);
				mParent.getContext().startActivity(intent);

				mSuperContainer.setVisibility(View.GONE);
			}
		});

		mFadeView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
	}

	public void dismiss() {
		if (mMainContainer == null || mDonationContainer == null || mTitleContainer == null || mActionContainer == null || mFadeView == null) {
			mSuperContainer.setVisibility(View.GONE);
			return;
		}

		final int TOP_BOT_CONTAINER_ANIMATION_DURATION = 240;
		mTitleContainer.animate().translationY(mTitleContainer.getHeight()).alpha(0f).setDuration(TOP_BOT_CONTAINER_ANIMATION_DURATION).start();
		mActionContainer.animate().translationY(-mActionContainer.getHeight()).alpha(0f).setDuration(TOP_BOT_CONTAINER_ANIMATION_DURATION).start();

		circularDismissView(mMainContainer);
		circularDismissView(mDonationContainer);
		mFadeView.animate().alpha(0f).setDuration(300).start();
	}

	private void share() {
		Context mContext = mParent.getContext();
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, mContext.getString(R.string.update_share_text));
		sendIntent.setType("text/plain");

		try {
			mContext.startActivity(sendIntent);
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void circularDismissView(final View view) {
		final int DURATION = 600;

		// Get the center for the FAB
		int cx = (int) view.getX() + view.getMeasuredHeight() / 2;
		int cy = (int) view.getY() + view.getMeasuredWidth() / 2;

		// get the final radius for the clipping circle
		int dx = Math.max(cx, view.getWidth() - cx);
		int dy = Math.max(cy, view.getHeight() - cy);
		float finalRadius = (float) Math.hypot(dx, dy);

		final SupportAnimator dismissAnimation = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius).reverse();
		dismissAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
		dismissAnimation.setDuration(DURATION);
		dismissAnimation.start();
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				mSuperContainer.setVisibility(View.GONE);
			}
		}, DURATION);
	}

	private void setupRecyclerView(RecyclerView recyclerView) {
		recyclerView.setAdapter(this);
		recyclerView.setLayoutManager(new LinearLayoutManager(mParent.getContext(), LinearLayoutManager.VERTICAL, false));

		addFeature("Chat @ Mention");
		addFeature("See VOD progress directly from feed");
		addFeature("Additional Profile information");
		addFeature("Check Profile without exiting stream");
		addFeature("Added Viewer count in 'My Games'");
		addFeature("Optimized Emote size");

		this.notifyDataSetChanged();
	}

	private void setRandomEmote(ImageView mImageView) {
		int[] emoteIds = new int[] {
				R.drawable.emote_feelsamazingman, R.drawable.emote_feelsgoodman, R.drawable.emote_yohiho,
				R.drawable.emote_datcheffy, R.drawable.emote_coolcat, R.drawable.emote_helix, R.drawable.emote_goldenkappa,
				R.drawable.emote_seemsgood
		};

		@DrawableRes
		int emoteId = emoteIds[new Random().nextInt(emoteIds.length)];
		mImageView.setImageResource(emoteId);
	}

	private void addFeature(String feature) {
		newFeatures.add(feature);
	}

	@Override
	public UpdateDialogHandler.FeatureViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View itemView = LayoutInflater
				.from(parent.getContext())
				.inflate(R.layout.cell_new_feature, parent, false);

		return new UpdateDialogHandler.FeatureViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(UpdateDialogHandler.FeatureViewHolder holder, int position) {
		String feature = newFeatures.get(position);
		holder.mFeatureText.setText(feature);
	}

	@Override
	public int getItemCount() {
		return newFeatures.size();
	}

	protected class FeatureViewHolder extends RecyclerView.ViewHolder {
		protected TextView mFeatureText;

		FeatureViewHolder(View itemView) {
			super(itemView);
			mFeatureText = (TextView) itemView.findViewById(R.id.txt_feature);
		}
	}

	public interface Delegate {
		void onClickShare();
		void onClickGotIt();
		void onClickDonate();
	}
}
