package com.sebastianrask.bettersubscription.model;

import android.graphics.Bitmap;

/**
 * Created by SebastianRask on 03-03-2016.
 */
public class ChatEmote {
	private String[] emotePositions;
	private Bitmap emoteBitmap;

	private boolean isGif = false;

	public ChatEmote(String[] emotePositions, Bitmap emoteBitmap) {
		this.emotePositions = emotePositions;
		this.emoteBitmap = emoteBitmap;
	}

	public Bitmap getEmoteBitmap() {
		return emoteBitmap;
	}

	public String[] getEmotePositions() {
		return emotePositions;
	}

	public boolean isGif() {
		return isGif;
	}

	public void setGif(boolean gif) {
		isGif = gif;
	}


}
