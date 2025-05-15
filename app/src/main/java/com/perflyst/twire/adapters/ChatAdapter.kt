package com.perflyst.twire.adapters

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Patterns
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.perflyst.twire.R
import com.perflyst.twire.adapters.ChatAdapter.ContactViewHolder
import com.perflyst.twire.misc.GlideImageSpan
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.model.ChatMessage
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.emoteSize
import com.perflyst.twire.service.Settings.isDarkTheme
import com.perflyst.twire.service.Settings.messageSize
import com.perflyst.twire.views.recyclerviews.ChatRecyclerView
import timber.log.Timber
import java.util.Locale

/**
 * Created by SebastianRask on 03-03-2016.
 */
class ChatAdapter(
    private val mRecyclerView: ChatRecyclerView,
    private val context: Activity,
    private val mCallback: ChatAdapterCallback
) : RecyclerView.Adapter<ContactViewHolder?>() {
    private val messages: MutableList<ChatMessage> = ArrayList()
    private val isNightTheme: Boolean = isDarkTheme
    private val textSize: Float

    init {
        textSize =
            context.resources.getDimension(R.dimen.chat_message_text_size) * this.textScale
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.chat_message, parent, false)

        return ContactViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        try {
            val message = messages[position]

            val builder = SpannableStringBuilder()
            if (!message.systemMessage.isEmpty()) {
                Utils.appendSpan(builder, message.systemMessage, ForegroundColorSpan(Color.GRAY))
                holder.message.setBackgroundResource(R.drawable.system_message)
            } else {
                holder.message.setBackgroundResource(0)
            }

            if (!message.message.isEmpty()) {
                if (!message.systemMessage.isEmpty()) builder.append('\n')

                for (badge in message.badges) {
                    if (badge == null) {
                        continue
                    }

                    val badgeSpan = GlideImageSpan(
                        context,
                        badge.getUrl(2),
                        holder.message,
                        36,
                        1f,
                        badge.color
                    )
                    Utils.appendSpan(builder, "  ", badgeSpan).append(" ")
                }

                val nameColor = getNameColor(message.color)
                Utils.appendSpan(
                    builder,
                    message.name,
                    ForegroundColorSpan(nameColor),
                    StyleSpan(Typeface.BOLD)
                )

                val preLength = builder.length
                val beforeMessage = ": "
                val messageWithPre = beforeMessage + message.message
                Utils.appendSpan(
                    builder, messageWithPre, ForegroundColorSpan(
                        this.messageColor
                    )
                )

                checkForLink(builder.toString(), builder)

                for (entry in message.emotes.entries) {
                    val emotePosition = entry.key
                    val emote = entry.value
                    val fromPosition = emotePosition + preLength
                    val toPosition = emotePosition + emote.keyword.length - 1 + preLength

                    val emoteSize = emoteSize
                    val emotePixels = if (emoteSize == 1) 28 else if (emoteSize == 2) 56 else 112

                    val emoteSpan = GlideImageSpan(
                        context,
                        emote.getEmoteUrl(emoteSize, isNightTheme),
                        holder.message,
                        emotePixels,
                        emote.getBestAvailableSize(emoteSize).toFloat() / emoteSize
                    )

                    builder.setSpan(
                        emoteSpan,
                        fromPosition + beforeMessage.length,
                        toPosition + 1 + beforeMessage.length,
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }
            }

            if (message.isHighlight) {
                holder.message.setBackgroundColor(
                    Service.getColorAttribute(
                        androidx.appcompat.R.attr.colorAccent,
                        R.color.accent,
                        context
                    )
                )
            }

            holder.message.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            holder.message.text = builder
            holder.message.movementMethod = LinkMovementMethod.getInstance()
            holder.message.setOnClickListener { view: View? ->
                mCallback.onMessageClicked(
                    builder,
                    message.name,
                    message.message
                )
            }
        } catch (e: Exception) {
            //In case twitch doesn't comply to their own API.
            Timber.d("Failed to show Message")
            Timber.e(e)
        }
    }

    fun getNamesThatMatches(match: String, suggestions: MutableList<String>) {
        for (message in messages) {
            val name = message.name
            if (name.lowercase(Locale.getDefault())
                    .matches(("^$match\\w+").toRegex()) && !suggestions.contains(name)
            ) {
                suggestions.add(name)
            }
        }

        suggestions.sort()
    }

    private fun checkForLink(message: String, spanBuilder: SpannableStringBuilder) {
        val linkMatcher = Patterns.WEB_URL.matcher(message)
        while (linkMatcher.find()) {
            var url = linkMatcher.group(0)

            if (!url!!.matches("^https?://.+".toRegex())) url = "http://$url"

            val finalUrl = url
            val clickableSpan: ClickableSpan = object : ClickableSpan() {
                override fun onClick(view: View) {
                    val mTabs = CustomTabsIntent.Builder()
                    mTabs.setStartAnimations(
                        context,
                        R.anim.slide_in_bottom_anim,
                        R.anim.fade_out_semi_anim
                    )
                    mTabs.setExitAnimations(
                        context,
                        R.anim.fade_in_semi_anim,
                        R.anim.slide_out_bottom_anim
                    )
                    mTabs.build().launchUrl(context, finalUrl.toUri())

                    mRecyclerView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }

            spanBuilder.setSpan(
                clickableSpan,
                linkMatcher.start(),
                linkMatcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private val textScale: Float
        get() {
            val settingsSize = messageSize
            when (settingsSize) {
                1 -> return 0.9f
                2 -> return 1.1f
                3 -> return 1.2f
            }
            return 1f
        }

    override fun getItemCount(): Int {
        return messages.size
    }

    private fun getNameColor(colorFromAPI: String?): Int {
        val blackText = "#000000"
        if (colorFromAPI == null || colorFromAPI == blackText) {
            return if (isNightTheme) {
                ContextCompat.getColor(context, R.color.blue_500)
            } else {
                ContextCompat.getColor(context, R.color.blue_700)
            }
        }

        val whiteText = "#FFFFFF"
        if (colorFromAPI == whiteText && !isNightTheme) {
            return ContextCompat.getColor(context, R.color.blue_700)
        }

        return colorFromAPI.toColorInt()
    }

    private val messageColor: Int
        get() {
            return if (isNightTheme) {
                ContextCompat.getColor(context, R.color.white_text)
            } else {
                ContextCompat.getColor(context, R.color.black_text)
            }
        }

    /**
     * Add a message and make sure it is in view
     */
    fun add(message: ChatMessage) {
        messages.add(message)

        notifyItemInserted(messages.size - 1)
        if (!mRecyclerView.isScrolled) {
            checkSize()
            mRecyclerView.scrollToPosition(messages.size - 1)
        }
        Timber.v("Adding Message %s", message.message)
    }

    fun clear() {
        val size = messages.size
        messages.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun clear(target: String?) {
        for (i in messages.indices.reversed()) {
            val message = messages[i]
            if (message.id != target) {
                continue
            }

            messages.removeAt(i)
            notifyItemRemoved(i)
        }
    }

    /**
     * Checks if the data structure contains more items that the specified max amount, if so. Remove the first item in the structure.
     * Notifies observers that item has been removed
     */
    private fun checkSize() {
        val maxMessages = 150
        if (messages.size > maxMessages) {
            val messagesOverLimit = messages.size - maxMessages
            repeat(messagesOverLimit) {
                messages.removeAt(0)
                notifyItemRemoved(0)
            }
        }
    }

    interface ChatAdapterCallback {
        fun onMessageClicked(
            formattedString: SpannableStringBuilder?,
            userName: String?,
            message: String?
        )
    }

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val message: TextView = itemView.findViewById(R.id.txt_message)
    }
}
