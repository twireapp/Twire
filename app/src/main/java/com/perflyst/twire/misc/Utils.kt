package com.perflyst.twire.misc

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.activity.addCallback
import androidx.annotation.FloatRange
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import com.perflyst.twire.R
import com.perflyst.twire.activities.StartUpActivity
import com.perflyst.twire.utils.RoundTransition
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.Locale
import java.util.Stack

object Utils {
    val systemLanguage: String
        get() = Locale.getDefault().language

    fun appendSpan(
        builder: SpannableStringBuilder,
        charSequence: CharSequence?,
        vararg whats: Any?
    ): SpannableStringBuilder {
        val preLength = builder.length
        builder.append(charSequence)

        for (what in whats) {
            builder.setSpan(what, preLength, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return builder
    }

    /**
     * Sets the text of a [TextView] to a locale aware number.
     *
     * @param textView The [TextView] to set.
     * @param number   The number to set.
     */
    fun setNumber(textView: TextView, number: Long) {
        textView.text = NumberFormat.getIntegerInstance().format(number)
    }

    /**
     * Sets the text of a [TextView] to a locale aware percent.
     *
     * @param textView The [TextView] to set.
     * @param percent  The percent to set.
     */
    fun setPercent(textView: TextView, @FloatRange(from = 0.0, to = 1.0) percent: Double) {
        textView.text = NumberFormat.getPercentInstance().format(percent)
    }

    @JvmStatic
    fun safeEncode(s: String?): String? {
        return try {
            URLEncoder.encode(s, StandardCharsets.UTF_8.toString())
        } catch (_: UnsupportedEncodingException) {
            s
        }
    }

    fun safeUrl(url: String?): URL? {
        return try {
            URL(url)
        } catch (_: MalformedURLException) {
            null
        }
    }

    fun getPreviewUrl(url: String?, width: String, height: String): String? {
        if (url == null) return null
        return url.replace("%?\\{width\\}".toRegex(), width)
            .replace("%?\\{height\\}".toRegex(), height)
    }

    fun getPreviewUrl(url: String?): String? {
        return getPreviewUrl(url, "320", "180")
    }

    fun getOnlineSince(startedAt: Long): String {
        return DateUtils.formatElapsedTime((System.currentTimeMillis() - startedAt) / 1000)
    }
}

private val FragmentActivity.singleStack: Stack<Pair<Class<out Fragment>, Bundle?>>
    get() = (this as? StartUpActivity)?.singleStack
        ?: throw IllegalStateException("single=true navigation requires a StartUpActivity host")

fun Fragment.navigate(
    fragment: Class<out Fragment>,
    args: Bundle? = null,
    enterAnim: Transition? = null,
    exitAnim: Transition? = null,
    backStack: Boolean = true,
    single: Boolean = false,
    sharedElement: View? = null,
    sharedName: String? = null
) {
    requireActivity().navigate(
        fragment,
        args,
        enterAnim,
        exitAnim,
        backStack,
        single,
        sharedElement,
        sharedName
    )
}

fun FragmentActivity.navigate(
    fragment: Class<out Fragment>,
    args: Bundle? = null,
    enterAnim: Transition? = null,
    exitAnim: Transition? = null,
    backStack: Boolean = true,
    single: Boolean = false,
    sharedElement: View? = null,
    sharedName: String? = null
) {
    supportFragmentManager.commit {
        setReorderingAllowed(true)
        if (sharedElement != null) {
            addSharedElement(sharedElement, sharedName ?: sharedElement.transitionName)
        }

        if (exitAnim != null) {
            supportFragmentManager.findFragmentById(R.id.startup_activity)?.exitTransition =
                exitAnim
        }

        val fragmentInstance = fragment.getDeclaredConstructor().newInstance().apply {
            arguments = args
            enterTransition = enterAnim
            returnTransition = exitAnim
            sharedElementEnterTransition = (TransitionInflater.from(baseContext)
                .inflateTransition(R.transition.change_image_transform) as androidx.transition.TransitionSet)
                .addTransition(
                    RoundTransition()
                )
        }

        replace(R.id.startup_activity, fragmentInstance)
        if (single) {
            if (!backStack) singleStack.clear()
            singleStack.removeIf { it.first == fragment }
            singleStack.push(Pair(fragment, args))
        } else if (backStack) {
            addToBackStack(null)
        } else {
            supportFragmentManager.popBackStackImmediate(
                null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
    }
}

fun Fragment.setupToolbar(
    toolbar: Toolbar,
    title: Int? = null
) {
    val activity = requireActivity() as AppCompatActivity
    activity.setSupportActionBar(toolbar)
    activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    activity.supportActionBar?.title = title?.let { getString(it) }
    setHasOptionsMenu(true)
}

fun Fragment.addBackPressed(callback: () -> Boolean) {
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
        if (!callback()) {
            isEnabled = false
            requireActivity().onBackPressedDispatcher.onBackPressed()
            isEnabled = true
        }
    }
}

fun Fragment.popBackStack() {
    requireActivity().supportFragmentManager.popBackStack()
}
