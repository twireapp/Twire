package com.perflyst.twire.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.perflyst.twire.R
import com.perflyst.twire.adapters.MentionAdapter.SuggestionViewHolder

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
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.mention_suggestion, parent, false)

        return SuggestionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.mName.text = mentionSuggestions[position]
        holder.itemView.setOnClickListener { view: View? ->
            mDelegate.onSuggestionClick(
                holder.mName.getText().toString()
            )
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

    class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mName: TextView = itemView.findViewById(R.id.txtSuggestion)
    }
}
