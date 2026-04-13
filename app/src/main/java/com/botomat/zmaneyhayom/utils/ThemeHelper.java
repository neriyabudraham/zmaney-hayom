package com.botomat.zmaneyhayom.utils;

import android.app.Activity;
import android.content.Context;

import androidx.preference.PreferenceManager;

import com.botomat.zmaneyhayom.R;

import java.util.Calendar;

public class ThemeHelper {

    public static void applyTheme(Activity activity) {
        if (shouldUseDarkMode(activity)) {
            activity.setTheme(R.style.AppTheme_Dark);
        } else {
            activity.setTheme(R.style.AppTheme);
        }
    }

    public static boolean isDarkMode(Context context) {
        return shouldUseDarkMode(context);
    }

    private static boolean shouldUseDarkMode(Context context) {
        String mode = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("theme_mode", "auto");

        switch (mode) {
            case "dark":
                return true;
            case "light":
                return false;
            case "auto":
            default:
                // Auto: dark between 19:00-06:00
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                return hour >= 19 || hour < 6;
        }
    }
}
