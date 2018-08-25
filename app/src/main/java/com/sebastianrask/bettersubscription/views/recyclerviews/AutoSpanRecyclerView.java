package com.sebastianrask.bettersubscription.views.recyclerviews;

import android.content.Context;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;

import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.service.Settings;
import com.sebastianrask.bettersubscription.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;

/**
 * Created by Sebastian Rask Jepsen on 08-02-2016.
 */
public class AutoSpanRecyclerView extends RecyclerView {
	private GridLayoutManager mManager;
	private int mSpanCount;
	private int mScrollAmount;
	private Settings mSettings;
	private boolean scrolled;
	private String mSizeName;
	private AutoSpanBehaviour mBehaviour;

	public AutoSpanRecyclerView(Context context) {
		super(context);
		init(context, null);
	}

	public AutoSpanRecyclerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public AutoSpanRecyclerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	protected void init(Context context, AttributeSet attrs) {
		mSettings = new Settings(getContext());
		mSpanCount = 1;

		this.addOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
			}

			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				mScrollAmount += dy;
				setScrolled(mScrollAmount != 0);
			}
		});

		mManager = new GridLayoutManager(getContext(), mSpanCount);
		setLayoutManager(mManager);
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		super.onMeasure(widthSpec, heightSpec);
		if (mBehaviour == null) {
			return;
		}

		int mElementWidth = mBehaviour.getElementWidth(getContext());

		if (mElementWidth > 0) {
			mSpanCount = Math.max(1, getMeasuredWidth() / mElementWidth);
            setSpanCount(mSpanCount);
		}
	}

	protected void setSpanCount(int count) {
		int additionSpan = 0;
		if (mBehaviour == null) {
			mManager.setSpanCount(count);
			return;
		}

		mSizeName = mBehaviour.getElementSizeName(mSettings);
		if (mSizeName.equals(getContext().getString(R.string.card_size_normal))) {
			additionSpan = 1;
		} else if (mSizeName.equals(getContext().getString(R.string.card_size_small))) {
			additionSpan = 2;
		}

		if (mSizeName.equals(getContext().getString(R.string.card_size_huge))) {
			mManager.setSpanCount(1);
		} else {
			mManager.setSpanCount(count + additionSpan);
		}
    }

	public boolean hasSizedChanged() {
		if (mBehaviour == null) {
			return false;
		}

		String newSizeName = mBehaviour.getElementSizeName(mSettings);
		if (mSizeName != null && !mSizeName.equals(newSizeName)) {
			mSizeName = newSizeName;
			return true;
		}
		return false;
	}

	public GridLayoutManager getManager() {
		return mManager;
	}

	public boolean hasScrolled() {
		return scrolled;
	}

	public void setScrolled(boolean isScrolled) {
		scrolled = isScrolled;
	}

	protected Settings getSettings() {
		return mSettings;
	}

	public int getSpanCount() {
		return mManager.getSpanCount();
	}

	public void setBehaviour(AutoSpanBehaviour mBehaviour) {
		this.mBehaviour = mBehaviour;
	}

	public int getElementWidth() {
		return mBehaviour.getElementWidth(getContext());
	}
}
