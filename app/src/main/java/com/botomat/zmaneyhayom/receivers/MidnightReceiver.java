package com.botomat.zmaneyhayom.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.botomat.zmaneyhayom.utils.AlarmScheduler;

public class MidnightReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MidnightReceiver", "Midnight recalculation triggered");
        AlarmScheduler.scheduleAllAlarms(context);
    }
}
