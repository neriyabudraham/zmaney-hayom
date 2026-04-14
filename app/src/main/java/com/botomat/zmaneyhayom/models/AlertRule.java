package com.botomat.zmaneyhayom.models;

import java.util.Calendar;

public class AlertRule {
    // Days bitmask: bit 0=Sunday, bit 1=Monday, ..., bit 6=Saturday (matching Calendar.SUNDAY=1 -> bit 0)
    public static final int DAY_SUNDAY = 1;       // 0b0000001
    public static final int DAY_MONDAY = 2;       // 0b0000010
    public static final int DAY_TUESDAY = 4;      // 0b0000100
    public static final int DAY_WEDNESDAY = 8;    // 0b0001000
    public static final int DAY_THURSDAY = 16;    // 0b0010000
    public static final int DAY_FRIDAY = 32;      // 0b0100000
    public static final int DAY_SATURDAY = 64;    // 0b1000000
    public static final int DAYS_ALL = 127;       // all 7 days
    public static final int DAYS_WEEKDAYS = 62;   // Sun-Thu (no Fri/Sat)
    public static final int DAYS_SUN_TO_FRI = 63; // Sun-Fri (no Sat)

    private long id;
    private ZmanType zmanType;
    private OffsetType offsetType;
    private int offsetMinutes;
    private boolean enabled;
    private boolean soundEnabled;
    private boolean vibrateEnabled;
    private int daysMask;

    public AlertRule() {
        this.enabled = true;
        this.soundEnabled = true;
        this.vibrateEnabled = true;
        this.offsetType = OffsetType.AT_TIME;
        this.offsetMinutes = 0;
        this.daysMask = DAYS_ALL;
    }

    public AlertRule(long id, ZmanType zmanType, OffsetType offsetType, int offsetMinutes,
                     boolean enabled, boolean soundEnabled, boolean vibrateEnabled, int daysMask) {
        this.id = id;
        this.zmanType = zmanType;
        this.offsetType = offsetType;
        this.offsetMinutes = offsetMinutes;
        this.enabled = enabled;
        this.soundEnabled = soundEnabled;
        this.vibrateEnabled = vibrateEnabled;
        this.daysMask = daysMask == 0 ? DAYS_ALL : daysMask;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public ZmanType getZmanType() { return zmanType; }
    public void setZmanType(ZmanType zmanType) { this.zmanType = zmanType; }
    public OffsetType getOffsetType() { return offsetType; }
    public void setOffsetType(OffsetType offsetType) { this.offsetType = offsetType; }
    public int getOffsetMinutes() { return offsetMinutes; }
    public void setOffsetMinutes(int offsetMinutes) { this.offsetMinutes = offsetMinutes; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }
    public boolean isVibrateEnabled() { return vibrateEnabled; }
    public void setVibrateEnabled(boolean vibrateEnabled) { this.vibrateEnabled = vibrateEnabled; }
    public int getDaysMask() { return daysMask; }
    public void setDaysMask(int daysMask) { this.daysMask = daysMask; }

    /** Check if alert is enabled for a specific day (Calendar.SUNDAY..Calendar.SATURDAY) */
    public boolean isEnabledForDay(int calendarDayOfWeek) {
        int bit = 1 << (calendarDayOfWeek - Calendar.SUNDAY);
        return (daysMask & bit) != 0;
    }

    public String getDaysDisplayText() {
        if (daysMask == DAYS_ALL) return "כל יום";
        if (daysMask == DAYS_WEEKDAYS) return "ימי חול (א׳-ה׳)";
        if (daysMask == DAYS_SUN_TO_FRI) return "א׳-ו׳";

        String[] shortNames = {"א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((daysMask & (1 << i)) != 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(shortNames[i]);
            }
        }
        return sb.length() > 0 ? sb.toString() : "אף יום";
    }

    public String getDisplayText() {
        String zmanName = zmanType.getHebrewName();
        switch (offsetType) {
            case MINUTES_BEFORE:
                return offsetMinutes + " דקות לפני " + zmanName;
            case MINUTES_AFTER:
                return offsetMinutes + " דקות אחרי " + zmanName;
            case AT_TIME:
            default:
                return "בזמן " + zmanName;
        }
    }
}
