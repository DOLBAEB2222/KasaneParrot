package dev.dolbaeb.kasaneparrot.util;

import java.util.Locale;

/**
 * Человекочитаемое форматирование времени: «45 с», «4 мин 12 с».
 */
public final class TimeFormat {

    private TimeFormat() {
    }

    public static String format(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        long min = seconds / 60;
        long sec = seconds % 60;
        if (min <= 0) {
            return sec + " с";
        }
        if (sec == 0) {
            return min + " мин";
        }
        return String.format(Locale.ROOT, "%d мин %02d с", min, sec);
    }
}
