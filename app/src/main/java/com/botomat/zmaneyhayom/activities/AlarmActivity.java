package com.botomat.zmaneyhayom.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.models.OffsetType;
import com.botomat.zmaneyhayom.models.ZmanType;
import com.botomat.zmaneyhayom.receivers.AlarmReceiver;
import com.botomat.zmaneyhayom.utils.AlarmScheduler;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity {

    private static final long AUTO_STOP_MS = 60 * 1000L; // 1 minute

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private Handler autoStopHandler;
    private Runnable autoStopRunnable;

    private static final int MAX_RING_CYCLES = 3;
    private long alarmId;
    private int ringCycle = 1;
    private String zmanTypeName;
    private long zmanTimeMs;
    private String offsetTypeStr;
    private int offsetMinutes;
    private boolean soundEnabled;
    private boolean vibrateEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        setContentView(R.layout.activity_alarm);

        // Get alarm data
        Intent i = getIntent();
        alarmId = i.getLongExtra(AlarmScheduler.EXTRA_RULE_ID, -1);
        zmanTypeName = i.getStringExtra(AlarmScheduler.EXTRA_ZMAN_TYPE);
        zmanTimeMs = i.getLongExtra(AlarmScheduler.EXTRA_ZMAN_TIME, 0);
        offsetTypeStr = i.getStringExtra(AlarmScheduler.EXTRA_OFFSET_TYPE);
        offsetMinutes = i.getIntExtra(AlarmScheduler.EXTRA_OFFSET_MINUTES, 0);
        soundEnabled = i.getBooleanExtra(AlarmScheduler.EXTRA_SOUND, true);
        vibrateEnabled = i.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true);
        ringCycle = i.getIntExtra("ring_cycle", 1);

        ZmanType zmanType = ZmanType.fromString(zmanTypeName);
        OffsetType offsetType = OffsetType.fromString(offsetTypeStr);

        TextView zmanNameView = findViewById(R.id.alarm_zman_name);
        TextView timeView = findViewById(R.id.alarm_time);
        TextView detailsView = findViewById(R.id.alarm_details);

        zmanNameView.setText(zmanType.getHebrewName());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeView.setText(sdf.format(new Date()));

        String details;
        switch (offsetType) {
            case MINUTES_BEFORE:
                details = offsetMinutes + " דקות לפני " + zmanType.getHebrewName();
                details += "\nהזמן: " + sdf.format(new Date(zmanTimeMs));
                break;
            case MINUTES_AFTER:
                details = offsetMinutes + " דקות אחרי " + zmanType.getHebrewName();
                details += "\nהזמן: " + sdf.format(new Date(zmanTimeMs));
                break;
            default:
                details = "הזמן הגיע!";
                break;
        }
        detailsView.setText(details);

        // Update auto-stop hint to show cycle
        TextView hint = findViewById(R.id.auto_stop_hint);
        if (hint != null) {
            if (ringCycle >= MAX_RING_CYCLES) {
                hint.setText("צלצול אחרון מתוך " + MAX_RING_CYCLES + " · יפסיק בעוד 60 שניות");
            } else {
                hint.setText("צלצול " + ringCycle + " מתוך " + MAX_RING_CYCLES
                        + " · יפסיק בעוד 60 שניות");
            }
        }

        // Start sound + vibration
        startAlarm();

        // Auto-stop after 1 minute
        autoStopHandler = new Handler();
        autoStopRunnable = new Runnable() {
            @Override public void run() {
                stopAlarmPlayback();
                if (ringCycle < MAX_RING_CYCLES) {
                    // Auto-snooze so it retries later
                    snoozeAlarm();
                }
                // After 3 cycles - just stop completely
                finish();
            }
        };
        autoStopHandler.postDelayed(autoStopRunnable, AUTO_STOP_MS);

        // Dismiss button - stop completely
        MaterialButton dismissBtn = findViewById(R.id.btn_dismiss);
        dismissBtn.setOnClickListener(new android.view.View.OnClickListener() {
            @Override public void onClick(android.view.View v) {
                stopAlarmPlayback();
                finish();
            }
        });

        // Snooze button
        MaterialButton snoozeBtn = findViewById(R.id.btn_snooze);
        int snoozeDuration = Integer.parseInt(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("snooze_duration", "5"));
        snoozeBtn.setText("נודניק (" + snoozeDuration + " דק')");
        snoozeBtn.setOnClickListener(new android.view.View.OnClickListener() {
            @Override public void onClick(android.view.View v) {
                stopAlarmPlayback();
                snoozeAlarm();
                finish();
            }
        });
    }

    private void startAlarm() {
        if (soundEnabled) {
            String customUri = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("custom_ringtone", null);

            boolean played = false;
            // Try custom URI first
            if (customUri != null) {
                played = tryPlay(Uri.parse(customUri));
            }
            // Fallback to default if custom failed
            if (!played) {
                Uri def = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (def == null) def = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                if (def == null) def = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                if (def != null) tryPlay(def);
            }
        }

        if (vibrateEnabled) {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 600, 400};
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private boolean tryPlay(Uri uri) {
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(this, uri);
            mp.setAudioStreamType(AudioManager.STREAM_ALARM);
            mp.setLooping(true);
            mp.prepare();
            mp.start();
            mediaPlayer = mp;
            return true;
        } catch (Exception e) {
            android.util.Log.w("AlarmActivity", "Failed to play " + uri, e);
            return false;
        }
    }

    private void stopAlarmPlayback() {
        if (autoStopHandler != null && autoStopRunnable != null) {
            autoStopHandler.removeCallbacks(autoStopRunnable);
        }
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception ignored) {}
        try {
            if (vibrator != null) vibrator.cancel();
        } catch (Exception ignored) {}
    }

    private void snoozeAlarm() {
        try {
            int snoozeMin = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("snooze_duration", "5"));
            long fireAt = System.currentTimeMillis() + snoozeMin * 60 * 1000L;

            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra(AlarmScheduler.EXTRA_RULE_ID, alarmId);
            intent.putExtra(AlarmScheduler.EXTRA_ZMAN_TYPE, zmanTypeName);
            intent.putExtra(AlarmScheduler.EXTRA_ZMAN_TIME, zmanTimeMs);
            intent.putExtra(AlarmScheduler.EXTRA_OFFSET_TYPE, offsetTypeStr);
            intent.putExtra(AlarmScheduler.EXTRA_OFFSET_MINUTES, offsetMinutes);
            intent.putExtra(AlarmScheduler.EXTRA_SOUND, soundEnabled);
            intent.putExtra(AlarmScheduler.EXTRA_VIBRATE, vibrateEnabled);
            intent.putExtra("is_snooze", true);
            intent.putExtra("ring_cycle", ringCycle + 1);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            // Use a different request code for snooze to not conflict
            int reqCode = (int) (alarmId * 1000 + (System.currentTimeMillis() % 100));
            PendingIntent pi = PendingIntent.getBroadcast(this, reqCode, intent, flags);
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    am.setExact(AlarmManager.RTC_WAKEUP, fireAt, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, fireAt, pi);
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onBackPressed() {
        // Prevent accidental dismiss - user must press button
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmPlayback();
    }
}
