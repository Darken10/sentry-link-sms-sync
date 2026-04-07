package com.africasys.sentrylink.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.africasys.sentrylink.crypto.CryptoManager;
import com.africasys.sentrylink.dtos.SMSDecryptedDTO;
import com.africasys.sentrylink.models.SMSMessage;
import com.africasys.sentrylink.repository.ConfigRepository;
import com.africasys.sentrylink.repository.SMSRepository;
import com.africasys.sentrylink.utils.MessageHelpers;

import java.security.PrivateKey;

/**
 * BroadcastReceiver secondaire pour les SMS entrants. Déchiffre les messages
 * SentryLink, vérifie la signature, et ignore les messages non-SentryLink.
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SL-SmsReceiverAlt";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final int SMS_TYPE_INBOX = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SMS_RECEIVED.equals(intent.getAction()))
            return;

        Bundle bundle = intent.getExtras();
        if (bundle == null)
            return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        if (pdus == null)
            return;

        try {
            // SmsEncryptor smsEncryptor = SmsEncryptor.getInstance();
            SMSRepository repository = new SMSRepository(context);
            String privateKeyPem = ConfigRepository.getInstance(context).getPrivateKey();

            // Reconstituer le message complet (multi-part)
            StringBuilder fullMessage = new StringBuilder();
            String expediteur = null;
            long timestamp = System.currentTimeMillis();

            for (Object pdu : pdus) {
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                if (smsMessage != null) {
                    if (expediteur == null) {
                        expediteur = smsMessage.getDisplayOriginatingAddress();
                        timestamp = smsMessage.getTimestampMillis();
                    }
                    fullMessage.append(smsMessage.getMessageBody());
                }
            }

            if (expediteur == null || fullMessage.length() == 0)
                return;

            String messageBody = fullMessage.toString();

            if (MessageHelpers.isSentryLinkMessage(messageBody)) {
                // Déchiffrer
                CryptoManager cryptoManager = new CryptoManager();
                PrivateKey privateKey = CryptoManager.stringToPrivateKey(privateKeyPem);
                String messsageContent = cryptoManager.receiveSms(messageBody, privateKey);
                SMSDecryptedDTO decrypted = new SMSDecryptedDTO(messsageContent);

                if (decrypted.isSignatureValid()) {
                    // Stocker avec préfixe SL1 pour que le filtre de conversation fonctionne
                    SMSMessage sms = new SMSMessage(expediteur, decrypted.getMessage(), timestamp, SMS_TYPE_INBOX);
                    repository.saveSMS(sms);
                    Log.d(TAG, "Message SentryLink reçu de: " + expediteur);

                    // Notifier l'UI
                    Intent broadcastLocal = new Intent("com.africasys.sentrylink.NEW_SMS");
                    broadcastLocal.putExtra("expediteur", expediteur);
                    broadcastLocal.putExtra("message", decrypted.getMessage());
                    broadcastLocal.putExtra("type", decrypted.getType());
                    broadcastLocal.setPackage(context.getPackageName());
                    context.sendBroadcast(broadcastLocal);
                } else {
                    Log.w(TAG, "Message SentryLink rejeté (signature invalide) de: " + expediteur);
                }
            } else {
                Log.d(TAG, "SMS non-SentryLink ignoré de: " + expediteur);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur réception SMS", e);
        }
    }
}
