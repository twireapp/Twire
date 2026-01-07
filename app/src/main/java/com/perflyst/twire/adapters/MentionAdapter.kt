package com.perflyst.twire.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.perflyst.twire.adapters.MentionAdapter.SuggestionViewHolder
import com.perflyst.twire.databinding.MentionSuggestionBinding

/**
 * Created by Sebastian Rask on 29-01-2017.
 */
class MentionAdapter(private val mDelegate: MentionAdapterDelegate) :
    RecyclerView.Adapter<SuggestionViewHolder?>() {
    private var mentionSuggestions: MutableList<String>

    init {
        mentionSuggestions = ArrayList<String>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = MentionSuggestionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.binding.txtSuggestion.text = mentionSuggestions[position]
        holder.itemView.setOnClickListener {
            mDelegate.onSuggestionClick(holder.binding.txtSuggestion.getText().toString())
        }
    }

    override fun getItemCount(): Int {
        return mentionSuggestions.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSuggestions(suggestions: MutableList<String>) {
        mentionSuggestions = suggestions
        notifyDataSetChanged()
    }

    fun interface MentionAdapterDelegate {
        fun onSuggestionClick(suggestion: String?)
    }

    class SuggestionViewHolder(binding: MentionSuggestionBinding) :
        BindingViewHolder<MentionSuggestionBinding>(binding)
}
