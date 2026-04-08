package com.africasys.sentrylink.smssync.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.africasys.sentrylink.smssync.config.MessageConfig;
import com.africasys.sentrylink.smssync.crypto.CryptoManager;
import com.africasys.sentrylink.smssync.dtos.SMSDecryptedDTO;
// ...existing imports...
import com.africasys.sentrylink.smssync.service.message.MessageProcessor;
import com.africasys.sentrylink.smssync.repository.ConfigRepository;
import com.africasys.sentrylink.smssync.utils.MessageHelpers;

import java.security.PrivateKey;

/**
 * Reçoit les SMS entrants en temps réel.
 * Comportement : 1. Reconstitue le message complet (multi-part SMS) 2. Vérifie
 * le préfixe SentryLink (SL0 ou SL1) 3. Sélectionne la clé privée selon le
 * préfixe : - SL0 → clé privée personnelle de l'utilisateur - SL1 → clé privée
 * partagée de l'unité (fallback second QR) 4. Déchiffre avec ECIES 5. Si la
 * signature est valide et type=MSG → sauvegarde en DB et notifie l'UI
 *                                    6. Tout autre SMS est ignoré silencieusement
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SL-SmsReceiver";
    // constant SMS_TYPE_INBOX not needed here (handlers will handle storage)

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "┌── [RECEIVE] onReceive déclenché");

        if (intent == null) {
            Log.w(TAG, "└── Intent null — ignoré");
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.w(TAG, "└── Bundle null — ignoré");
            return;
        }

        try {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null || pdus.length == 0) {
                Log.w(TAG, "└── PDUs null ou vides — ignoré");
                return;
            }

            Log.d(TAG, "│   Nombre de PDUs : " + pdus.length);

            // Reconstituer le message complet (multi-part SMS)
            StringBuilder fullMessage = new StringBuilder();
            String phoneNumber = null;
            long timestamp = System.currentTimeMillis();

            for (Object pdu : pdus) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu, bundle.getString("format"));
                if (sms != null) {
                    if (phoneNumber == null) {
                        phoneNumber = sms.getOriginatingAddress();
                        timestamp = sms.getTimestampMillis();
                    }
                    fullMessage.append(sms.getMessageBody());
                }
            }

            if (phoneNumber == null || fullMessage.length() == 0) {
                Log.w(TAG, "└── Numéro ou corps vide après reconstruction — ignoré");
                return;
            }

            String body = fullMessage.toString();
            Log.d(TAG, "│   Expéditeur : " + phoneNumber);
            Log.d(TAG, "│   Corps reconstitué : " + body.length() + " chars | début: \""
                    + body.substring(0, Math.min(20, body.length())) + "...\"");

            // Vérifier le préfixe SentryLink
            if (!MessageHelpers.isSentryLinkMessage(body)) {
                Log.d(TAG, "└── SMS non-SentryLink (pas de préfixe SL0/SL1) — ignoré");
                return;
            }

            Log.i(TAG, "│   ✓ SMS SentryLink détecté de: " + phoneNumber
                    + " | préfixe: " + MessageHelpers.getMessagePrefix(body));
            decryptAndSave(context, phoneNumber, body, timestamp);

        } catch (Exception e) {
            Log.e(TAG, "└── [FATAL] Erreur inattendue lors de la réception SMS", e);
        }
    }

    /**
     * Déchiffre un payload SentryLink et sauvegarde le message en clair en DB.
     *
     * <p>
     * La clé privée est choisie selon le préfixe du message :
     * <ul>
     * <li>{@code SL0} → clé privée personnelle de l'utilisateur.</li>
     * <li>{@code SL1} → clé privée partagée de l'unité (second QR).</li>
     * </ul>
     */
    public static void decryptAndSave(Context context, String phoneNumber, String body, long timestamp) {
        Log.d(TAG, "│   [DECRYPT] Début decryptAndSave pour: " + phoneNumber);
        try {
            ConfigRepository config = ConfigRepository.getInstance(context);

            // Identifier le préfixe
            String prefix = MessageHelpers.getMessagePrefix(body);
            if (prefix == null) {
                Log.w(TAG, "└── [DECRYPT] Préfixe non reconnu — SMS ignoré");
                return;
            }
            Log.d(TAG, "│   [DECRYPT] Préfixe détecté : " + prefix);

            // Charger la clé privée correspondante
            String privateKeyPem;
            if (MessageConfig.PERSONAL_KEY_PREFIX.equals(prefix)) {
                privateKeyPem = config.getPrivateKey();
                Log.d(TAG, "│   [CLE] Utilisation clé personnelle (SL0)");
            } else {
                privateKeyPem = config.getUnitPrivateKey();
                Log.d(TAG, "│   [CLE] Utilisation clé partagée (SL1)");
            }

            if (privateKeyPem == null || privateKeyPem.isEmpty()) {
                Log.w(TAG, "└── [CLE] Clé privée " + prefix + " non configurée — SMS ignoré");
                return;
            }
            Log.d(TAG, "│   [CLE] Clé chargée (" + privateKeyPem.length() + " chars)");

            // Extraire le payload chiffré
            String encryptedPayload = MessageHelpers.getMessageContent(body);
            if (encryptedPayload.isEmpty()) {
                Log.w(TAG, "└── [DECRYPT] Payload chiffré vide — SMS ignoré");
                return;
            }
            Log.d(TAG, "│   [DECRYPT] Payload chiffré : " + encryptedPayload.length() + " chars");

            // Déchiffrement ECIES
            PrivateKey privateKey = CryptoManager.stringToPrivateKey(privateKeyPem);
            CryptoManager cryptoManager = new CryptoManager();
            String decryptedContent = cryptoManager.receiveSms(encryptedPayload, privateKey);
            Log.d(TAG, "│   [DECRYPT] Déchiffrement OK — texte clair : " + decryptedContent.length() + " chars");

            // Parser le contenu déchiffré
            SMSDecryptedDTO dto = new SMSDecryptedDTO(decryptedContent);
            if (!dto.isSignatureValid()) {
                Log.w(TAG, "└── [PARSE] Format header invalide après déchiffrement — SMS ignoré");
                return;
            }
            Log.d(TAG, "│   [PARSE] type=" + dto.getType()
                    + " | unityId=" + dto.getUnityId()
                    + " | msgTimestamp=" + dto.getTimestamp()
                    + " | message=\"" + dto.getMessage() + "\"");

            // Déléguer le traitement du message déchiffré au MessageProcessor
            // Celui-ci va appeler le handler adapté (MSG, SOS, LOC, ...)
            Log.d(TAG, "│   [PROCESS] Délégation au MessageProcessor pour type=" + dto.getType());
            MessageProcessor.getInstance().process(context, phoneNumber, dto, timestamp, prefix);
            Log.d(TAG, "└── [PROCESS] Délégation effectuée pour: " + phoneNumber);

        } catch (Exception e) {
            Log.e(TAG, "└── [FATAL] Erreur déchiffrement SMS de: " + phoneNumber, e);
        }
    }
}
