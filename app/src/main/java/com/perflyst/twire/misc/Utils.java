package com.perflyst.twire.misc;

import android.app.Activity;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

    public static void useContext(Fragment fragment, UseInterface<Context> useInterface) {
        useNullable(fragment.getContext(), useInterface);
    }

    public static void useActivity(Fragment fragment, UseInterface<Activity> useInterface) {
        useNullable(fragment.getActivity(), useInterface);
    }

    public static <T> void useNullable(@Nullable T value, UseInterface<T> useInterface) {
        if (value != null)
            useInterface.use(value);
    }

    public interface UseInterface<T> {
        void use(@NonNull T value);
    }
}
