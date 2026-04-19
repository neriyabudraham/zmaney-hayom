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
        JewishCalendar jc = new JewishCalendar(calendar.getTime());
        return formatter.format(jc);
    }

    public static String getParsha() {
        JewishCalendar jc = new JewishCalendar();
        HebrewDateFormatter hdf = new HebrewDateFormatter();
        hdf.setHebrewFormat(true);
        return hdf.formatParsha(jc);
    }

    public static String getSpecialDay() {
        return getSpecialDay(Calendar.getInstance());
    }

    public static String getSpecialDay(Calendar calendar) {
        JewishCalendar jc = new JewishCalendar(calendar.getTime());
        HebrewDateFormatter hdf = new HebrewDateFormatter();
        hdf.setHebrewFormat(true);
        String yomTov = hdf.formatYomTov(jc);
        return yomTov != null ? yomTov : "";
    }

    /**
     * Returns Sefirat HaOmer display for today (empty if not in Omer period).
     * Returns something like: "ספירת העומר: היום כ\"ו יום שהם ג' שבועות וה' ימים לעומר"
     */
    public static String getSefiratHaOmer() {
        return getSefiratHaOmer(Calendar.getInstance());
    }

    public static String getSefiratHaOmer(Calendar calendar) {
        try {
            JewishCalendar jc = new JewishCalendar(calendar.getTime());
            int omer = jc.getDayOfOmer();
            if (omer < 1 || omer > 49) return "";

            String[] hebDays = {
                "", "א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ז׳", "ח׳", "ט׳",
                "י׳", "י״א", "י״ב", "י״ג", "י״ד", "ט״ו", "ט״ז", "י״ז", "י״ח", "י״ט",
                "כ׳", "כ״א", "כ״ב", "כ״ג", "כ״ד", "כ״ה", "כ״ו", "כ״ז", "כ״ח", "כ״ט",
                "ל׳", "ל״א", "ל״ב", "ל״ג", "ל״ד", "ל״ה", "ל״ו", "ל״ז", "ל״ח", "ל״ט",
                "מ׳", "מ״א", "מ״ב", "מ״ג", "מ״ד", "מ״ה", "מ״ו", "מ״ז", "מ״ח", "מ״ט"
            };

            StringBuilder sb = new StringBuilder();
            sb.append("היום ").append(hebDays[omer]).append(" ");
            sb.append(omer == 1 ? "יום" : "ימים");

            if (omer >= 7) {
                int weeks = omer / 7;
                int days = omer % 7;
                sb.append(" שהם ").append(hebDays[weeks]).append(" ");
                sb.append(weeks == 1 ? "שבוע" : "שבועות");
                if (days > 0) {
                    sb.append(" ו").append(hebDays[days]).append(" ");
                    sb.append(days == 1 ? "יום" : "ימים");
                }
            }
            sb.append(" לעומר");
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static int getOmerDay(Calendar calendar) {
        try {
            JewishCalendar jc = new JewishCalendar(calendar.getTime());
            return jc.getDayOfOmer();
        } catch (Exception e) {
            return -1;
        }
    }
}
