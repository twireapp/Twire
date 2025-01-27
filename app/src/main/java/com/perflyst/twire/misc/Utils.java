package com.perflyst.twire.misc;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.widget.TextView;

import androidx.annotation.FloatRange;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

public class Utils {
    public static String getSystemLanguage() {
        return Locale.getDefault().getLanguage();
    }

    public static SpannableStringBuilder appendSpan(SpannableStringBuilder builder, CharSequence charSequence, Object... whats) {
        int preLength = builder.length();
        builder.append(charSequence);

        for (Object what : whats) {
            builder.setSpan(what, preLength, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }

    /**
     * Sets the text of a {@link TextView} to a locale aware number.
     *
     * @param textView The {@link TextView} to set.
     * @param number   The number to set.
     */
    public static void setNumber(TextView textView, long number) {
        textView.setText(NumberFormat.getIntegerInstance().format(number));
    }

    /**
     * Sets the text of a {@link TextView} to a locale aware percent.
     *
     * @param textView The {@link TextView} to set.
     * @param percent  The percent to set.
     */
    public static void setPercent(TextView textView, @FloatRange(from = 0, to = 1) double percent) {
        textView.setText(NumberFormat.getPercentInstance().format(percent));
    }

    public static String safeEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public static URL safeUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
