package com.botomat.zmaneyhayom.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.botomat.zmaneyhayom.activities.AlarmActivity;
import com.botomat.zmaneyhayom.database.DatabaseHelper;
import com.botomat.zmaneyhayom.models.AlertHistoryItem;
import com.botomat.zmaneyhayom.models.OffsetType;
import com.botomat.zmaneyhayom.models.ZmanType;
import com.botomat.zmaneyhayom.utils.AlarmScheduler;

import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    public static final int MAX_RING_CYCLES = 3;
    /** Drop snooze if more than this many minutes after the original zman time */
    private static final long STALE_SNOOZE_LIMIT_MS = 30 * 60 * 1000L; // 30 min

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");

        long ruleId = intent.getLongExtra(AlarmScheduler.EXTRA_RULE_ID, -1);
        String zmanTypeName = intent.getStringExtra(AlarmScheduler.EXTRA_ZMAN_TYPE);
        long zmanTimeMs = intent.getLongExtra(AlarmScheduler.EXTRA_ZMAN_TIME, 0);
        String offsetTypeStr = intent.getStringExtra(AlarmScheduler.EXTRA_OFFSET_TYPE);
        int offsetMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_OFFSET_MINUTES, 0);
        boolean soundEnabled = intent.getBooleanExtra(AlarmScheduler.EXTRA_SOUND, true);
        boolean vibrateEnabled = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true);
        boolean isSnooze = intent.getBooleanExtra("is_snooze", false);
        int ringCycle = intent.getIntExtra("ring_cycle", 1);

        // ===== CRITICAL SAFETY CHECKS =====

        // 1. Hard cycle limit - drop snoozes beyond cycle 3
        if (ringCycle > MAX_RING_CYCLES) {
            Log.w(TAG, "Dropping alarm - exceeded max cycles (" + ringCycle + ")");
            return;
        }

        // 2. Stale snooze - drop if too far past original zman time
        // Only applies to snoozes - regular alarm fires at scheduled time
        if (isSnooze && zmanTimeMs > 0) {
            long elapsedSinceZman = System.currentTimeMillis() - zmanTimeMs;
            if (elapsedSinceZman > STALE_SNOOZE_LIMIT_MS) {
                Log.w(TAG, "Dropping stale snooze - " + (elapsedSinceZman / 60000)
                        + " min past zman");
                return;
            }
        }

        // Wake the device
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = null;
        if (pm != null) {
            wl = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "zmaneyhayom:alarm_wake");
            wl.acquire(30 * 1000L);
        }

        ZmanType zmanType = ZmanType.fromString(zmanTypeName);
        OffsetType offsetType = OffsetType.fromString(offsetTypeStr);

        // Save to history (only first ring, not snoozes)
        if (!isSnooze) {
            AlertHistoryItem historyItem = new AlertHistoryItem(
                    zmanType, new Date(), new Date(zmanTimeMs), offsetType, offsetMinutes);
            DatabaseHelper.getInstance(context).insertAlertHistory(historyItem);
        }

        // Launch alarm activity
        try {
            Intent alarmIntent = new Intent(context, AlarmActivity.class);
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            alarmIntent.putExtra(AlarmScheduler.EXTRA_RULE_ID, ruleId);
            alarmIntent.putExtra(AlarmScheduler.EXTRA_ZMAN_TYPE, zmanTypeName);
            alarmIntent.putExtra(AlarmScheduler.EXTRA_ZMAN_TIME, zmanTimeMs);
            alarmIntent.putExtra(AlarmScheduler.EXTRA_OFFSET_TYPE, offsetTypeStr);
            alarmIntent.putExtra(AlarmScheduler.EXTRA_OFFSET_MINUTES, offsetMinutes);
            alarmIntent.putExtra(AlarmScheduler.EXTRA_SOUND, soundEnabled);
            alarmIntent.putExtra(AlarmScheduler.EXTRA_VIBRATE, vibrateEnabled);
            alarmIntent.putExtra("ring_cycle", ringCycle);
            context.startActivity(alarmIntent);
        } catch (Exception e) {
            Log.e(TAG, "Could not launch AlarmActivity", e);
        }

        // Reschedule daily alarms only on first ring (not snoozes)
        if (!isSnooze) {
            AlarmScheduler.scheduleAllAlarms(context);
        }

        if (wl != null && wl.isHeld()) {
            wl.release();
        }
    }
}
