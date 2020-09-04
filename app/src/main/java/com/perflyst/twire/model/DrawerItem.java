package com.perflyst.twire.model;

import android.graphics.drawable.Drawable;

public class DrawerItem {
    private final Drawable icon;
    private final String title;
    private final Class classForIntent;

    public DrawerItem(Drawable icon, String title, Class classForIntent) {
        this.icon = icon;
        this.title = title;
        this.classForIntent = classForIntent;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public Class getClassForIntent() {
        return classForIntent;
    }
}
