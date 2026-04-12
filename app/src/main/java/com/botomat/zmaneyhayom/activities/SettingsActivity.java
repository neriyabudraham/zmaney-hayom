package com.botomat.zmaneyhayom.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.utils.AlarmScheduler;
import com.botomat.zmaneyhayom.utils.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Reschedule alarms when settings change (location, DST, etc.)
        AlarmScheduler.scheduleAllAlarms(this);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            // Show/hide custom location fields based on city selection
            findPreference("city").setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isCustom = "custom".equals(newValue);
                findPreference("custom_latitude").setVisible(isCustom);
                findPreference("custom_longitude").setVisible(isCustom);
                findPreference("custom_elevation").setVisible(isCustom);
                return true;
            });

            // Initialize visibility
            String currentCity = getPreferenceManager().getSharedPreferences()
                    .getString("city", "jerusalem");
            boolean isCustom = "custom".equals(currentCity);
            findPreference("custom_latitude").setVisible(isCustom);
            findPreference("custom_longitude").setVisible(isCustom);
            findPreference("custom_elevation").setVisible(isCustom);

            // Theme change requires activity restart
            findPreference("theme_mode").setOnPreferenceChangeListener((preference, newValue) -> {
                requireActivity().recreate();
                return true;
            });
        }
    }
}
