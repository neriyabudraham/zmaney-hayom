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
    private TextView omerText;
    private TextView btnToday;
    private android.widget.ImageButton btnPrevDay, btnNextDay, btnCalendar;
    private Calendar viewedDay = Calendar.getInstance();

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

        // Check for updates in background (only once per session)
        checkForUpdatesIfNeeded();
    }

    private void checkForUpdatesIfNeeded() {
        // Throttle: only check at most once per 6 hours
        long lastCheck = prefs.getLong("last_update_check", 0);
        long now = System.currentTimeMillis();
        if (now - lastCheck < 6 * 60 * 60 * 1000L) return;

        com.botomat.zmaneyhayom.utils.UpdateChecker.check(this,
                new com.botomat.zmaneyhayom.utils.UpdateChecker.UpdateCallback() {
            @Override
            public void onResult(com.botomat.zmaneyhayom.utils.UpdateChecker.UpdateResult result) {
                if (result == null || !result.updateAvailable) {
                    // Clear pending update flag
                    prefs.edit()
                            .putBoolean("update_pending", false)
                            .putInt("pending_update_code", 0)
                            .putLong("last_update_check", System.currentTimeMillis())
                            .apply();
                    return;
                }

                prefs.edit()
                        .putBoolean("update_pending", true)
                        .putInt("pending_update_code", result.latestVersionCode)
                        .putString("pending_update_name", result.latestVersionName)
                        .putLong("last_update_check", System.currentTimeMillis())
                        .apply();

                // If user skipped this exact version - don't popup
                int skipped = prefs.getInt("skipped_update_code", 0);
                if (skipped == result.latestVersionCode) return;

                showUpdateAvailablePopup(result);
            }
        });
    }

    private void showUpdateAvailablePopup(
            final com.botomat.zmaneyhayom.utils.UpdateChecker.UpdateResult result) {
        StringBuilder changes = new StringBuilder();
        if (!result.allVersions.isEmpty()) {
            com.botomat.zmaneyhayom.utils.UpdateChecker.VersionInfo vi = result.allVersions.get(0);
            for (int i = 0; i < Math.min(5, vi.changes.size()); i++) {
                changes.append("• ").append(vi.changes.get(i)).append('\n');
            }
        }
        String msg = "גרסה נוכחית: " + result.currentVersionName +
                "\nגרסה חדשה: " + result.latestVersionName + "\n\n";
        if (changes.length() > 0) {
            msg += "מה חדש:\n" + changes.toString();
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("עדכון חדש זמין!")
                .setMessage(msg)
                .setPositiveButton("עדכן עכשיו",
                        new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        startActivity(new Intent(MainActivity.this, UpdateActivity.class));
                    }
                })
                .setNeutralButton("אחר כך", null)
                .setNegativeButton("דלג על גרסה זו",
                        new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        prefs.edit()
                                .putInt("skipped_update_code", result.latestVersionCode)
                                .apply();
                    }
                })
                .show();
    }

    private void initViews() {
        gregorianDate = findViewById(R.id.gregorian_date);
        hebrewDate = findViewById(R.id.hebrew_date);
        nextAlertContainer = findViewById(R.id.next_alert_container);
        nextAlertText = findViewById(R.id.next_alert_text);
        zmanimList = findViewById(R.id.zmanim_list);
        omerText = findViewById(R.id.omer_text);
        btnToday = findViewById(R.id.btn_today);
        btnPrevDay = findViewById(R.id.btn_prev_day);
        btnNextDay = findViewById(R.id.btn_next_day);
        btnCalendar = findViewById(R.id.btn_calendar);

        btnPrevDay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changeDay(-1); }
        });
        btnNextDay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changeDay(1); }
        });
        btnToday.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                viewedDay = Calendar.getInstance();
                refreshView();
            }
        });
        btnCalendar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showDatePicker(); }
        });
    }

    private void showDatePicker() {
        android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(
                this,
                new android.app.DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
                        viewedDay.set(Calendar.YEAR, year);
                        viewedDay.set(Calendar.MONTH, month);
                        viewedDay.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        refreshView();
                    }
                },
                viewedDay.get(Calendar.YEAR),
                viewedDay.get(Calendar.MONTH),
                viewedDay.get(Calendar.DAY_OF_MONTH));

        // Update title with Hebrew date as user navigates
        try {
            final android.app.DatePickerDialog dlg = dialog;
            dialog.setTitle(HebrewDateHelper.getHebrewDate(viewedDay));
            dialog.getDatePicker().init(
                    viewedDay.get(Calendar.YEAR),
                    viewedDay.get(Calendar.MONTH),
                    viewedDay.get(Calendar.DAY_OF_MONTH),
                    new android.widget.DatePicker.OnDateChangedListener() {
                        @Override
                        public void onDateChanged(android.widget.DatePicker view,
                                int year, int month, int dayOfMonth) {
                            try {
                                Calendar temp = Calendar.getInstance();
                                temp.set(year, month, dayOfMonth);
                                dlg.setTitle(HebrewDateHelper.getHebrewDate(temp));
                            } catch (Exception ignored) {}
                        }
                    });
        } catch (Exception ignored) {}

        dialog.show();
    }

    private void changeDay(int delta) {
        viewedDay.add(Calendar.DAY_OF_MONTH, delta);
        refreshView();
    }

    private boolean isToday() {
        Calendar now = Calendar.getInstance();
        return viewedDay.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && viewedDay.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
    }

    private void refreshView() {
        updateDates();
        loadZmanim();
        btnToday.setVisibility(isToday() ? View.GONE : View.VISIBLE);
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

        // Show blue dot if update is pending
        boolean updatePending = prefs.getBoolean("update_pending", false);
        int pendingCode = prefs.getInt("pending_update_code", 0);
        int skippedCode = prefs.getInt("skipped_update_code", 0);
        // Show dot even if user "skipped" - they should still see something pending
        View updateDot = dialogView.findViewById(R.id.menu_update_dot);
        if (updateDot != null) {
            updateDot.setVisibility(updatePending && pendingCode > 0 ? View.VISIBLE : View.GONE);
        }

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
        dialogView.findViewById(R.id.menu_update).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, UpdateActivity.class));
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
        gregorianDate.setText(sdf.format(viewedDay.getTime()));

        try {
            hebrewDate.setText(HebrewDateHelper.getHebrewDate(viewedDay));
        } catch (Exception e) {
            hebrewDate.setVisibility(View.GONE);
        }

        // Sefirat HaOmer
        try {
            String omer = HebrewDateHelper.getSefiratHaOmer(viewedDay);
            if (omer != null && !omer.isEmpty()) {
                omerText.setText(omer);
                omerText.setVisibility(View.VISIBLE);
            } else {
                omerText.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            omerText.setVisibility(View.GONE);
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
        List<ZmanItem> zmanim = calculator.calculateZmanim(viewedDay);
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
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // Long-press OK / Center / Enter opens main menu (alternative if # / * not available)
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            showCustomMenu();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Mark long-pressable keys for menu access
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            event.startTracking();
        }
        // Scroll zmanim list with D-pad or volume keys
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_PAGE_UP) {
            androidx.recyclerview.widget.LinearLayoutManager lm =
                    (androidx.recyclerview.widget.LinearLayoutManager) zmanimList.getLayoutManager();
            if (lm != null) {
                int first = lm.findFirstVisibleItemPosition();
                if (first > 0) {
                    zmanimList.smoothScrollToPosition(Math.max(0, first - 2));
                }
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
            androidx.recyclerview.widget.LinearLayoutManager lm =
                    (androidx.recyclerview.widget.LinearLayoutManager) zmanimList.getLayoutManager();
            if (lm != null) {
                int last = lm.findLastVisibleItemPosition();
                int total = adapter.getItemCount();
                if (last < total - 1) {
                    zmanimList.smoothScrollToPosition(Math.min(total - 1, last + 2));
                }
            }
            return true;
        }
        // # / * / Menu / Soft-left key opens main menu
        if (keyCode == KeyEvent.KEYCODE_POUND
                || keyCode == KeyEvent.KEYCODE_STAR
                || keyCode == KeyEvent.KEYCODE_MENU
                || keyCode == KeyEvent.KEYCODE_SOFT_LEFT) {
            showCustomMenu();
            return true;
        }
        // Left arrow = previous day
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            changeDay(-1);
            return true;
        }
        // Right arrow = next day
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            changeDay(1);
            return true;
        }
        // Center/Enter opens calendar picker
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            showDatePicker();
            return true;
        }
        // Number keys 1/2/3 for quick day nav
        if (keyCode == KeyEvent.KEYCODE_1) {
            changeDay(-1);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_2) {
            showDatePicker();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_3) {
            changeDay(1);
            return true;
        }
        // Number 0 - return to today
        if (keyCode == KeyEvent.KEYCODE_0) {
            viewedDay = Calendar.getInstance();
            refreshView();
            return true;
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
