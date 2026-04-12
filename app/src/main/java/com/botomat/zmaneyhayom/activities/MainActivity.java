package com.botomat.zmaneyhayom.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_VIEW_MODE = "view_mode";
    private static final int VIEW_LIST = 0;
    private static final int VIEW_CARDS = 1;
    private static final int VIEW_COMPACT = 2;

    private ZmanimAdapter adapter;
    private ZmanimCalculator calculator;
    private DatabaseHelper db;
    private Handler handler;
    private Runnable updateRunnable;
    private SharedPreferences prefs;
    private int currentViewMode;

    private TextView gregorianDate;
    private TextView hebrewDate;
    private LinearLayout nextAlertContainer;
    private TextView nextAlertText;
    private RecyclerView zmanimList;
    private ImageButton btnViewList, btnViewCards, btnViewCompact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        calculator = new ZmanimCalculator(this);
        db = DatabaseHelper.getInstance(this);
        handler = new Handler();
        currentViewMode = prefs.getInt(PREF_VIEW_MODE, VIEW_LIST);

        initViews();
        setupToolbar();
        setupViewModeButtons();
        setupZmanimList();
        applyCustomBackground();
        updateDates();
        loadZmanim();

        AlarmScheduler.scheduleAllAlarms(this);

        updateRunnable = () -> {
            loadZmanim();
            updateNextAlert();
            handler.postDelayed(updateRunnable, 30000);
        };
    }

    private void initViews() {
        gregorianDate = findViewById(R.id.gregorian_date);
        hebrewDate = findViewById(R.id.hebrew_date);
        nextAlertContainer = findViewById(R.id.next_alert_container);
        nextAlertText = findViewById(R.id.next_alert_text);
        zmanimList = findViewById(R.id.zmanim_list);
        btnViewList = findViewById(R.id.btn_view_list);
        btnViewCards = findViewById(R.id.btn_view_cards);
        btnViewCompact = findViewById(R.id.btn_view_compact);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_manage_alerts) {
                startActivity(new Intent(this, ManageAlertsActivity.class));
                return true;
            } else if (id == R.id.action_share) {
                shareApp();
                return true;
            } else if (id == R.id.action_history) {
                startActivity(new Intent(this, AlertHistoryActivity.class));
                return true;
            } else if (id == R.id.action_customize) {
                startActivity(new Intent(this, CustomizeActivity.class));
                return true;
            } else if (id == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            } else if (id == R.id.action_about) {
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupViewModeButtons() {
        highlightViewMode(currentViewMode);

        btnViewList.setOnClickListener(v -> switchViewMode(VIEW_LIST));
        btnViewCards.setOnClickListener(v -> switchViewMode(VIEW_CARDS));
        btnViewCompact.setOnClickListener(v -> switchViewMode(VIEW_COMPACT));
    }

    private void switchViewMode(int mode) {
        currentViewMode = mode;
        prefs.edit().putInt(PREF_VIEW_MODE, mode).apply();
        highlightViewMode(mode);
        adapter.setViewMode(mode);
    }

    private void highlightViewMode(int mode) {
        btnViewList.setAlpha(mode == VIEW_LIST ? 1.0f : 0.4f);
        btnViewCards.setAlpha(mode == VIEW_CARDS ? 1.0f : 0.4f);
        btnViewCompact.setAlpha(mode == VIEW_COMPACT ? 1.0f : 0.4f);
    }

    private void setupZmanimList() {
        zmanimList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ZmanimAdapter();
        adapter.setViewMode(currentViewMode);

        // Apply font scale
        int fontScale = prefs.getInt("font_scale", 2);
        adapter.setFontScale(fontScale);

        zmanimList.setAdapter(adapter);
    }

    private void applyCustomBackground() {
        View root = findViewById(R.id.main_root);
        if (prefs.getBoolean("has_custom_bg", false)) {
            File bgFile = new File(getFilesDir(), "custom_bg.jpg");
            if (bgFile.exists()) {
                ImageView bgImage = new ImageView(this);
                bgImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                bgImage.setImageBitmap(BitmapFactory.decodeFile(bgFile.getAbsolutePath()));
                // Background handled via window
            }
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
        // D-pad navigation support
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            zmanimList.requestFocus();
            return zmanimList.dispatchKeyEvent(event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh font scale in case it changed
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
