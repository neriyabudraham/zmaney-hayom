package com.botomat.zmaneyhayom.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.botomat.zmaneyhayom.database.DatabaseHelper;
import com.botomat.zmaneyhayom.models.AlertRule;
import com.botomat.zmaneyhayom.models.OffsetType;
import com.botomat.zmaneyhayom.models.ZmanType;
import com.botomat.zmaneyhayom.receivers.AlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    private static final String EXTRA_RULE_ID = "rule_id";
    private static final String EXTRA_ZMAN_TYPE = "zman_type";
    private static final String EXTRA_ZMAN_TIME = "zman_time";
    private static final String EXTRA_OFFSET_TYPE = "offset_type";
    private static final String EXTRA_OFFSET_MINUTES = "offset_minutes";
    private static final String EXTRA_SOUND = "sound_enabled";
    private static final String EXTRA_VIBRATE = "vibrate_enabled";

    public static void scheduleAllAlarms(Context context) {
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        List<AlertRule> rules = db.getEnabledAlertRules();
        ZmanimCalculator calculator = new ZmanimCalculator(context);

        for (AlertRule rule : rules) {
            scheduleAlarm(context, rule, calculator);
        }

        // Schedule midnight recalculation
        scheduleMidnightRecalc(context);

        Log.d(TAG, "Scheduled " + rules.size() + " alarms");
    }

    public static void scheduleAlarm(Context context, AlertRule rule, ZmanimCalculator calculator) {
        Date zmanTime = calculator.getZmanTime(rule.getZmanType());
        if (zmanTime == null) return;

        long alertTimeMs = calculateAlertTime(zmanTime, rule.getOffsetType(), rule.getOffsetMinutes());

        // If time already passed today, schedule for tomorrow
        if (alertTimeMs <= System.currentTimeMillis()) {
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            Date tomorrowZman = calculator.getZmanTime(rule.getZmanType(), tomorrow);
            if (tomorrowZman == null) return;
            alertTimeMs = calculateAlertTime(tomorrowZman, rule.getOffsetType(), rule.getOffsetMinutes());
            zmanTime = tomorrowZman;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(EXTRA_RULE_ID, rule.getId());
        intent.putExtra(EXTRA_ZMAN_TYPE, rule.getZmanType().name());
        intent.putExtra(EXTRA_ZMAN_TIME, zmanTime.getTime());
        intent.putExtra(EXTRA_OFFSET_TYPE, rule.getOffsetType().getValue());
        intent.putExtra(EXTRA_OFFSET_MINUTES, rule.getOffsetMinutes());
        intent.putExtra(EXTRA_SOUND, rule.isSoundEnabled());
        intent.putExtra(EXTRA_VIBRATE, rule.isVibrateEnabled());

        int requestCode = (int) rule.getId();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alertTimeMs, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alertTimeMs, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, alertTimeMs, pendingIntent);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Log.d(TAG, "Alarm scheduled for " + rule.getZmanType().getHebrewName() +
                    " at " + sdf.format(new Date(alertTimeMs)));
        }
    }

    public static void cancelAlarm(Context context, long ruleId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) ruleId, intent, flags);
        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
            pendingIntent.cancel();
        }
    }

    public static void cancelAllAlarms(Context context) {
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        List<AlertRule> rules = db.getAllAlertRules();
        for (AlertRule rule : rules) {
            cancelAlarm(context, rule.getId());
        }
    }

    public static Date getNextAlertTime(Context context) {
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        List<AlertRule> rules = db.getEnabledAlertRules();
        ZmanimCalculator calculator = new ZmanimCalculator(context);

        Date earliest = null;
        long now = System.currentTimeMillis();

        for (AlertRule rule : rules) {
            Date zmanTime = calculator.getZmanTime(rule.getZmanType());
            if (zmanTime == null) continue;

            long alertTimeMs = calculateAlertTime(zmanTime, rule.getOffsetType(), rule.getOffsetMinutes());

            if (alertTimeMs > now) {
                Date alertDate = new Date(alertTimeMs);
                if (earliest == null || alertDate.before(earliest)) {
                    earliest = alertDate;
                }
            }
        }

        return earliest;
    }

    private static long calculateAlertTime(Date zmanTime, OffsetType offsetType, int offsetMinutes) {
        long baseTime = zmanTime.getTime();
        switch (offsetType) {
            case MINUTES_BEFORE:
                return baseTime - (offsetMinutes * 60 * 1000L);
            case MINUTES_AFTER:
                return baseTime + (offsetMinutes * 60 * 1000L);
            case AT_TIME:
            default:
                return baseTime;
        }
    }

    private static void scheduleMidnightRecalc(Context context) {
        Calendar midnight = Calendar.getInstance();
        midnight.add(Calendar.DAY_OF_MONTH, 1);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 1);
        midnight.set(Calendar.SECOND, 0);

        Intent intent = new Intent(context, com.botomat.zmaneyhayom.receivers.MidnightReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 99999, intent, flags);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        midnight.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                        midnight.getTimeInMillis(), pendingIntent);
            }
        }
    }
}
