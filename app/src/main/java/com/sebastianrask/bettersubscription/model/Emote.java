package com.sebastianrask.bettersubscription.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * Created by Sebastian Rask Jepsen on 28/07/16.
 */
public class Emote implements Comparable<Emote>, Serializable {
	private String emoteId, emoteKeyword;
	private boolean isBetterTTVEmote, isTextEmote, isSubscriberEmote, isBetterTTVChannelEmote;

	public Emote(String emoteId, String emoteKeyword, boolean isBetterTTVEmote) {
		this.emoteId = emoteId;
		this.emoteKeyword = emoteKeyword;
		this.isBetterTTVEmote = isBetterTTVEmote;
		this.isTextEmote = false;
	}

	public Emote(String textEmoteUnicode) {
		emoteKeyword = textEmoteUnicode;
		isTextEmote = true;
	}

	public boolean isBetterTTVChannelEmote() {
		return isBetterTTVChannelEmote;
	}

	public void setBetterTTVChannelEmote(boolean betterTTVChannelEmote) {
		isBetterTTVChannelEmote = betterTTVChannelEmote;
	}

	public boolean isSubscriberEmote() {
		return isSubscriberEmote;
	}

	public void setSubscriberEmote(boolean subscriberEmote) {
		isSubscriberEmote = subscriberEmote;
	}

	public String getEmoteId() {
		return emoteId;
	}

	public void setEmoteId(String emoteId) {
		this.emoteId = emoteId;
	}

	public boolean isBetterTTVEmote() {
		return isBetterTTVEmote;
	}

	public void setBetterTTVEmote(boolean betterTTVEmote) {
		isBetterTTVEmote = betterTTVEmote;
	}

	public boolean isTextEmote() {
		return isTextEmote;
	}

	public void setTextEmote(boolean textEmote) {
		isTextEmote = textEmote;
	}

	public String getKeyword() {
		return emoteKeyword;
	}

	@Override
	public int compareTo(@NonNull Emote emote) {
		if (this.isBetterTTVChannelEmote() && !emote.isBetterTTVChannelEmote()) {
			return -1;
		} else if (emote.isBetterTTVChannelEmote() && !this.isBetterTTVChannelEmote()) {
			return 1;
		} else {
			return this.emoteKeyword.compareTo(emote.emoteKeyword);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Emote emote = (Emote) o;

		if (isBetterTTVEmote != emote.isBetterTTVEmote) return false;
		if (isTextEmote != emote.isTextEmote) return false;
		if (emoteId != null ? !emoteId.equals(emote.emoteId) : emote.emoteId != null) return false;
		return emoteKeyword != null ? emoteKeyword.equals(emote.emoteKeyword) : emote.emoteKeyword == null;
	}

	@Override
	public int hashCode() {
		int result = emoteId != null ? emoteId.hashCode() : 0;
		result = 31 * result + (emoteKeyword != null ? emoteKeyword.hashCode() : 0);
		result = 31 * result + (isBetterTTVEmote ? 1 : 0);
		result = 31 * result + (isTextEmote ? 1 : 0);
		return result;
	}
}
