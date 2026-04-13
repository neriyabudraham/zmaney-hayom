package com.botomat.zmaneyhayom.receivers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.activities.AlarmActivity;
import com.botomat.zmaneyhayom.database.DatabaseHelper;
import com.botomat.zmaneyhayom.models.AlertHistoryItem;
import com.botomat.zmaneyhayom.models.OffsetType;
import com.botomat.zmaneyhayom.models.ZmanType;
import com.botomat.zmaneyhayom.utils.AlarmScheduler;

import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "zmaney_hayom_alarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");

        // Wake the device
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = null;
        if (pm != null) {
            wl = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "zmaneyhayom:alarm_wake");
            wl.acquire(30 * 1000L);
        }

        String zmanTypeName = intent.getStringExtra(AlarmScheduler.EXTRA_ZMAN_TYPE);
        long zmanTimeMs = intent.getLongExtra(AlarmScheduler.EXTRA_ZMAN_TIME, 0);
        String offsetTypeStr = intent.getStringExtra(AlarmScheduler.EXTRA_OFFSET_TYPE);
        int offsetMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_OFFSET_MINUTES, 0);
        boolean soundEnabled = intent.getBooleanExtra(AlarmScheduler.EXTRA_SOUND, true);
        boolean vibrateEnabled = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true);

        ZmanType zmanType = ZmanType.fromString(zmanTypeName);
        OffsetType offsetType = OffsetType.fromString(offsetTypeStr);

        // Save to history
        AlertHistoryItem historyItem = new AlertHistoryItem(
                zmanType, new Date(), new Date(zmanTimeMs), offsetType, offsetMinutes);
        DatabaseHelper.getInstance(context).insertAlertHistory(historyItem);

        // Build alert text
        String alertTitle = zmanType.getHebrewName();
        String alertText;
        switch (offsetType) {
            case MINUTES_BEFORE:
                alertText = offsetMinutes + " דקות לפני " + alertTitle;
                break;
            case MINUTES_AFTER:
                alertText = offsetMinutes + " דקות אחרי " + alertTitle;
                break;
            default:
                alertText = "הזמן הגיע!";
                break;
        }

        // Show notification (works on all API levels)
        showNotification(context, alertTitle, alertText, soundEnabled);

        // Vibrate
        if (vibrateEnabled) {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                long[] pattern = {0, 800, 400, 800, 400, 800};
                v.vibrate(pattern, -1);
            }
        }

        // Play alarm sound
        if (soundEnabled) {
            try {
                Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alarmUri == null) {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
                final MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(context, alarmUri);
                mp.setAudioStreamType(AudioManager.STREAM_ALARM);
                mp.setLooping(false);
                mp.prepare();
                mp.start();
                // Stop after 10 seconds
                mp.setOnCompletionListener(MediaPlayer::release);
                new android.os.Handler().postDelayed(() -> {
                    try {
                        if (mp.isPlaying()) mp.stop();
                        mp.release();
                    } catch (Exception ignored) {}
                }, 10000);
            } catch (Exception e) {
                Log.e(TAG, "Error playing alarm", e);
            }
        }

        // Try to launch alarm activity
        try {
            Intent alarmIntent = new Intent(context, AlarmActivity.class);
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            alarmIntent.putExtra(AlarmScheduler.EXTRA_ZMAN_TYPE, zmanTypeName);
            alarmIntent.putExtra(AlarmScheduler.EXTRA_ZMAN_TIME, zmanTimeMs);
            alarmIntent.putExtra(AlarmScheduler.EXTRA_OFFSET_TYPE, offsetTypeStr);
            alarmIntent.putExtra(AlarmScheduler.EXTRA_OFFSET_MINUTES, offsetMinutes);
            context.startActivity(alarmIntent);
        } catch (Exception e) {
            Log.e(TAG, "Could not launch AlarmActivity", e);
        }

        // Reschedule for tomorrow
        AlarmScheduler.scheduleAllAlarms(context);

        if (wl != null && wl.isHeld()) {
            wl.release();
        }
    }

    private void showNotification(Context context, String title, String text, boolean withSound) {
        createNotificationChannel(context);

        Intent tapIntent = new Intent(context, AlarmActivity.class);
        tapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, tapIntent, flags);

        Uri soundUri = withSound ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) : null;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("זמני היום - " + title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS);

        if (withSound && soundUri != null) {
            builder.setSound(soundUri, AudioManager.STREAM_ALARM);
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "התראות זמני היום",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("התראות עבור זמני תפילה");
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
