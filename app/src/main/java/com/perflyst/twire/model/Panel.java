package com.perflyst.twire.model;

/**
 * Created by Sebastian Rask on 24-02-2017.
 */

public class Panel {
    private String mStreamerName;
    private int mUserId;
    private int mOrder;
    private String mDescription;
    private String mImageUrl;
    private String mLinkUrl;
    private String mTitle;
    private String mHtml;

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

    public void setmHtml(String mHtml) {
        this.mHtml = mHtml;
    }

    public String getmStreamerName() {
        return mStreamerName;
    }

    public void setmStreamerName(String mStreamerName) {
        this.mStreamerName = mStreamerName;
    }

    public int getmUserId() {
        return mUserId;
    }

    public void setmUserId(int mUserId) {
        this.mUserId = mUserId;
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

    public void setmDescription(String mDescription) {
        this.mDescription = mDescription;
    }

    public String getmImageUrl() {
        return mImageUrl;
    }

    public void setmImageUrl(String mImageUrl) {
        this.mImageUrl = mImageUrl;
    }

    public String getmLinkUrl() {
        return mLinkUrl;
    }

    public void setmLinkUrl(String mLinkUrl) {
        this.mLinkUrl = mLinkUrl;
    }

    public String getmTitle() {
        return mTitle;
    }

    public void setmTitle(String mTitle) {
        this.mTitle = mTitle;
    }
}
