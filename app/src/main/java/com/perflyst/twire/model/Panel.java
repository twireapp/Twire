package com.perflyst.twire.model;

/**
 * Created by Sebastian Rask on 24-02-2017.
 */

public class Panel {
    private final String mStreamerName;
    private final int mUserId;
    private final String mDescription;
    private final String mImageUrl;
    private final String mLinkUrl;
    private final String mTitle;
    private final String mHtml;
    private int mOrder;

    public Panel(String mStreamerName, int mUserId, int mOrder, String mDescription,
                 String mImageUrl, String mLinkUrl, String mTitle, String mHtml) {
        this.mStreamerName = mStreamerName;
        this.mUserId = mUserId;
        this.mOrder = mOrder;
        this.mDescription = mDescription;
        this.mImageUrl = mImageUrl;
        this.mLinkUrl = mLinkUrl;
        this.mTitle = mTitle;
        this.mHtml = mHtml;
    }

    public String getmHtml() {
        return mHtml;
    }

    public String getmStreamerName() {
        return mStreamerName;
    }

    public int getmUserId() {
        return mUserId;
    }

    public int getmOrder() {
        return mOrder;
    }

    public void setmOrder(int mOrder) {
        this.mOrder = mOrder;
    }

    public String getmDescription() {
        return mDescription;
    }

    public String getmImageUrl() {
        return mImageUrl;
    }

    public String getmLinkUrl() {
        return mLinkUrl;
    }

    public String getmTitle() {
        return mTitle;
    }
}
