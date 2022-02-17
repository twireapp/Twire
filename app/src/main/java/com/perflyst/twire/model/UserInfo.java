package com.perflyst.twire.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class UserInfo implements Parcelable {
    public static final Parcelable.Creator<UserInfo> CREATOR = new ClassLoaderCreator<>() {
        @Override
        public UserInfo createFromParcel(Parcel source) {
            String[] data = new String[3];

            source.readStringArray(data);
            return new UserInfo(Integer.parseInt(data[0]), data[1], data[2]);
        }

        @Override
        public UserInfo createFromParcel(Parcel source, ClassLoader loader) {
            return createFromParcel(source);
        }

        @Override
        public UserInfo[] newArray(int size) {
            return new UserInfo[size];
        }
    };

    private final int id;
    private final String login;
    private final String name;

    public UserInfo(int id, String login, String name) {
        this.id = id;
        this.login = login;
        this.name = name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String[] toSend = {
                String.valueOf(this.id),
                this.login,
                this.name
        };

        dest.writeStringArray(toSend);
    }

    @NonNull
    public String toString() {
        return this.name;
    }

    public boolean equals(Object o) {
        if (getClass() != o.getClass())
            return false;

        UserInfo other = (UserInfo) o;
        return this.id == other.getUserId();

    }

    public int getUserId() {
        return id;
    }

    public String getDisplayName() {
        return this.name;
    }

    public String getLogin() {
        return this.login;
    }
}
