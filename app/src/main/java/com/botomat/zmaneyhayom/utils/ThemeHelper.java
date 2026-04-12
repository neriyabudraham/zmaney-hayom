package com.botomat.zmaneyhayom.utils;

import android.app.Activity;
import android.content.Context;

import androidx.preference.PreferenceManager;

import com.botomat.zmaneyhayom.R;

public class ThemeHelper {

    public static void applyTheme(Activity activity) {
        if (isDarkMode(activity)) {
            activity.setTheme(R.style.AppTheme_Dark);
        } else {
            activity.setTheme(R.style.AppTheme);
        }
    }

    public static boolean isDarkMode(Context context) {
        return "dark".equals(PreferenceManager.getDefaultSharedPreferences(context)
                .getString("theme_mode", "light"));
    }
}
