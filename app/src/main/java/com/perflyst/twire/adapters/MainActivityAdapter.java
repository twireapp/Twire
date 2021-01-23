package com.perflyst.twire.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;
import com.perflyst.twire.R;
import com.perflyst.twire.misc.PreviewTarget;
import com.perflyst.twire.misc.RoundedTopTransformation;
import com.perflyst.twire.model.MainElement;
import com.perflyst.twire.service.AnimationService;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Sebastian Rask on 03-04-2016.
 */
public abstract class MainActivityAdapter<E extends Comparable<E> & MainElement,
        T extends MainActivityAdapter.ElementsViewHolder> extends RecyclerView.Adapter<T> {
    private final boolean isBelowLollipop;
    private final HashMap<CharSequence, PreviewTarget> mTargets;
    private final AutoSpanRecyclerView mRecyclerView;
    private final int translateLength, cardWidth;
    private final View.OnClickListener mOnClickListener;
    private final View.OnLongClickListener mOnLongClickListener;
    private final Settings mSettings;
    private String LOG_TAG,
            elementStyle;
    private List<E> mElements;
    private boolean sortElements, animateInsert;
    private int mLastPosition, topMargin;
    private Context context;

    public MainActivityAdapter(AutoSpanRecyclerView recyclerView, Context aContext) {
        mElements = new ArrayList<>();
        mTargets = new HashMap<>();
        mRecyclerView = recyclerView;
        context = aContext;
        mSettings = new Settings(aContext);

        elementStyle = initElementStyle();
        mLastPosition = -1;
        topMargin = (int) context.getResources().getDimension(getTopMarginResource());
        cardWidth = calculateCardWidth();
        translateLength = context.getResources().getDisplayMetrics().heightPixels - topMargin;
        isBelowLollipop = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
        sortElements = true;
        animateInsert = true;
        mOnClickListener = this::handleElementOnClick;
        mOnLongClickListener = v -> {
            handleElementOnLongClick(v);
            return true;
        };
    }

    @Override
    public int getItemCount() {
        return mElements.size();
    }

    @Override
    public void onBindViewHolder(@NonNull final T viewHolder, final int position) {
        final E element = mElements.get(position);
        if (element == null) {
            return;
        }
        View viewToInsert = viewHolder.getElementWrapper();
        String previewURL = element.getMediumPreview();

        initElementStyle(viewHolder);
        setViewData(element, viewHolder);
        loadImagePreview(previewURL, element, viewHolder);
        setViewLayoutParams(viewToInsert, position);
        adapterSpecial(viewHolder);
        animateInsert(position, viewToInsert);
    }

    @Override
    @NonNull
    // Is called every time a new viewHolder instance is created.
    // It tells the adapter how we want to the layout of the data for each row should be formatted
    public T onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(getLayoutResource(), viewGroup, false);

        itemView.setOnClickListener(mOnClickListener);
        itemView.setOnLongClickListener(mOnLongClickListener);
        return getElementsViewHolder(itemView);
    }

    /**
     * Way for child adapters to specialize how a view is handled when added to the recyclerview
     *
     * @param viewHolder The viewholder to specialize
     */
    protected void adapterSpecial(T viewHolder) {
    }

    /**
     * Sets the element style of the view.
     *
     * @param viewHolder The viewholder to apply the style to
     */
    private void initElementStyle(T viewHolder) {
        String elementStyle = getElementStyle();
        if (elementStyle.equals(getContext().getString(R.string.card_style_expanded))) {
            setExpandedStyle(viewHolder);
        } else if (elementStyle.equals(getContext().getString(R.string.card_style_normal))) {
            setNormalStyle(viewHolder);
        } else if (elementStyle.equals(getContext().getString(R.string.card_style_minimal))) {
            setCollapsedStyle(viewHolder);
        }
    }

    private void loadImagePreview(String previewURL, E element,
                                  final ElementsViewHolder viewHolder) {
        RequestBuilder<Bitmap> creator = Glide.with(context)
                .asBitmap()
                .load(previewURL)
                // Refresh preview images every 5 minutes
                .signature(new ObjectKey(
                        System.currentTimeMillis() / TimeUnit.MINUTES.toMillis(5)))
                // Image to show while loading, on failure, or if previewURL is null
                .placeholder(
                        ContextCompat.getDrawable(context, element.getPlaceHolder(getContext())))
                // Fade from placeholder image to loaded image over 300ms with cross fade
                .transition(BitmapTransitionOptions.withWrapped(new DrawableCrossFadeFactory
                        .Builder(300).setCrossFadeEnabled(true).build()));
        if (isBelowLollipop) {
            /* On platforms before Lollipop the CardView that holds the preview image does not
             * clip its children that intersect with rounded corners. Round the image corners so
             * the rounded corners still appear. */
            creator = creator.transform(new RoundedTopTransformation(
                    context.getResources().getDimension(getCornerRadiusResource())));
        }
        PreviewTarget mTarget = new PreviewTarget() {
            private boolean loaded = false;

            @Override
            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition transition) {
                if (!loaded) {
                    loaded = true;
                    ImageView previewView = viewHolder.getPreviewView();
                    boolean success = transition != null && transition.transition(bitmap, new BitmapImageViewTarget(previewView));
                    if (!success) previewView.setImageBitmap(bitmap);
                    setPreview(bitmap);
                }
            }

            @Override
            public void onLoadStarted(@Nullable Drawable placeHolderDrawable) {
                viewHolder.getPreviewView().setImageDrawable(placeHolderDrawable);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        };

        creator.into(mTarget);
        mTargets.put(viewHolder.getTargetsKey(), mTarget);
    }

    private void animateInsert(int position, View viewToInsert) {
        if (!animateInsert) {
            return;
        }

        // Animate the cards insert if the card has not already been shown
        int row = (int) Math.ceil((1.0 + position) / mRecyclerView.getSpanCount()) - 1;
        if (!mRecyclerView.hasScrolled() && position > mLastPosition) {
            AnimationService.setAdapterInsertAnimation(viewToInsert, row, translateLength);
            mLastPosition = position;
        }
    }

    /**
     * Adds a list of elements to the adapter and recyclerview
     *
     * @param aElementList The list of elements.
     */
    public void addList(List<E> aElementList) {
        for (E element : aElementList) {
            add(element);
        }
    }

    /**
     * Adds an element to the adapter and recyclerview.
     * The correct position to add the element is found.
     *
     * @param element The element to add
     */
    public void add(E element) {
        if (!mElements.contains(element)) {
            int position = 0;
            if (sortElements) {
                boolean foundPosition = false;
                while (!foundPosition && position < mElements.size()) {
                    E elementToCompare = mElements.get(position);
                    if (element.compareTo(elementToCompare) > 0) {
                        foundPosition = true;
                    } else {
                        position++;
                    }
                }
            } else {
                position = mElements.size();
            }

            mElements.add(position, element);
            notifyItemInserted(position);
        }
    }

    /**
     * Hides every currently shown element in the recyclerview. When the last view has been hidden
     * clearNoAnimation() is called
     *
     * @return The time is takes for the last element to hide
     */
    public int clear() {
        final int ANIMATION_DURATION = 300;
        final int BASE_DELAY = 50;

        int startPosition = mRecyclerView.getManager().findFirstVisibleItemPosition();
        int endPosition = mRecyclerView.getManager().findLastVisibleItemPosition();

        int timeBeforeLastAnimIsDone = ANIMATION_DURATION;
        for (int i = startPosition; i <= endPosition; i++) {
            int delay = (i - startPosition) * BASE_DELAY;
            final int finalI = i;

            final int TRANSLATE_LENGTH = context.getResources().getDisplayMetrics().heightPixels;
            Animation mTranslateAnim = new TranslateAnimation(0, 0, 0, TRANSLATE_LENGTH);
            Animation mAlphaAnim = new AlphaAnimation(1f, 0f);

            final AnimationSet mAnimSet = new AnimationSet(true);
            mAnimSet.addAnimation(mTranslateAnim);
            mAnimSet.addAnimation(mAlphaAnim);
            mAnimSet.setDuration(ANIMATION_DURATION);
            mAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
            mAnimSet.setFillAfter(true);
            mAnimSet.setFillBefore(true);

            new Handler().postDelayed(() -> {
                View v = mRecyclerView.getManager().getChildAt(finalI);
                if (v != null) {
                    v.startAnimation(mAnimSet);
                }
            }, delay);

            if (i == endPosition) {
                timeBeforeLastAnimIsDone = ANIMATION_DURATION + delay;
            }
        }

        new Handler().postDelayed(this::clearNoAnimation, timeBeforeLastAnimIsDone);

        return timeBeforeLastAnimIsDone;
    }

    /**
     * Instantly clears the children of the recyclerview
     */
    public void clearNoAnimation() {
        mLastPosition = -1;
        mElements.clear();
        notifyDataSetChanged();
    }

    /**
     * Returns the style that was used to lay out the elements
     *
     * @return The style title.
     */
    public String getElementStyle() {
        return elementStyle;
    }

    /**
     * Set the style the elements should be laid out.
     *
     * @param elementStyle The style title
     */
    public void setElementStyle(String elementStyle) {
        this.elementStyle = elementStyle;
    }

    /**
     * Initiates the Style title the elements should be laid out as.
     *
     * @return The style title
     */
    public abstract String initElementStyle();

    /**
     * Sets the expanded style to a viewholder
     *
     * @param viewHolder The viewHolder to apply the style to
     */
    protected abstract void setExpandedStyle(T viewHolder);

    /**
     * Sets the normal style to a viewholder
     *
     * @param viewHolder The viewHolder to apply the style to
     */
    protected abstract void setNormalStyle(T viewHolder);

    /**
     * Sets the collapsed style to a viewholder
     *
     * @param viewHolder The viewHolder to apply the style to
     */
    protected abstract void setCollapsedStyle(T viewHolder);

    abstract T getElementsViewHolder(View view);

    /**
     * Called when the user clicks on any element in the recyclerview
     *
     * @param view The clicked view
     */
    abstract void handleElementOnClick(View view);

    /**
     * Called when the user long clicks on any element in the recyclerview
     *
     * @param view The view which long click was invoked on
     */
    protected void handleElementOnLongClick(View view) {
    }

    /**
     * Sets the layout parameters for a view based on its position
     *
     * @param view     The view
     * @param position The position of the view in the recyclerview
     */
    abstract void setViewLayoutParams(View view, int position);

    /**
     * Sets data from an element to a viewholder
     *
     * @param element    the element containing the information
     * @param viewHolder the viewholder to show the information
     */
    abstract void setViewData(E element, T viewHolder);

    /**
     * Returns the layout resource used for the views showing the element information
     *
     * @return the layout resource
     */
    abstract int getLayoutResource();

    /**
     * Returns the dimension resource for the corner radius
     *
     * @return the resource
     */
    abstract int getCornerRadiusResource();

    /**
     * Returns the dimension resource that defines how long the first added element should be from the top.
     *
     * @return the resource
     */
    abstract int getTopMarginResource();

    abstract int calculateCardWidth();

    List<E> getElements() {
        return mElements;
    }

    public void setElements(List<E> mElements) {
        this.mElements = mElements;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public AutoSpanRecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    int getTopMargin() {
        return topMargin;
    }

    public void setTopMargin(int newTopMargin) {
        topMargin = newTopMargin;
    }

    int getCardWidth() {
        return cardWidth;
    }

    public void setSortElements(boolean sortElements) {
        this.sortElements = sortElements;
    }

    public void enableInsertAnimation() {
        animateInsert = true;
    }

    public void disableInsertAnimation() {
        animateInsert = false;
    }

    HashMap<CharSequence, PreviewTarget> getTargets() {
        return mTargets;
    }

    public Settings getSettings() {
        return mSettings;
    }

    protected abstract static class ElementsViewHolder extends RecyclerView.ViewHolder {
        ElementsViewHolder(View v) {
            super(v);
        }

        public abstract ImageView getPreviewView();

        public abstract CharSequence getTargetsKey();

        public abstract View getElementWrapper();
    }
}
