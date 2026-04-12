package com.botomat.zmaneyhayom.utils;

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.util.Calendar;

public class HebrewDateHelper {

    private static final HebrewDateFormatter formatter = new HebrewDateFormatter();

    static {
        formatter.setHebrewFormat(true);
    }

    public static String getHebrewDate() {
        JewishCalendar jc = new JewishCalendar();
        return formatter.format(jc);
    }

    public static String getHebrewDate(Calendar calendar) {
        JewishCalendar jc = new JewishCalendar(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        return formatter.format(jc);
    }

    public static String getParsha() {
        JewishCalendar jc = new JewishCalendar();
        HebrewDateFormatter hdf = new HebrewDateFormatter();
        hdf.setHebrewFormat(true);
        return hdf.formatParsha(jc);
    }

    public static String getSpecialDay() {
        JewishCalendar jc = new JewishCalendar();
        HebrewDateFormatter hdf = new HebrewDateFormatter();
        hdf.setHebrewFormat(true);
        String yomTov = hdf.formatYomTov(jc);
        return yomTov != null ? yomTov : "";
    }
}
