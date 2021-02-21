package com.perflyst.twire.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sebastian Rask on 29-01-2017.
 */

public class MentionAdapter extends RecyclerView.Adapter<MentionAdapter.SuggestionViewHolder> {
    private final MentionAdapterDelegate mDelegate;
    private List<String> mentionSuggestions;

    public MentionAdapter(MentionAdapterDelegate aDelegate) {
        mDelegate = aDelegate;
        mentionSuggestions = new ArrayList<>();
    }

    @Override
    @NonNull
    public SuggestionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.mention_suggestion, parent, false);

        return new SuggestionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final SuggestionViewHolder holder, int position) {
        holder.mName.setText(mentionSuggestions.get(position));
        holder.itemView.setOnClickListener(view -> mDelegate.onSuggestionClick(holder.mName.getText().toString()));
    }

    @Override
    public int getItemCount() {
        return mentionSuggestions.size();
    }

    public void setSuggestions(List<String> suggestions) {
        mentionSuggestions = suggestions;
        notifyDataSetChanged();
    }

    public interface MentionAdapterDelegate {
        void onSuggestionClick(String suggestion);
    }

    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        final TextView mName;

        SuggestionViewHolder(View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.txtSuggestion);
        }
    }
}
