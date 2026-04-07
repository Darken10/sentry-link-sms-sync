package com.africasys.sentrylink.smssync.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.africasys.sentrylink.smssync.repository.ConfigRepository;
import com.africasys.sentrylink.smssync.service.HttpGatewayService;
import com.africasys.sentrylink.smssync.service.SmsMonitorService;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "SL-BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }

        Log.d(TAG, "Boot détecté — démarrage des services SentryLink");

        // Service de surveillance SMS (toujours actif)
        startService(context, SmsMonitorService.class);

        // Passerelle HTTP — uniquement si activée par l'utilisateur
        if (ConfigRepository.getInstance(context).isHttpGatewayEnabled()) {
            Log.d(TAG, "  → Passerelle HTTP activée — démarrage au boot");
            startService(context, HttpGatewayService.class);
        }
    }

    private void startService(Context context, Class<?> serviceClass) {
        Intent intent = new Intent(context, serviceClass);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
