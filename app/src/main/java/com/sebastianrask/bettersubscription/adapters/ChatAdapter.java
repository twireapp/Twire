package com.sebastianrask.bettersubscription.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Vibrator;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;

import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.model.ChatEmote;
import com.sebastianrask.bettersubscription.model.ChatMessage;
import com.sebastianrask.bettersubscription.service.Service;
import com.sebastianrask.bettersubscription.service.Settings;
import com.sebastianrask.bettersubscription.views.recyclerviews.ChatRecyclerView;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by SebastianRask on 03-03-2016.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ContactViewHolder> implements Drawable.Callback {
	private final String LOG_TAG = getClass().getSimpleName();
	private final int MAX_MESSAGES = 500;
	private List<ChatMessage> messages;
	private ChatRecyclerView mRecyclerView;
	private Activity context;
	private Settings settings;
	private Pattern linkPattern;
	private Matcher linkMatcher;

	private ImageSpan imageMod;
	private ImageSpan imageTurbo;
	private ImageSpan imageSub;
	private int emoteAlignment;
	private ChatAdapterCallback mCallback;

	private boolean isNightTheme;
	private final String WHITE_TEXT = "#FFFFFF";
	private final String BLACK_TEXT = "#000000";
	private final String EMPTY_MESSAGE = "";
	private final String PREMESSAGE = ": ";

	public ChatAdapter(ChatRecyclerView aRecyclerView, Activity aContext, ChatAdapterCallback aCallback) {
		messages = new ArrayList<>();
		mRecyclerView = aRecyclerView;
		context = aContext;
		mCallback = aCallback;
		settings = new Settings(context);
		linkPattern = Pattern.compile("((http|https|ftp)\\:\\/\\/[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,3}(:[a-zA-Z0-9]*)?\\/?([a-zA-Z0\u200C123456789\\-\\._\\?\\,\\'\\/\\\\\\+&amp;%\\$#\\=~])*[^\\.\\,\\)\\(\\s])");

		emoteAlignment = DynamicDrawableSpan.ALIGN_BASELINE;
		imageMod = new ImageSpan(context, R.drawable.ic_moderator, emoteAlignment);
		imageTurbo = new ImageSpan(context, R.drawable.ic_twitch_turbo, emoteAlignment);
		isNightTheme = settings.getTheme().equals(context.getString(R.string.night_theme_name)) || settings.getTheme().equals(context.getString(R.string.true_night_theme_name));
	}

	@Override
	public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View itemView = LayoutInflater
								.from(parent.getContext())
								.inflate(R.layout.chat_message, parent, false);

		return new ContactViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(final ContactViewHolder holder, int position) {
		try {
			final ChatMessage message = messages.get(position);
			if (message.getMessage().equals("Test")) {
				Log.d(LOG_TAG, "Binding Message for user");
				Log.d(LOG_TAG, "Message: " + message.toString());
			}
			if(imageSub == null) {
				Bitmap resizedSubscriberEmote = Service.getResizedBitmap(message.getSubscriberIcon(),
																				imageMod.getDrawable().getIntrinsicWidth(),
																				imageMod.getDrawable().getIntrinsicHeight());
				imageSub = new ImageSpan(context, resizedSubscriberEmote, emoteAlignment);
			}


			final SpannableStringBuilder builder = new SpannableStringBuilder();
			if(message.isMod()) {
				builder
						.append("  ")
						.setSpan(imageMod, 0, 1, 0);
			}

			if(message.isSubscriber()) {
				builder
						.append("  ")
						.setSpan(imageSub, builder.length() - 2, builder.length() - 1, 0);
			}

			if(message.isTurbo()) {
				builder
						.append("  ")
						.setSpan(imageTurbo, builder.length() - 2, builder.length() - 1, 0);
			}

			if (message.getName() == null) {
				return;
			}

			builder.append(message.getName());
			int nameColor = getNameColor(message.getColor());
			builder.setSpan(new ForegroundColorSpan(nameColor), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			String messageWithPre = PREMESSAGE + message.getMessage();
			final SpannableStringBuilder resultMessage = new SpannableStringBuilder(messageWithPre);
			resultMessage.setSpan(new ForegroundColorSpan(getMessageColor()), 0, resultMessage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			checkForLink(messageWithPre, resultMessage);

			for(ChatEmote emote : message.getEmotes()) {
				for(String emotePosition : emote.getEmotePositions()) {
					String[] toAndFrom = emotePosition.split("-");
					final int fromPosition = Integer.parseInt(toAndFrom[0]);
					final int toPosition = Integer.parseInt(toAndFrom[1]);


					final ImageSpan emoteSpan = new ImageSpan(context, emote.getEmoteBitmap(), emoteAlignment);
					resultMessage.setSpan(emoteSpan, fromPosition + PREMESSAGE.length(), toPosition + 1 + PREMESSAGE.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

					holder.message.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
					holder.message.setTextIsSelectable(true);

					/*
					if (BuildConfig.DEBUG && false) {
						Glide
								.with(context)
								.load("https://cdn.betterttv.net/emote/561c1cb4f291bd650621b2c5/2x")
								.asGif()
								.into(new SimpleTarget<GifDrawable>() {
									@Override
									public void onResourceReady(GifDrawable gifDrawable, GlideAnimation<? super GifDrawable> glideAnimation) {
										gifDrawable.setBounds(0, 0, gifDrawable.getIntrinsicWidth(), gifDrawable.getIntrinsicHeight());
										gifDrawable.setCallback(ChatAdapter.this);

										final SpannableStringBuilder ssb = new SpannableStringBuilder("test\ufffc");

										ssb.setSpan(new ImageSpan(gifDrawable), ssb.length() - 1, ssb.length(), 0);
										holder.message.setText(ssb);
										gifDrawable.stop();
										gifDrawable.start();
										Log.d(LOG_TAG, "Gif frames: " + gifDrawable.getFrameCount());
										//Log.d(LOG_TAG, "Setting GIF");
									}
								});
					}
					*/
				}
			}

			if (message.isHighlight()) {
				holder.message.setBackgroundColor(Service.getColorAttribute(R.attr.colorAccent, R.color.accent, context));
			}

			builder.append(resultMessage);
			builder.setSpan(new RelativeSizeSpan(getTextSize()), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			holder.message.setText(builder);
			holder.message.setMovementMethod(LinkMovementMethod.getInstance());

			holder.itemView.findViewById(R.id.message_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					mCallback.onMessageClicked(builder, message.getName(), message.getMessage());
					Log.d("Click", "Button clicked");
				}
			});

		} catch(Exception e) {
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

	private void checkForLink(String message, SpannableStringBuilder spanbuilder) {
		linkMatcher = linkPattern.matcher(message);
		while(linkMatcher.find()) {
			final String url = linkMatcher.group(1);

			ClickableSpan clickableSpan = new ClickableSpan() {
				@Override
				public void onClick(View view) {
					CustomTabsIntent.Builder mTabs = new CustomTabsIntent.Builder();
					mTabs.setStartAnimations(context, R.anim.slide_in_bottom_anim, R.anim.fade_out_semi_anim);
					mTabs.setExitAnimations(context, R.anim.fade_in_semi_anim, R.anim.slide_out_bottom_anim);
					mTabs.build().launchUrl(context, Uri.parse(url));

					Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
					vibrator.vibrate(25);
				}
			};

			int start = message.indexOf(url);
			spanbuilder.setSpan(clickableSpan, start, start + url.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}

	private float getTextSize() {
		int settingsSize = settings.getMessageSize();
		switch (settingsSize){
			case 1: return 0.9f;
			case 2: return 1.1f;
			case 3: return 1.2f;
		}
		return 1f;
	}

	@Override
	public int getItemCount() {
		return messages.size();
	}

	private int getNameColor(String colorFromAPI) {
		if (colorFromAPI.equals(EMPTY_MESSAGE) || colorFromAPI.equals(BLACK_TEXT)) {
			if (isNightTheme) {
				return ContextCompat.getColor(context, R.color.blue_500);
			} else {
				return ContextCompat.getColor(context, R.color.blue_700);
			}
		}

		if(colorFromAPI.equals(WHITE_TEXT) && !isNightTheme) {
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
		if(!mRecyclerView.isScrolled()) {
			checkSize();
			mRecyclerView.scrollToPosition(messages.size() - 1);
		}
		Log.d(LOG_TAG, "Adding Message " + message.getMessage());
	}

	/**
	 * Checks if the data structure contains more items that the specified max amount, if so. Remove the first item in the structure.
	 * Notifies observers that item has been removed
	 */
	private void checkSize() {
		if(messages.size() > MAX_MESSAGES) {
			int messagesOverLimit = messages.size() - MAX_MESSAGES;
			for(int i = 0; i < messagesOverLimit; i++) {
				messages.remove(0);
				notifyItemRemoved(0);
			}
		}
	}

	@Override
	public void invalidateDrawable(Drawable drawable) {
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
	public void scheduleDrawable(Drawable drawable, Runnable runnable, long l) {
		Log.d(LOG_TAG, "Schedule drawable");

	}

	@Override
	public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
		Log.d(LOG_TAG, "Unschedule drawable");
	}

	public class ContactViewHolder extends RecyclerView.ViewHolder {
		private TextView message;

		public ContactViewHolder(View itemView) {
			super(itemView);
			message = (TextView) itemView.findViewById(R.id.txtMessage);
		}
	}

	public interface ChatAdapterCallback {
		void onMessageClicked(SpannableStringBuilder formattedString, String userName, String message);
	}
}
