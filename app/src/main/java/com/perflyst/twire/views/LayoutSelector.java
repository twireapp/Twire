package com.perflyst.twire.views;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import androidx.annotation.ArrayRes;
import androidx.annotation.AttrRes;
import androidx.annotation.DimenRes;
import androidx.annotation.LayoutRes;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.widget.CompoundButtonCompat;

import com.perflyst.twire.R;
import com.perflyst.twire.service.Service;

/**
 * Created by Sebastian Rask on 13-05-2016.
 */
public class LayoutSelector {
    private final String[] layoutTitles;
    private final OnLayoutSelected selectCallback;
    private final Activity activity;
    @LayoutRes
    private final int previewLayout;
    private View layoutSelectorView;
    @AttrRes
    private int textColor = -1;
    @DimenRes
    private int previewMaxHeightRes = -1;
    private int selectedLayoutIndex = -1;
    private String selectedLayoutTitle = null;

    public LayoutSelector(@LayoutRes int previewLayoutRes, @ArrayRes int choices, OnLayoutSelected selectCallback, Activity activity) {
        previewLayout = previewLayoutRes;
        layoutTitles = activity.getResources().getStringArray(choices);
        this.selectCallback = selectCallback;
        this.activity = activity;
    }

    private void init() {
        layoutSelectorView = activity.getLayoutInflater().inflate(R.layout.stream_layout_preview, null);
        final RadioGroup rg = layoutSelectorView.findViewById(R.id.layouts_radiogroup);
        final FrameLayout previewWrapper = layoutSelectorView.findViewById(R.id.preview_wrapper);

        if (previewMaxHeightRes != -1) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) activity.getResources().getDimension(previewMaxHeightRes)
            );
            previewWrapper.setLayoutParams(lp);
            //previewWrapper.setMinimumHeight((int) activity.getResources().getDimension(previewMaxHeightRes));
        }

        ViewStub preview = layoutSelectorView.findViewById(R.id.layout_stub);
        preview.setLayoutResource(previewLayout);
        final View inflated = preview.inflate();

        for (int i = 0; i < layoutTitles.length; i++) {
            final String layoutTitle = layoutTitles[i];

            final AppCompatRadioButton radioButton = new AppCompatRadioButton(activity);
            radioButton.setText(layoutTitle);

            final int finalI = i;
            radioButton.setOnClickListener(v -> selectCallback.onSelected(layoutTitle, finalI, inflated));

            if (textColor != -1) {
                radioButton.setTextColor(Service.getColorAttribute(textColor, R.color.black_text, activity));

                ColorStateList colorStateList = new ColorStateList(
                        new int[][]{
                                new int[]{-android.R.attr.state_checked},
                                new int[]{android.R.attr.state_checked}
                        },
                        new int[]{

                                Color.GRAY, //Disabled
                                Service.getColorAttribute(R.attr.colorAccent, R.color.accent, activity), //Enabled
                        }
                );
                CompoundButtonCompat.setButtonTintList(radioButton, colorStateList);
            }


            radioButton.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, // Width
                    (int) activity.getResources().getDimension(R.dimen.layout_selector_height) // Height
            ));


            rg.addView(radioButton, i);


            if (selectedLayoutIndex != -1 && selectedLayoutIndex == i || selectedLayoutTitle != null && selectedLayoutTitle.equals(layoutTitle)) {
                radioButton.performClick();
            }
        }
    }

    public LayoutSelector setTextColorAttr(@AttrRes int textAppearanceResource) {
        textColor = textAppearanceResource;
        return this;
    }

    public LayoutSelector setSelectedLayoutIndex(int selectedLayoutIndex) {
        this.selectedLayoutIndex = selectedLayoutIndex;
        return this;
    }

    public LayoutSelector setSelectedLayoutTitle(String selectedLayoutTitle) {
        this.selectedLayoutTitle = selectedLayoutTitle;
        return this;
    }

    public LayoutSelector setPreviewMaxHeightRes(int previewMaxHeightRes) {
        this.previewMaxHeightRes = previewMaxHeightRes;
        return this;
    }

    public View build() {
        init();
        return layoutSelectorView;
    }

    public interface OnLayoutSelected {
        void onSelected(String title, int index, View previewView);
    }

}
