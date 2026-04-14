package com.botomat.zmaneyhayom.utils;

import android.app.Activity;
import android.content.Context;

import androidx.preference.PreferenceManager;

import com.botomat.zmaneyhayom.R;

import java.util.Calendar;

public class ThemeHelper {

    public static void applyTheme(Activity activity) {
        if (isDarkMode(activity)) {
            activity.setTheme(R.style.AppTheme_Dark);
        } else {
            activity.setTheme(R.style.AppTheme);
        }
    }

    public static boolean isDarkMode(Context context) {
        String mode = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("theme_mode", "light");
        if ("dark".equals(mode)) return true;
        if ("light".equals(mode)) return false;
        // Auto mode: dark between 19:00-06:00
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour >= 19 || hour < 6;
    }
}
