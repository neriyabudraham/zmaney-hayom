package com.botomat.zmaneyhayom.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.botomat.zmaneyhayom.activities.AlarmActivity;
import com.botomat.zmaneyhayom.database.DatabaseHelper;
import com.botomat.zmaneyhayom.models.AlertHistoryItem;
import com.botomat.zmaneyhayom.models.OffsetType;
import com.botomat.zmaneyhayom.models.ZmanType;
import com.botomat.zmaneyhayom.services.AlarmService;
import com.botomat.zmaneyhayom.utils.AlarmScheduler;

import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");

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

        // Start alarm service for sound/vibration
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra("sound_enabled", soundEnabled);
        serviceIntent.putExtra("vibrate_enabled", vibrateEnabled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // Launch alarm activity
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        alarmIntent.putExtra(AlarmScheduler.EXTRA_ZMAN_TYPE, zmanTypeName);
        alarmIntent.putExtra(AlarmScheduler.EXTRA_ZMAN_TIME, zmanTimeMs);
        alarmIntent.putExtra(AlarmScheduler.EXTRA_OFFSET_TYPE, offsetTypeStr);
        alarmIntent.putExtra(AlarmScheduler.EXTRA_OFFSET_MINUTES, offsetMinutes);
        context.startActivity(alarmIntent);

        // Reschedule for tomorrow
        AlarmScheduler.scheduleAllAlarms(context);
    }
}
