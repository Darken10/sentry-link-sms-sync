package com.africasys.sentrylink.smssync.service.message;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.africasys.sentrylink.smssync.dtos.SMSDecryptedDTO;
import com.africasys.sentrylink.smssync.models.SMSMessage;
import com.africasys.sentrylink.smssync.repository.SMSRepository;

/**
 * Traitement pour les messages de type MSG : sauvegarde en DB et notification UI.
 */
public class MsgMessageHandler implements MessageHandler {
    private static final String TAG = "SL-MsgHandler";

    @Override
    public void handle(Context context, String sender, SMSDecryptedDTO dto, long receivedTimestamp, String prefix) {
        try {
            SMSRepository repo = new SMSRepository(context);
            SMSMessage sms = new SMSMessage(sender, dto.getMessage(), receivedTimestamp, 1);
            repo.saveSMS(sms);

            // Broadcast local pour UI
            Intent broadcastLocal = new Intent("com.africasys.sentrylink.NEW_SMS");
            broadcastLocal.putExtra("expediteur", sender);
            broadcastLocal.putExtra("message", dto.getMessage());
            broadcastLocal.putExtra("type", dto.getType().name());
            broadcastLocal.setPackage(context.getPackageName());
            context.sendBroadcast(broadcastLocal);

            Log.i(TAG, "MSG traité et stocké pour: " + sender);
        } catch (Exception e) {
            Log.e(TAG, "Erreur sauvegarde MSG", e);
        }
    }
}

