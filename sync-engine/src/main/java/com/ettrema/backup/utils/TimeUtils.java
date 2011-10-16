package com.ettrema.backup.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 *
 * @author brad
 */
public class TimeUtils {

    public static String formatBandwidth(double bytesPerSec) {
        long lBytesPerSec = Math.round(bytesPerSec);
        return formatBytes(lBytesPerSec) + "/sec";
    }

    public static String formatSecsAsTime(Long totalSecs) {
        if (totalSecs != null) {
            int mins = (int) (totalSecs / 60);
            int secs = (int) (totalSecs % 60);
            int hours = mins / 60;
            mins = mins % 60;
            if (hours > 0) {
                return pad2(hours) + ":" + pad2(mins) + ":" + pad2(secs);
            } else if (mins > 0) {
                return pad2(mins) + ":" + pad2(secs) + " secs";
            } else {
                return secs + " secs";
            }
        } else {
            return "Unknown";
        }
    }

    public static String pad2(int secs) {
        if (secs < 10) {
            return "0" + secs;
        } else {
            return secs + "";
        }
    }

    public static String formatBytes(Long n) {
        if (n == null) {
            return "Unknown";
        } else if (n < 1000) {
            return n + " bytes";
        } else if (n < 1000000) {
            return n / 1000 + "KB";
        } else if (n < 1000000000) {
            return n / 1000000 + "MB";
        } else {
            return n / 1000000000 + "GB";
        }
    }

    public static String formatTime(Date completed) {
        if (completed == null) {
            return "";
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(completed);
        return TimeUtils.pad2(cal.get(Calendar.HOUR_OF_DAY)) + ":" + TimeUtils.pad2(cal.get(Calendar.MINUTE)) + ":" + TimeUtils.pad2(cal.get(Calendar.SECOND));
    }

    public static String formatDateTime(Date completed) {
        if (completed == null) {
            return "";
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(completed);
        String s = cal.get(Calendar.DAY_OF_MONTH) + " " + monthName(cal) + formatTime(completed);
        return s;
    }

    private static String monthName(Calendar cal) {
        return cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
    }
    
}
