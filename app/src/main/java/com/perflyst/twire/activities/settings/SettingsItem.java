package com.perflyst.twire.activities.settings;

public class SettingsItem {
    private final String title;
    private final Class classForIntent;

    public SettingsItem(String title, Class classForIntent) {
        this.title = title;
        this.classForIntent = classForIntent;
    }

    public String getTitle() {
        return title;
    }

    public Class getClassForIntent() {
        return classForIntent;
    }
}
