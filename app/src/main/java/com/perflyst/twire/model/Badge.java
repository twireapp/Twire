package com.perflyst.twire.model;

import android.util.SparseArray;

import java.util.List;

public class Badge {
    public final String name;
    public final SparseArray<String> urls;
    public final String color;
    public final String replaces;
    public final List<String> users;

    public Badge(String name, SparseArray<String> urls, String color, String replaces, List<String> users) {
        this.name = name;
        this.urls = urls;
        this.color = color;
        this.replaces = replaces;
        this.users = users;
    }

    public Badge(String name, SparseArray<String> urls) {
        this.name = name;
        this.urls = urls;
        color = null;
        replaces = null;
        users = null;
    }

    public String getUrl(int size) {
        return urls.get(getBestAvailableSize(size));
    }

    public int getBestAvailableSize(int size) {
        for (int i = size; i >= 1; i--) {
            if (urls.indexOfKey(i) >= 0) {
                return i;
            }
        }

        return 1;
    }
}
