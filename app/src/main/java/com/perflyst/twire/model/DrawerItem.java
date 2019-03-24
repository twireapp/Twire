package com.perflyst.twire.model;

import android.graphics.drawable.Drawable;

public class DrawerItem {
    private Drawable icon;
    private String title;
    private Class classForIntent;

    public DrawerItem(Drawable icon, String title, Class classForIntent) {
        this.icon = icon;
        this.title = title;
        this.classForIntent = classForIntent;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Class getClassForIntent() {
        return classForIntent;
    }

    public void setClassForIntent(Class classForIntent) {
        this.classForIntent = classForIntent;
    }
}
