package com.sebastianrask.bettersubscription.fragments;

import android.content.Context;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.sebastianrask.bettersubscription.R;

/**
 * Created by Sebastian Rask Jepsen on 13/05/2018.
 */

public class StreamSessionManagerListener implements SessionManagerListener<CastSession> {
    private Callback mCallback;
    private Context mContext;

    public StreamSessionManagerListener(Callback callback, Context context) {
        this.mCallback = callback;
        this.mContext = context;
    }

    @Override
    public void onSessionStarting(CastSession session) { }

    @Override
    public void onSessionStarted(CastSession session, String s) {
        mCallback.onConnected();
    }

    @Override
    public void onSessionStartFailed(CastSession session, int i) {
        String errorMessage = mContext.getString(R.string.stream_chromecast_connection_failed, getDeviceName(session));
        mCallback.onError(errorMessage);
    }

    @Override
    public void onSessionEnding(CastSession session) { }

    @Override
    public void onSessionEnded(CastSession session, int i) {
        mCallback.onDisconnected();
    }

    @Override
    public void onSessionResuming(CastSession session, String s) {}

    @Override
    public void onSessionResumed(CastSession session, boolean b) {}

    @Override
    public void onSessionResumeFailed(CastSession session, int i) {
        String errorMessage = mContext.getString(R.string.stream_chromecast_failed, getDeviceName(session));
        mCallback.onError(errorMessage);
    }

    @Override
    public void onSessionSuspended(CastSession session, int i) {
        mCallback.onDisconnected();
    }

    private String getDeviceName(CastSession session) {
        return session.getCastDevice().getFriendlyName();
    }

    public interface Callback {
        void onError(String errorMessage);
        void onConnected();
        void onDisconnected();
    }
}
