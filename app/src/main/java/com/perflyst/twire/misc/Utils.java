package com.perflyst.twire.misc;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

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
}
