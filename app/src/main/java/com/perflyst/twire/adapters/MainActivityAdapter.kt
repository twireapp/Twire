package com.perflyst.twire.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import com.perflyst.twire.R
import com.perflyst.twire.adapters.MainActivityAdapter.ElementsViewHolder
import com.perflyst.twire.misc.PreviewTarget
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.service.AnimationService
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/**
 * Created by Sebastian Rask on 03-04-2016.
 */
abstract class MainActivityAdapter<E, T : ElementsViewHolder>(
    val recyclerView: AutoSpanRecyclerView,
    var context: Context
) : RecyclerView.Adapter<T?>() {
    val targets: HashMap<CharSequence, PreviewTarget> = HashMap()
    private val translateLength: Int
    val cardWidth: Int
    private val mOnClickListener: View.OnClickListener
    private val mOnLongClickListener: OnLongClickListener
    /**
     * Returns the style that was used to lay out the elements
     *
     * @return The style title.
     */
    /**
     * Set the style the elements should be laid out.
     *
     * @param elementStyle The style title
     */
    var elementStyle: String
    var elements: MutableList<E> = ArrayList()
    private var sortElements: Boolean
    private var animateInsert: Boolean
    private var mLastPosition: Int
    var topMarginFirst: Int

    init {

        elementStyle = initElementStyle()
        mLastPosition = -1
        topMarginFirst = context.resources.getDimension(this.topMarginResource).toInt()
        cardWidth = calculateCardWidth()
        translateLength = context.resources.displayMetrics.heightPixels - topMarginFirst
        sortElements = true
        animateInsert = true
        mOnClickListener = View.OnClickListener { view: View -> this.handleElementOnClick(view) }
        mOnLongClickListener = OnLongClickListener { v: View ->
            handleElementOnLongClick(v)
            true
        }
    }

    override fun getItemCount(): Int {
        return elements.size
    }

    override fun onBindViewHolder(viewHolder: T, position: Int) {
        val element = elements[position]
        if (element == null) {
            return
        }
        val viewToInsert = viewHolder.elementWrapper
        val previewURL = getPreviewUrl(element)

        initElementStyle(viewHolder)
        setViewData(element, viewHolder)
        loadImagePreview(previewURL, element, viewHolder)
        setViewLayoutParams(viewToInsert, position)
        adapterSpecial(viewHolder)
        animateInsert(position, viewToInsert)
    }

    // Is called every time a new viewHolder instance is created.
    // It tells the adapter how we want to the layout of the data for each row should be formatted
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): T {
        val itemView: View = LayoutInflater.from(viewGroup.context)
            .inflate(this.layoutResource, viewGroup, false)

        itemView.setOnClickListener(mOnClickListener)
        itemView.setOnLongClickListener(mOnLongClickListener)
        return getElementsViewHolder(itemView)
    }

    /**
     * Way for child adapters to specialize how a view is handled when added to the recyclerview
     *
     * @param viewHolder The viewholder to specialize
     */
    protected open fun adapterSpecial(viewHolder: T) {
    }

    /**
     * Sets the element style of the view.
     *
     * @param viewHolder The viewholder to apply the style to
     */
    private fun initElementStyle(viewHolder: T) {
        val elementStyle = this.elementStyle
        when (elementStyle) {
            this.context.getString(R.string.card_style_expanded) -> {
                setExpandedStyle(viewHolder)
            }

            this.context.getString(R.string.card_style_normal) -> {
                setNormalStyle(viewHolder)
            }

            this.context.getString(R.string.card_style_minimal) -> {
                setCollapsedStyle(viewHolder)
            }
        }
    }

    private fun loadImagePreview(
        previewURL: String?, element: E,
        viewHolder: ElementsViewHolder
    ) {
        val creator = Glide.with(context)
            .asBitmap()
            .load(previewURL) // Refresh preview images every 5 minutes
            .signature(
                ObjectKey(
                    System.currentTimeMillis() / TimeUnit.MINUTES.toMillis(5)
                )
            ) // Image to show while loading, on failure, or if previewURL is null
            .placeholder(
                AppCompatResources.getDrawable(
                    context, getPlaceHolder(element, this.context)
                )
            ) // Fade from placeholder image to loaded image over 300ms with cross fade
            .transition(
                BitmapTransitionOptions.withWrapped(
                    DrawableCrossFadeFactory.Builder(300).setCrossFadeEnabled(true).build()
                )
            )
        val mTarget: PreviewTarget = object : PreviewTarget() {
            private var loaded = false

            override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap?>?) {
                if (!loaded) {
                    loaded = true
                    val previewView = viewHolder.previewView
                    val success = transition != null && transition.transition(
                        bitmap,
                        BitmapImageViewTarget(previewView)
                    )
                    if (!success) previewView.setImageBitmap(bitmap)
                    preview = bitmap
                }
            }

            override fun onLoadStarted(placeHolderDrawable: Drawable?) {
                viewHolder.previewView.setImageDrawable(placeHolderDrawable)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                refreshPreview(
                    element,
                    context
                ) { loadImagePreview(getPreviewUrl(element), element, viewHolder) }
            }
        }

        creator.into<PreviewTarget?>(mTarget)
        targets.put(viewHolder.targetsKey, mTarget)
    }

    private fun animateInsert(position: Int, viewToInsert: View) {
        if (!animateInsert) {
            return
        }

        // Animate the cards insert if the card has not already been shown
        val row = ceil((1.0 + position) / recyclerView.spanCount).toInt() - 1
        if (!recyclerView.hasScrolled() && position > mLastPosition) {
            AnimationService.setAdapterInsertAnimation(viewToInsert, row, translateLength)
            mLastPosition = position
        }
    }

    /**
     * Adds a list of elements to the adapter and recyclerview
     *
     * @param aElementList The list of elements.
     */
    fun addList(aElementList: MutableList<E>) {
        for (element in aElementList) {
            add(element)
        }
    }

    /**
     * Adds an element to the adapter and recyclerview.
     * The correct position to add the element is found.
     *
     * @param element The element to add
     */
    fun add(element: E) {
        if (!elements.contains(element)) {
            var position = 0
            if (sortElements) {
                var foundPosition = false
                while (!foundPosition && position < elements.size) {
                    val elementToCompare = elements[position]!!
                    if (compareTo(element, elementToCompare) > 0) {
                        foundPosition = true
                    } else {
                        position++
                    }
                }
            } else {
                position = elements.size
            }

            elements.add(position, element)
            notifyItemInserted(position)
        }
    }

    /**
     * Hides every currently shown element in the recyclerview. When the last view has been hidden
     * clearNoAnimation() is called
     *
     * @return The time is takes for the last element to hide
     */
    fun clear(): Int {
        val animationDuration = 300
        val baseDelay = 50

        val startPosition = recyclerView.manager.findFirstVisibleItemPosition()
        val endPosition = recyclerView.manager.findLastVisibleItemPosition()

        var timeBeforeLastAnimIsDone = animationDuration
        for (i in startPosition..endPosition) {
            val delay = (i - startPosition) * baseDelay
            val finalI = i

            val translateLength = context.resources.displayMetrics.heightPixels
            val mTranslateAnim: Animation =
                TranslateAnimation(0f, 0f, 0f, translateLength.toFloat())
            val mAlphaAnim: Animation = AlphaAnimation(1f, 0f)

            val mAnimSet = AnimationSet(true)
            mAnimSet.addAnimation(mTranslateAnim)
            mAnimSet.addAnimation(mAlphaAnim)
            mAnimSet.setDuration(animationDuration.toLong())
            mAnimSet.interpolator = AccelerateDecelerateInterpolator()
            mAnimSet.setFillAfter(true)
            mAnimSet.setFillBefore(true)

            Handler().postDelayed({
                val v = recyclerView.manager.getChildAt(finalI)
                v?.startAnimation(mAnimSet)
            }, delay.toLong())

            if (i == endPosition) {
                timeBeforeLastAnimIsDone = animationDuration + delay
            }
        }

        Handler().postDelayed(
            { this.clearNoAnimation() },
            timeBeforeLastAnimIsDone.toLong()
        )

        return timeBeforeLastAnimIsDone
    }

    /**
     * Instantly clears the children of the recyclerview
     */
    fun clearNoAnimation() {
        mLastPosition = -1
        elements.clear()
        notifyDataSetChanged()
    }

    /**
     * Initiates the Style title the elements should be laid out as.
     *
     * @return The style title
     */
    abstract fun initElementStyle(): String

    /**
     * Sets the expanded style to a viewholder
     *
     * @param viewHolder The viewHolder to apply the style to
     */
    protected abstract fun setExpandedStyle(viewHolder: T)

    /**
     * Sets the normal style to a viewholder
     *
     * @param viewHolder The viewHolder to apply the style to
     */
    protected abstract fun setNormalStyle(viewHolder: T)

    /**
     * Sets the collapsed style to a viewholder
     *
     * @param viewHolder The viewHolder to apply the style to
     */
    protected abstract fun setCollapsedStyle(viewHolder: T)

    abstract fun getElementsViewHolder(view: View): T

    /**
     * Called when the user clicks on any element in the recyclerview
     *
     * @param view The clicked view
     */
    abstract fun handleElementOnClick(view: View)

    /**
     * Called when the user long clicks on any element in the recyclerview
     *
     * @param view The view which long click was invoked on
     */
    protected open fun handleElementOnLongClick(view: View) {
    }

    /**
     * Sets the layout parameters for a view based on its position
     *
     * @param view     The view
     * @param position The position of the view in the recyclerview
     */
    abstract fun setViewLayoutParams(view: View, position: Int)

    /**
     * Sets data from an element to a viewholder
     *
     * @param element    the element containing the information
     * @param viewHolder the viewholder to show the information
     */
    abstract fun setViewData(element: E, viewHolder: T)

    /**
     * Returns the layout resource used for the views showing the element information
     *
     * @return the layout resource
     */
    abstract val layoutResource: Int

    /**
     * Returns the dimension resource for the corner radius
     *
     * @return the resource
     */
    abstract val cornerRadiusResource: Int

    /**
     * Returns the dimension resource that defines how long the first added element should be from the top.
     *
     * @return the resource
     */
    abstract val topMarginResource: Int

    abstract fun calculateCardWidth(): Int

    // MainElement implementation
    abstract fun compareTo(element: E, other: E): Int

    fun getPreviewUrl(element: E): String? {
        return Utils.getPreviewUrl(
            getPreviewTemplate(element),
            this.width,
            this.height
        )
    }

    abstract fun getPreviewTemplate(element: E): String?

    open val width: String
        get() = "320"

    open val height: String
        get() = "180"

    @DrawableRes
    abstract fun getPlaceHolder(element: E, context: Context?): Int

    open fun refreshPreview(element: E, context: Context?, callback: Runnable) {
    }

    fun setSortElements(sortElements: Boolean) {
        this.sortElements = sortElements
    }

    fun enableInsertAnimation() {
        animateInsert = true
    }

    fun disableInsertAnimation() {
        animateInsert = false
    }

    abstract class ElementsViewHolder internal constructor(v: View) : RecyclerView.ViewHolder(v) {
        abstract val previewView: ImageView

        abstract val targetsKey: CharSequence

        abstract val elementWrapper: View
    }
}
