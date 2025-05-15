package com.perflyst.twire.adapters;

import static com.perflyst.twire.misc.Utils.appendSpan;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.R;
import com.perflyst.twire.misc.GlideImageSpan;
import com.perflyst.twire.model.Badge;
import com.perflyst.twire.model.ChatMessage;
import com.perflyst.twire.model.Emote;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.views.recyclerviews.ChatRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import timber.log.Timber;

/**
 * Created by SebastianRask on 03-03-2016.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ContactViewHolder> {
    private final List<ChatMessage> messages;
    private final ChatRecyclerView mRecyclerView;
    private final Activity context;
    private final ChatAdapterCallback mCallback;
    private final boolean isNightTheme;
    private final float textSize;

    public ChatAdapter(ChatRecyclerView aRecyclerView, Activity aContext, ChatAdapterCallback aCallback) {
        messages = new ArrayList<>();
        mRecyclerView = aRecyclerView;
        context = aContext;
        mCallback = aCallback;

        isNightTheme = Settings.isDarkTheme();
        textSize = aContext.getResources().getDimension(R.dimen.chat_message_text_size) * getTextScale();
    }

    @Override
    @NonNull
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.chat_message, parent, false);

        return new ContactViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ContactViewHolder holder, int position) {
        try {
            final ChatMessage message = messages.get(position);
            if (message.name == null) {
                return;
            }

            final SpannableStringBuilder builder = new SpannableStringBuilder();
            if (!message.systemMessage.isEmpty()) {
                appendSpan(builder, message.systemMessage, new ForegroundColorSpan(Color.GRAY));
                holder.message.setBackgroundResource(R.drawable.system_message);
            } else {
                holder.message.setBackgroundResource(0);
            }

            if (!message.message.isEmpty()) {
                if (!message.systemMessage.isEmpty()) builder.append('\n');

                for (Badge badge : message.badges) {
                    if (badge == null) {continue;}

                    final GlideImageSpan badgeSpan = new GlideImageSpan(context, badge.getUrl(2), holder.message, 36, 1, badge.color);
                    appendSpan(builder, "  ", badgeSpan).append(" ");
                }

                int nameColor = getNameColor(message.color);
                appendSpan(builder, message.name, new ForegroundColorSpan(nameColor), new StyleSpan(Typeface.BOLD));

                int preLength = builder.length();
                String beforeMessage = ": ";
                String messageWithPre = beforeMessage + message.message;
                appendSpan(builder, messageWithPre, new ForegroundColorSpan(getMessageColor()));

                checkForLink(builder.toString(), builder);

                for (var entry : message.emotes.entrySet()) {
                    Integer emotePosition = entry.getKey();
                    final Emote emote = entry.getValue();
                    final int fromPosition = emotePosition + preLength;
                    final int toPosition = emotePosition + emote.keyword.length() - 1 + preLength;

                    int emoteSize = Settings.getEmoteSize();
                    int emotePixels = emoteSize == 1 ? 28 : emoteSize == 2 ? 56 : 112;

                    final GlideImageSpan emoteSpan = new GlideImageSpan(context, emote.getEmoteUrl(emoteSize, isNightTheme), holder.message, emotePixels, (float) emote.getBestAvailableSize(emoteSize) / emoteSize);

                    builder.setSpan(emoteSpan, fromPosition + beforeMessage.length(), toPosition + 1 + beforeMessage.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }

            if (message.isHighlight) {
                holder.message.setBackgroundColor(Service.getColorAttribute(androidx.appcompat.R.attr.colorAccent, R.color.accent, context));
            }

            holder.message.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            holder.message.setText(builder);
            holder.message.setMovementMethod(LinkMovementMethod.getInstance());
            holder.message.setOnClickListener(view -> mCallback.onMessageClicked(builder, message.name, message.message));

        } catch (Exception e) {
            //In case twitch doesn't comply to their own API.
            Timber.d("Failed to show Message");
            Timber.e(e);
        }
    }

    public void getNamesThatMatches(String match, List<String> suggestions) {
        for (ChatMessage message : messages) {
            String name = message.name;
            if (name.toLowerCase().matches("^" + match + "\\w+") && !suggestions.contains(name)) {
                suggestions.add(name);
            }
        }

        Collections.sort(suggestions);
    }

    private void checkForLink(String message, SpannableStringBuilder spanBuilder) {
        Matcher linkMatcher = Patterns.WEB_URL.matcher(message);
        while (linkMatcher.find()) {
            String url = linkMatcher.group(0);

            if (!url.matches("^https?://.+"))
                url = "http://" + url;

            final String finalUrl = url;
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View view) {
                    CustomTabsIntent.Builder mTabs = new CustomTabsIntent.Builder();
                    mTabs.setStartAnimations(context, R.anim.slide_in_bottom_anim, R.anim.fade_out_semi_anim);
                    mTabs.setExitAnimations(context, R.anim.fade_in_semi_anim, R.anim.slide_out_bottom_anim);
                    mTabs.build().launchUrl(context, Uri.parse(finalUrl));

                    mRecyclerView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                }
            };

            spanBuilder.setSpan(clickableSpan, linkMatcher.start(), linkMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private float getTextScale() {
        int settingsSize = Settings.getMessageSize();
        switch (settingsSize) {
            case 1:
                return 0.9f;
            case 2:
                return 1.1f;
            case 3:
                return 1.2f;
        }
        return 1f;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private int getNameColor(String colorFromAPI) {
        String BLACK_TEXT = "#000000";
        if (colorFromAPI == null || colorFromAPI.equals(BLACK_TEXT)) {
            if (isNightTheme) {
                return ContextCompat.getColor(context, R.color.blue_500);
            } else {
                return ContextCompat.getColor(context, R.color.blue_700);
            }
        }

        String WHITE_TEXT = "#FFFFFF";
        if (colorFromAPI.equals(WHITE_TEXT) && !isNightTheme) {
            return ContextCompat.getColor(context, R.color.blue_700);
        }

        return Color.parseColor(colorFromAPI);
    }

    private int getMessageColor() {
        if (isNightTheme) {
            return ContextCompat.getColor(context, R.color.white_text);
        } else {
            return ContextCompat.getColor(context, R.color.black_text);
        }
    }

    /**
     * Add a message and make sure it is in view
     */
    public void add(ChatMessage message) {
        messages.add(message);

        notifyItemInserted(messages.size() - 1);
        if (!mRecyclerView.isScrolled()) {
            checkSize();
            mRecyclerView.scrollToPosition(messages.size() - 1);
        }
        Timber.v("Adding Message %s", message.message);
    }

    public void clear() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }

    public void clear(String target) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (!message.getId().equals(target)) {
                continue;
            }

            messages.remove(i);
            notifyItemRemoved(i);
        }
    }

    /**
     * Checks if the data structure contains more items that the specified max amount, if so. Remove the first item in the structure.
     * Notifies observers that item has been removed
     */
    private void checkSize() {
        int MAX_MESSAGES = 150;
        if (messages.size() > MAX_MESSAGES) {
            int messagesOverLimit = messages.size() - MAX_MESSAGES;
            for (int i = 0; i < messagesOverLimit; i++) {
                messages.remove(0);
                notifyItemRemoved(0);
            }
        }
    }

    public interface ChatAdapterCallback {
        void onMessageClicked(SpannableStringBuilder formattedString, String userName, String message);
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private final TextView message;

        ContactViewHolder(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.txt_message);
        }
    }
}
