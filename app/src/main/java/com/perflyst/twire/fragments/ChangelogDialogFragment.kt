package com.perflyst.twire.fragments

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.CheckedTextView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.perflyst.twire.BuildConfig
import com.perflyst.twire.R
import com.perflyst.twire.misc.DrawableBulletSpan
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.service.DialogService
import com.perflyst.twire.service.Settings.lastVersionCode
import com.perflyst.twire.service.Settings.showChangelogs
import com.rey.material.widget.Button

class ChangelogDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity: Activity = requireActivity()

        val builder = SpannableStringBuilder()
        val lines =
            activity.resources.getString(R.string.changelog_lines).split("\n".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
        var firstHeader = true
        for (line in lines) {
            var line = line
            line = line.trim { it <= ' ' }
            if (line.isEmpty()) {
                continue
            }

            val prefix = line[0]
            val text = line.substring(2)
            if (prefix == 'V') {
                if (!firstHeader) builder.append('\n')

                Utils.appendSpan(builder, text, RelativeSizeSpan(1.5f), StyleSpan(Typeface.ITALIC))
                firstHeader = false
            } else {
                val colorMap: MutableMap<Char, Int> = object : HashMap<Char, Int>() {
                    init {
                        put('A', R.color.green_600)
                        put('C', R.color.amber_600)
                        put('F', R.color.purple_600)
                    }
                }
                val drawableMap: MutableMap<Char, Int> = object : HashMap<Char, Int>() {
                    init {
                        put('A', R.drawable.ic_add_circle)
                        put('C', R.drawable.ic_change_circle)
                        put('F', R.drawable.ic_bug_fix)
                    }
                }

                val drawable =
                    AppCompatResources.getDrawable(activity, drawableMap[prefix]!!)!!
                        .mutate()
                drawable.colorFilter = PorterDuffColorFilter(
                    ContextCompat.getColor(activity, colorMap[prefix]!!),
                    PorterDuff.Mode.SRC_IN
                )
                drawable.setBounds(
                    0,
                    0,
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight
                )

                Utils.appendSpan(builder, text, DrawableBulletSpan(10, drawable))
            }

            builder.append('\n')
        }

        val dialog = DialogService.getBaseThemedDialog(activity)
            .title(R.string.changelog_title)
            .customView(R.layout.dialog_changelog, false)
            .build()

        checkNotNull(dialog.customView)
        val customView: View = dialog.customView!!
        val textView = customView.findViewById<TextView>(R.id.changelog_text)
        textView.text = builder

        val checkedTextView = customView.findViewById<CheckedTextView>(R.id.show_next_update)
        checkedTextView.isChecked = showChangelogs
        checkedTextView.setOnClickListener { v: View? ->
            val value = !showChangelogs
            checkedTextView.isChecked = value
            showChangelogs = value
        }

        val doneButton = customView.findViewById<Button>(R.id.done_button)
        doneButton.setOnClickListener { v: View? -> dialog.dismiss() }

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        lastVersionCode = BuildConfig.VERSION_CODE
    }
}
