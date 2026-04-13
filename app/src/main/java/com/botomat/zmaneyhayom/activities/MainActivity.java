package com.botomat.zmaneyhayom.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
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
import com.botomat.zmaneyhayom.models.ZmanType;
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

    // Countdown
    private LinearLayout countdownContainer;
    private TextView countdownLabel;
    private TextView countdownTimer;
    private Date countdownTarget;
    private String countdownZmanName;
    private Runnable countdownRunnable;

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
        countdownContainer = findViewById(R.id.countdown_container);
        countdownLabel = findViewById(R.id.countdown_label);
        countdownTimer = findViewById(R.id.countdown_timer);

        ImageButton countdownClose = findViewById(R.id.countdown_close);
        countdownClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideCountdown();
            }
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomMenu();
            }
        });
    }

    private void showCustomMenu() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_menu, null);

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);

        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(android.view.Gravity.TOP);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.getAttributes().windowAnimations = android.R.style.Animation_Dialog;
            window.getAttributes().y = 52;
        }

        View.OnClickListener menuClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                int id = v.getId();
                if (id == R.id.menu_alerts) {
                    startActivity(new Intent(MainActivity.this, ManageAlertsActivity.class));
                } else if (id == R.id.menu_history) {
                    startActivity(new Intent(MainActivity.this, AlertHistoryActivity.class));
                } else if (id == R.id.menu_customize) {
                    startActivity(new Intent(MainActivity.this, CustomizeActivity.class));
                } else if (id == R.id.menu_settings) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                } else if (id == R.id.menu_share) {
                    shareApp();
                } else if (id == R.id.menu_about) {
                    startActivity(new Intent(MainActivity.this, AboutActivity.class));
                }
            }
        };
        dialogView.findViewById(R.id.menu_alerts).setOnClickListener(menuClick);
        dialogView.findViewById(R.id.menu_history).setOnClickListener(menuClick);
        dialogView.findViewById(R.id.menu_customize).setOnClickListener(menuClick);
        dialogView.findViewById(R.id.menu_settings).setOnClickListener(menuClick);
        dialogView.findViewById(R.id.menu_share).setOnClickListener(menuClick);
        dialogView.findViewById(R.id.menu_about).setOnClickListener(menuClick);

        dialog.show();
    }

    private void setupZmanimList() {
        zmanimList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ZmanimAdapter();
        adapter.setViewMode(ZmanimAdapter.VIEW_CARDS);

        int fontScale = prefs.getInt("font_scale", 2);
        adapter.setFontScale(fontScale);

        // Click on zman to show countdown
        adapter.setOnZmanClickListener(new ZmanimAdapter.OnZmanClickListener() {
            @Override
            public void onZmanClick(ZmanItem zmanItem) {
                if (zmanItem.getTime() != null && !zmanItem.isPassed()) {
                    showCountdown(zmanItem.getName(), zmanItem.getTime());
                }
            }
        });

        zmanimList.setAdapter(adapter);
    }

    private void showCountdown(String zmanName, Date targetTime) {
        countdownTarget = targetTime;
        countdownZmanName = zmanName;
        countdownLabel.setText("עד " + zmanName + ":");
        countdownContainer.setVisibility(View.VISIBLE);

        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long diff = countdownTarget.getTime() - System.currentTimeMillis();
                if (diff <= 0) {
                    countdownTimer.setText("הגיע הזמן!");
                    handler.postDelayed(new Runnable() {
                        @Override public void run() { hideCountdown(); }
                    }, 3000);
                    return;
                }
                long hours = diff / (1000 * 60 * 60);
                long minutes = (diff / (1000 * 60)) % 60;
                long seconds = (diff / 1000) % 60;
                countdownTimer.setText(String.format(Locale.getDefault(),
                        "%02d:%02d:%02d", hours, minutes, seconds));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(countdownRunnable);
    }

    private void hideCountdown() {
        countdownContainer.setVisibility(View.GONE);
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
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
            nextAlertText.setText(sdf.format(nextAlert));
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
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
    }
}
