package com.thy.cloud.base.util.date;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Date utility for parsing dates from strings.
 *
 * @author Engin Mahmut
 */
public final class DateUtil {

    private static final String[] PARSE_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd",
            "dd.MM.yyyy HH:mm:ss",
            "dd.MM.yyyy"
    };

    private DateUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Parse a date string trying multiple patterns.
     */
    public static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        for (String pattern : PARSE_PATTERNS) {
            try {
                return new SimpleDateFormat(pattern).parse(dateStr);
            } catch (ParseException ignored) {
                // try next pattern
            }
        }
        return null;
    }
}
