package com.perflyst.twire.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.perflyst.twire.BuildConfig;
import com.perflyst.twire.R;
import com.perflyst.twire.misc.DrawableBulletSpan;
import com.perflyst.twire.misc.Utils;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.Settings;
import com.rey.material.widget.Button;

import java.util.HashMap;
import java.util.Map;

public class ChangelogDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        assert activity != null;

        SpannableStringBuilder builder = new SpannableStringBuilder();
        String[] lines = activity.getResources().getString(R.string.changelog_lines).split("\n");
        boolean firstHeader = true;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            char prefix = line.charAt(0);
            String text = line.substring(2);
            if (prefix == 'V') {
                if (!firstHeader)
                    builder.append('\n');

                Utils.appendSpan(builder, text, new RelativeSizeSpan(1.5f), new StyleSpan(Typeface.ITALIC));
                firstHeader = false;
            } else {
                Map<Character, Integer> colorMap = new HashMap<>() {{
                    put('A', R.color.green_600);
                    put('C', R.color.amber_600);
                    put('F', R.color.purple_600);
                }};
                Map<Character, Integer> drawableMap = new HashMap<>() {{
                    put('A', R.drawable.ic_add_circle);
                    put('C', R.drawable.ic_change_circle);
                    put('F', R.drawable.ic_bug_fix);
                }};

                Drawable drawable = AppCompatResources.getDrawable(activity, drawableMap.get(prefix)).mutate();
                drawable.setColorFilter(new PorterDuffColorFilter(
                        ContextCompat.getColor(activity, colorMap.get(prefix)), PorterDuff.Mode.SRC_IN));
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

                Utils.appendSpan(builder, text, new DrawableBulletSpan(10, drawable));
            }

            builder.append('\n');
        }

        MaterialDialog dialog = DialogService.getBaseThemedDialog(activity)
                .title(R.string.changelog_title)
                .customView(R.layout.dialog_changelog, false)
                .build();

        assert dialog.getCustomView() != null;
        View customView = dialog.getCustomView();
        TextView textView = customView.findViewById(R.id.changelog_text);
        textView.setText(builder);

        CheckedTextView checkedTextView = customView.findViewById(R.id.show_next_update);
        checkedTextView.setChecked(Settings.getShowChangelogs());
        checkedTextView.setOnClickListener(v -> {
            boolean value = !Settings.getShowChangelogs();
            checkedTextView.setChecked(value);
            Settings.setShowChangelogs(value);
        });

        Button doneButton = customView.findViewById(R.id.done_button);
        doneButton.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        Settings.setLastVersionCode(BuildConfig.VERSION_CODE);
    }
}
