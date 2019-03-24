package com.perflyst.twire.activities.settings;

public class SettingsItem {
    private String title;
    private Class classForIntent;

    public SettingsItem(String title, Class classForIntent) {
        this.title = title;
        this.classForIntent = classForIntent;
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
