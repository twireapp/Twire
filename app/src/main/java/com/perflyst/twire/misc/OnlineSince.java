package com.perflyst.twire.misc;

import java.text.DecimalFormat;

public class OnlineSince {
    public static String getOnlineSince(long startedAt) {
        Long timestamp = System.currentTimeMillis();
        //do some time magic here
        long millis = timestamp - startedAt;
        int hours = (int) (millis / (1000 * 60 * 60));
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        int seconds = (int) ((millis / (1000)) % 60);
        String OnlineSince = new DecimalFormat("00").format(hours) + ":" + new DecimalFormat("00").format(minutes) + ":" + new DecimalFormat("00").format(seconds);

        return OnlineSince;
    }
}
