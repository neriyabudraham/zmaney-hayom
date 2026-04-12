package com.botomat.zmaneyhayom.activities;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.models.OffsetType;
import com.botomat.zmaneyhayom.models.ZmanType;
import com.botomat.zmaneyhayom.services.AlarmService;
import com.botomat.zmaneyhayom.utils.AlarmScheduler;
import com.botomat.zmaneyhayom.utils.PrefsHelper;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on and show over lock screen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        setContentView(R.layout.activity_alarm);

        // Get alarm data
        String zmanTypeName = getIntent().getStringExtra(AlarmScheduler.EXTRA_ZMAN_TYPE);
        long zmanTimeMs = getIntent().getLongExtra(AlarmScheduler.EXTRA_ZMAN_TIME, 0);
        String offsetTypeStr = getIntent().getStringExtra(AlarmScheduler.EXTRA_OFFSET_TYPE);
        int offsetMinutes = getIntent().getIntExtra(AlarmScheduler.EXTRA_OFFSET_MINUTES, 0);

        ZmanType zmanType = ZmanType.fromString(zmanTypeName);
        OffsetType offsetType = OffsetType.fromString(offsetTypeStr);

        // Setup views
        TextView zmanNameView = findViewById(R.id.alarm_zman_name);
        TextView timeView = findViewById(R.id.alarm_time);
        TextView detailsView = findViewById(R.id.alarm_details);

        zmanNameView.setText(zmanType.getHebrewName());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeView.setText(sdf.format(new Date()));

        String details = "";
        switch (offsetType) {
            case MINUTES_BEFORE:
                details = offsetMinutes + " דקות לפני " + zmanType.getHebrewName();
                details += "\nזמן: " + sdf.format(new Date(zmanTimeMs));
                break;
            case MINUTES_AFTER:
                details = offsetMinutes + " דקות אחרי " + zmanType.getHebrewName();
                details += "\nזמן: " + sdf.format(new Date(zmanTimeMs));
                break;
            default:
                details = "הזמן הגיע!";
                break;
        }
        detailsView.setText(details);

        // Dismiss button
        MaterialButton dismissBtn = findViewById(R.id.btn_dismiss);
        dismissBtn.setOnClickListener(v -> {
            AlarmService.stopAlarm(this);
            finish();
        });

        // Snooze button
        MaterialButton snoozeBtn = findViewById(R.id.btn_snooze);
        PrefsHelper prefs = new PrefsHelper(this);
        int snoozeDuration = prefs.getSnoozeDuration();
        snoozeBtn.setText("נודניק (" + snoozeDuration + " דק')");
        snoozeBtn.setOnClickListener(v -> {
            AlarmService.stopAlarm(this);
            // Snooze handled by rescheduling
            AlarmScheduler.scheduleAllAlarms(this);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent accidental dismiss
    }
}
