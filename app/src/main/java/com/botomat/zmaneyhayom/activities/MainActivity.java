package com.botomat.zmaneyhayom.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.adapters.ZmanimAdapter;
import com.botomat.zmaneyhayom.database.DatabaseHelper;
import com.botomat.zmaneyhayom.models.ZmanItem;
import com.botomat.zmaneyhayom.utils.AlarmScheduler;
import com.botomat.zmaneyhayom.utils.HebrewDateHelper;
import com.botomat.zmaneyhayom.utils.ThemeHelper;
import com.botomat.zmaneyhayom.utils.ZmanimCalculator;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ZmanimAdapter adapter;
    private ZmanimCalculator calculator;
    private DatabaseHelper db;
    private Handler handler;
    private Runnable updateRunnable;
    private SharedPreferences prefs;

    private TextView gregorianDate;
    private TextView hebrewDate;
    private LinearLayout nextAlertContainer;
    private TextView nextAlertText;
    private RecyclerView zmanimList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        calculator = new ZmanimCalculator(this);
        db = DatabaseHelper.getInstance(this);
        handler = new Handler();

        initViews();
        setupToolbar();
        setupZmanimList();
        updateDates();
        loadZmanim();

        AlarmScheduler.scheduleAllAlarms(this);

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                loadZmanim();
                updateNextAlert();
                handler.postDelayed(this, 30000);
            }
        };
    }

    private void initViews() {
        gregorianDate = findViewById(R.id.gregorian_date);
        hebrewDate = findViewById(R.id.hebrew_date);
        nextAlertContainer = findViewById(R.id.next_alert_container);
        nextAlertText = findViewById(R.id.next_alert_text);
        zmanimList = findViewById(R.id.zmanim_list);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> showCustomMenu());
    }

    private void showCustomMenu() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_menu, null);

        // Apply theme-appropriate background
        boolean dark = ThemeHelper.isDarkMode(this);
        dialogView.setBackgroundResource(dark ? R.drawable.card_bg_dark : R.drawable.card_bg_light);
        applyDialogTextColors(dialogView, dark);

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);

        // Position dialog at top with animation
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(android.view.Gravity.TOP);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.getAttributes().windowAnimations = android.R.style.Animation_Dialog;
            window.getAttributes().y = 52; // below toolbar
        }

        dialogView.findViewById(R.id.menu_alerts).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ManageAlertsActivity.class));
        });
        dialogView.findViewById(R.id.menu_history).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, AlertHistoryActivity.class));
        });
        dialogView.findViewById(R.id.menu_customize).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, CustomizeActivity.class));
        });
        dialogView.findViewById(R.id.menu_settings).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, SettingsActivity.class));
        });
        dialogView.findViewById(R.id.menu_share).setOnClickListener(v -> {
            dialog.dismiss();
            shareApp();
        });
        dialogView.findViewById(R.id.menu_about).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, AboutActivity.class));
        });

        // Theme toggle with subtitle showing current mode
        TextView themeSubtitle = dialogView.findViewById(R.id.menu_theme_subtitle);
        String currentMode = prefs.getString("theme_mode", "light");
        themeSubtitle.setText(getThemeLabel(currentMode));

        dialogView.findViewById(R.id.menu_theme).setOnClickListener(v -> {
            dialog.dismiss();
            showThemeDialog();
        });

        dialog.show();
    }

    private String getThemeLabel(String mode) {
        if ("dark".equals(mode)) return "כהה";
        if ("auto".equals(mode)) return "אוטומטי (יום/לילה)";
        return "בהיר";
    }

    private void showThemeDialog() {
        final String[] labels = {"בהיר", "כהה", "אוטומטי (יום/לילה)"};
        final String[] values = {"light", "dark", "auto"};
        String current = prefs.getString("theme_mode", "light");
        int currentIdx = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) { currentIdx = i; break; }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.theme)
                .setSingleChoiceItems(labels, currentIdx,
                        new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        prefs.edit().putString("theme_mode", values[which]).apply();
                        d.dismiss();
                        recreate();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupZmanimList() {
        zmanimList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ZmanimAdapter();
        // Always use cards view
        adapter.setViewMode(ZmanimAdapter.VIEW_CARDS);

        int fontScale = prefs.getInt("font_scale", 2);
        adapter.setFontScale(fontScale);

        zmanimList.setAdapter(adapter);
    }

    private void updateDates() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("he"));
        gregorianDate.setText(sdf.format(Calendar.getInstance().getTime()));

        try {
            hebrewDate.setText(HebrewDateHelper.getHebrewDate());
        } catch (Exception e) {
            hebrewDate.setVisibility(View.GONE);
        }
    }

    private void applyDialogTextColors(View root, boolean dark) {
        int textColor = dark ? 0xFFE5E7EB : 0xFF1F2937;
        int secondaryColor = dark ? 0xFF9CA3AF : 0xFF6B7280;
        applyTextColorRecursive(root, textColor, secondaryColor);
    }

    private void applyTextColorRecursive(View view, int primary, int secondary) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            float size = tv.getTextSize();
            // Small text = secondary, larger = primary. sp conversion: 12sp ~ 12*density
            float density = getResources().getDisplayMetrics().scaledDensity;
            if (size < 13 * density) {
                tv.setTextColor(secondary);
            } else {
                tv.setTextColor(primary);
            }
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyTextColorRecursive(vg.getChildAt(i), primary, secondary);
            }
        }
    }

    private void loadZmanim() {
        List<ZmanItem> zmanim = calculator.calculateZmanim();
        for (ZmanItem item : zmanim) {
            item.setHasAlert(db.hasAlertForZman(item.getType()));
        }
        adapter.setZmanim(zmanim);
        updateNextAlert();
    }

    private void updateNextAlert() {
        Date nextAlert = AlarmScheduler.getNextAlertTime(this);
        if (nextAlert != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            nextAlertText.setText("התראה הבאה: " + sdf.format(nextAlert));
            nextAlertContainer.setVisibility(View.VISIBLE);
        } else {
            nextAlertContainer.setVisibility(View.GONE);
        }
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app)));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            zmanimList.requestFocus();
            return zmanimList.dispatchKeyEvent(event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int fontScale = prefs.getInt("font_scale", 2);
        adapter.setFontScale(fontScale);
        loadZmanim();
        handler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }
}
