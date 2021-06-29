package com.perflyst.twire.misc;

import java.text.DecimalFormat;
import java.util.Calendar;

public class OnlineSince {
    //apparently timezones are the biggest bullshit in android
    //but this worked: https://stackoverflow.com/a/16595970 + the comment below
    public static long GetLocalTime()
    {
        Calendar c = Calendar.getInstance();
        int utcOffset = c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET);
        c.add(Calendar.MILLISECOND, (-utcOffset));
        Long utcMilliseconds = c.getTimeInMillis();
        return utcMilliseconds;
    }

    public static String getOnlineSince(long startedAt) {
        long timestamp = GetLocalTime();
        long millis = timestamp - startedAt;
        //do some time magic here
        int hours = (int) (millis / (1000 * 60 * 60));
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        int seconds = (int) ((millis / (1000)) % 60);
        String OnlineSince = new DecimalFormat("00").format(hours) + ":" + new DecimalFormat("00").format(minutes) + ":" + new DecimalFormat("00").format(seconds);

        return OnlineSince;
    }
}
