package com.africasys.sentrylink.smssync.service.message;

import android.content.Context;
import android.util.Log;

import com.africasys.sentrylink.smssync.dtos.SMSDecryptedDTO;
import com.africasys.sentrylink.smssync.models.SMSMessage;
import com.africasys.sentrylink.smssync.repository.SMSRepository;

/**
 * Handler par défaut pour les types non gérés explicitement.
 * Comportement minimal : journaliser et sauvegarder le texte en base.
 */
public class DefaultMessageHandler implements MessageHandler {
    private static final String TAG = "SL-DefaultHandler";

    @Override
    public void handle(Context context, String sender, SMSDecryptedDTO dto, long receivedTimestamp, String prefix) {
        try {
            SMSRepository repo = new SMSRepository(context);
            SMSMessage sms = new SMSMessage(sender, dto != null ? dto.getMessage() : "", receivedTimestamp, 0);
            repo.saveSMS(sms);
            Log.i(TAG, "Default handler: message saved for " + sender);
        } catch (Exception e) {
            Log.e(TAG, "Default handler error", e);
        }
    }
}
