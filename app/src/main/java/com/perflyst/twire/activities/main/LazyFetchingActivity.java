package com.perflyst.twire.activities.main;

import java.util.List;

/**
 * Created by SebastianRask on 18-09-2015.
 */
public interface LazyFetchingActivity<T> {

    void addToAdapter(List<T> aObjectList);

    void startRefreshing();

    void stopRefreshing();

    int getCurrentOffset();

    void setCurrentOffset(int aOffset);

    void startProgress();

    void stopProgress();

    int getLimit();

    void setLimit(int aLimit);

    int getMaxElementsToFetch();

    void setMaxElementsToFetch(int aMax);

    void notifyUserNoElementsAdded();

    List<T> getVisualElements() throws Exception;

}
