package com.perflyst.twire.misc

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.widget.TextView
import androidx.annotation.FloatRange
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.Locale

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
        return URLEncoder.encode(s, StandardCharsets.UTF_8)
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
}
