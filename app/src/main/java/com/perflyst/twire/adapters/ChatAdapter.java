package com.perflyst.twire.adapters;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.Patterns;
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
import com.perflyst.twire.model.ChatEmote;
import com.perflyst.twire.model.ChatMessage;
import com.perflyst.twire.model.Emote;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.views.recyclerviews.ChatRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import static com.perflyst.twire.misc.Utils.appendSpan;

/**
 * Created by SebastianRask on 03-03-2016.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ContactViewHolder> implements Drawable.Callback {
    private final String LOG_TAG = getClass().getSimpleName();
    private final List<ChatMessage> messages;
    private final ChatRecyclerView mRecyclerView;
    private final Activity context;
    private final Settings settings;
    private final ChatAdapterCallback mCallback;
    private final boolean isNightTheme;

    public ChatAdapter(ChatRecyclerView aRecyclerView, Activity aContext, ChatAdapterCallback aCallback) {
        messages = new ArrayList<>();
        mRecyclerView = aRecyclerView;
        context = aContext;
        mCallback = aCallback;
        settings = new Settings(context);

        isNightTheme = settings.getTheme().equals(context.getString(R.string.night_theme_name)) || settings.getTheme().equals(context.getString(R.string.true_night_theme_name));
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
            if (message.getMessage().equals("Test")) {
                Log.d(LOG_TAG, "Binding Message for user");
                Log.d(LOG_TAG, "Message: " + message.toString());
            }

            if (message.getName() == null) {
                return;
            }

            final SpannableStringBuilder builder = new SpannableStringBuilder();
            for (Badge badge : message.getBadges()) {
                final GlideImageSpan badgeSpan = new GlideImageSpan(context, badge.getUrl(2), holder.message, builder, 36, 1, badge.color);
                appendSpan(builder, "  ", badgeSpan).append(" ");
            }

            int nameColor = getNameColor(message.getColor());
            appendSpan(builder, message.getName(), new ForegroundColorSpan(nameColor), new StyleSpan(Typeface.BOLD));

            int preLength = builder.length();
            String beforeMessage = ": ";
            String messageWithPre = beforeMessage + message.getMessage();
            appendSpan(builder, messageWithPre, new ForegroundColorSpan(getMessageColor()));

            checkForLink(builder.toString(), builder);

            for (ChatEmote chatEmote : message.getEmotes()) {
                for (Integer emotePosition : chatEmote.getPositions()) {
                    final Emote emote = chatEmote.getEmote();
                    final int fromPosition = emotePosition + preLength;
                    final int toPosition = emotePosition + emote.getKeyword().length() - 1 + preLength;

                    int emoteSize = settings.getEmoteSize();
                    int emotePixels = emoteSize == 1 ? 28 : emoteSize == 2 ? 56 : 112;

                    final GlideImageSpan emoteSpan = new GlideImageSpan(context, emote.getEmoteUrl(emoteSize), holder.message, builder, emotePixels, (float) emote.getBestAvailableSize(emoteSize) / emoteSize);

                    holder.message.setTextIsSelectable(true);

                    builder.setSpan(emoteSpan, fromPosition + beforeMessage.length(), toPosition + 1 + beforeMessage.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }

            if (message.isHighlight()) {
                holder.message.setBackgroundColor(Service.getColorAttribute(R.attr.colorAccent, R.color.accent, context));
            }

            builder.setSpan(new RelativeSizeSpan(getTextSize()), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.message.setText(builder);
            holder.message.setMovementMethod(LinkMovementMethod.getInstance());
            holder.message.setOnClickListener(view -> mCallback.onMessageClicked(builder, message.getName(), message.getMessage()));

        } catch (Exception e) {
            //In case twitch doesn't comply to their own API.
            Log.d(LOG_TAG, "Failed to show Message");
            e.printStackTrace();
        }
    }

    public List<String> getNamesThatMatches(String match) {
        if (match.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();

        for (ChatMessage message : messages) {
            if (message.getName().toLowerCase().matches("^" + match.toLowerCase() + "\\w+") && !result.contains(message.getName())) {
                result.add(message.getName());
            }
        }

        Collections.sort(result);
        return result;
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

    private float getTextSize() {
        int settingsSize = settings.getMessageSize();
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
        Log.d(LOG_TAG, "Adding Message " + message.getMessage());
    }

    public void clear() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }

    /**
     * Checks if the data structure contains more items that the specified max amount, if so. Remove the first item in the structure.
     * Notifies observers that item has been removed
     */
    private void checkSize() {
        int MAX_MESSAGES = 500;
        if (messages.size() > MAX_MESSAGES) {
            int messagesOverLimit = messages.size() - MAX_MESSAGES;
            for (int i = 0; i < messagesOverLimit; i++) {
                messages.remove(0);
                notifyItemRemoved(0);
            }
        }
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        Log.d(LOG_TAG, "Invalidate drawable");
/*
        if (drawable instanceof GifDrawable) {
            GifDrawable gifDrawable = (GifDrawable) drawable;
            gifDrawable.stop();
            Log.d(LOG_TAG, "Stopping drawable");
        }
*/
    }

    @Override
    public void scheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable, long l) {
        Log.d(LOG_TAG, "Schedule drawable");

    }

    @Override
    public void unscheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable) {
        Log.d(LOG_TAG, "Unschedule drawable");
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
