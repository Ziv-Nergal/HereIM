package utils;

import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeStampParser {

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    public static String NonAccurateParse(long iTime){

        String time = null;

        if (iTime < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            iTime *= 1000;
        }

        long now = System.currentTimeMillis();

        if (iTime > now || iTime <= 0) {
            return time;
        }

        // TODO: localize
        final long diff = now - iTime;

        if (diff < MINUTE_MILLIS) {
            time = "just now";
        }
        else if (diff < 2 * MINUTE_MILLIS) {
            time = "a minute ago";
        }
        else if (diff < 50 * MINUTE_MILLIS) {
            time = diff / MINUTE_MILLIS + " minutes ago";
        }
        else if (diff < 90 * MINUTE_MILLIS) {
            time = "an hour ago";
        }
        else if (diff < 24 * HOUR_MILLIS) {
            time = diff / HOUR_MILLIS + " hours ago";
        }
        else if (diff < 48 * HOUR_MILLIS) {
            time = "yesterday";
        }
        else {
            time = diff / DAY_MILLIS + " days ago";
        }

        return time;
    }

    public static String AccurateParse(long iTime){

        if (iTime < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            iTime *= 1000;
        }

        Calendar todayCalendar = Calendar.getInstance();
        Calendar timeStampCalendar = Calendar.getInstance();

        todayCalendar.setTimeInMillis(iTime);

        SimpleDateFormat simpleDateFormat;

        if(todayCalendar.get(Calendar.YEAR) != timeStampCalendar.get(Calendar.YEAR)){
            simpleDateFormat = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());
        } else if (todayCalendar.get(Calendar.DAY_OF_YEAR) != timeStampCalendar.get(Calendar.DAY_OF_YEAR)){
            simpleDateFormat = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());
        } else {
            simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        }

        return simpleDateFormat.format(todayCalendar.getTime());
    }
}
