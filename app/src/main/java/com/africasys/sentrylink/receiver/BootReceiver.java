package com.africasys.sentrylink.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.africasys.sentrylink.service.SmsMonitorService;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "SL-BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.d(TAG, "Boot détecté - démarrage du service SMS");

            Intent serviceIntent = new Intent(context, SmsMonitorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8+ : obligatoire pour démarrer un ForegroundService
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
