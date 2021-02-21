package com.perflyst.twire.adapters;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.perflyst.twire.R;
import com.perflyst.twire.model.Panel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sebastian Rask on 24-02-2017.
 */

public class PanelAdapter extends RecyclerView.Adapter<PanelAdapter.PanelViewHolder> {
    private final List<Panel> mPanels;
    private final Activity mActivity;

    public PanelAdapter(Activity mActivity) {
        this.mActivity = mActivity;
        this.mPanels = new ArrayList<>();
    }

    public void addPanels(List<Panel> panels) {
        mPanels.addAll(panels);
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public PanelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.cell_panel, parent, false);

        return new PanelViewHolder(itemView);
    }

    @Override
    public int getItemCount() {
        return mPanels.size();
    }

    @Override
    public void onBindViewHolder(@NonNull final PanelViewHolder holder, int position) {
        Panel mPanel = mPanels.get(position);

        String imageUrl = mPanel.getmImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
            Glide.with(mActivity).load(mPanel.getmImageUrl()).into(holder.mImageView);
        }

        final String link = mPanel.getmLinkUrl();
        if (link != null && !link.isEmpty() && !link.equals("null")) {
            holder.itemView.setOnClickListener(view -> {
                CustomTabsIntent.Builder mTabs = new CustomTabsIntent.Builder();
                mTabs.setStartAnimations(mActivity, R.anim.slide_in_bottom_anim, R.anim.fade_out_semi_anim);
                mTabs.setExitAnimations(mActivity, R.anim.fade_in_semi_anim, R.anim.slide_out_bottom_anim);

                try {
                    mTabs.build().launchUrl(mActivity, Uri.parse(link));

                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            });
        }


        if (mPanel.getmHtml().isEmpty() || mPanel.getmHtml().equals("null")) {
            holder.mHtmlText.setVisibility(View.GONE);
        } else {
            holder.mHtmlText.setVisibility(View.VISIBLE);
            holder.mHtmlText.setText(HtmlCompat.fromHtml(mPanel.getmHtml(), HtmlCompat.FROM_HTML_MODE_LEGACY));
            holder.mHtmlText.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    static class PanelViewHolder extends RecyclerView.ViewHolder {
        final ImageView mImageView;
        final TextView mHtmlText;

        PanelViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.panel_image);
            mHtmlText = itemView.findViewById(R.id.panel_html);
        }
    }
}
