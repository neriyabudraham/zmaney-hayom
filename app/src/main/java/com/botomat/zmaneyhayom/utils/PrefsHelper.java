package com.botomat.zmaneyhayom.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class PrefsHelper {

    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_CITY = "city";
    private static final String KEY_DST_MODE = "dst_mode";
    private static final String KEY_SNOOZE = "snooze_duration";
    private static final String KEY_VIBRATE = "vibrate_enabled";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    private final SharedPreferences prefs;

    public PrefsHelper(Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isDarkMode() {
        return "dark".equals(prefs.getString(KEY_THEME, "light"));
    }

    public void setDarkMode(boolean dark) {
        prefs.edit().putString(KEY_THEME, dark ? "dark" : "light").apply();
    }

    public String getCity() {
        return prefs.getString(KEY_CITY, "jerusalem");
    }

    public String getDstMode() {
        return prefs.getString(KEY_DST_MODE, "auto");
    }

    public int getSnoozeDuration() {
        return Integer.parseInt(prefs.getString(KEY_SNOOZE, "5"));
    }

    public boolean isVibrateEnabled() {
        return prefs.getBoolean(KEY_VIBRATE, true);
    }

    public boolean isFirstLaunch() {
        boolean first = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        if (first) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        }
        return first;
    }
}
