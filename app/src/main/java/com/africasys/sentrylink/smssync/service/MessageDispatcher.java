package com.africasys.sentrylink.smssync.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import com.africasys.sentrylink.smssync.config.MessageConfig;
import com.africasys.sentrylink.smssync.crypto.CryptoManager;
import com.africasys.sentrylink.smssync.enums.MessageType;
import com.africasys.sentrylink.smssync.models.AuthenticatedUser;
import com.africasys.sentrylink.smssync.models.Contact;
import com.africasys.sentrylink.smssync.models.EncryptionKey;
import com.africasys.sentrylink.smssync.repository.AppDatabase;
import com.africasys.sentrylink.smssync.repository.ConfigRepository;
import com.africasys.sentrylink.smssync.repository.ContactDao;
import com.africasys.sentrylink.smssync.repository.EncryptionKeyDao;
import com.africasys.sentrylink.smssync.repository.UserRepository;
import com.africasys.sentrylink.smssync.utils.MessageHelpers;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Dispatcher de messages — canal unique SMS chiffré.
 *
 * Chaque message est chiffré selon la disponibilité de la clé publique du
 * destinataire : - Préfixe SL0 : chiffrement avec la clé personnelle du contact
 * (DEVICE_UNIQUE). - Préfixe SL1 : chiffrement avec la clé partagée de l'unité
 * (second QR, fallback).
 */
public class MessageDispatcher {

    private static final String TAG = "SL-MessageDispatcher";

    private final Context context;
    private final ConfigRepository configRepository;
    private final ContactDao contactDao;
    private final EncryptionKeyDao keyDao;
    private final UserRepository userRepository;

    public interface DispatchCallback {
        void onSuccess(String channel);

        void onFailure(String error);
    }

    /** Callback interne pour la résolution de la clé publique du destinataire. */
    private interface KeyCallback {
        void onResolved(String publicKeyPem);

        void onFallback();
    }

    public MessageDispatcher(Context context) {
        this.context = context;
        this.configRepository = ConfigRepository.getInstance(context);
        this.userRepository = UserRepository.getInstance(context);
        AppDatabase db = AppDatabase.getInstance(context);
        this.contactDao = db.contactDao();
        this.keyDao = db.encryptionKeyDao();
    }

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Envoie un message via SMS chiffré. Le préfixe (SL0/SL1) est déterminé
     * automatiquement selon la clé disponible.
     */
    public void dispatch(String recipient, String content, MessageType messageType, DispatchCallback callback) {
        try {
            sendEncryptedSms(recipient, content, messageType);
            callback.onSuccess("SMS");
        } catch (Exception e) {
            Log.e(TAG, "Erreur dispatch SMS vers " + recipient, e);
            callback.onFailure("Erreur SMS: " + e.getMessage());
        }
    }

    /**
     * Envoie un message SMS à toutes les tours de contrôle (contacts CENTRAL_SERVER
     * actifs).
     */
    public void dispatchToControlTower(String content, MessageType messageType, DispatchCallback callback) {
        Log.i(TAG, "  [TOWER] Recherche contacts CENTRAL_SERVER actifs en DB...");
        List<Contact> towers = contactDao.getActiveCentralServers();

        if (towers.isEmpty()) {
            Log.e(TAG, "  [TOWER] ✗ Aucune tour de contrôle (CENTRAL_SERVER ACTIVE) trouvée en base"
                    + " — vérifier que les contacts ont bien été synchronisés");
            callback.onFailure("Aucune tour de contrôle configurée en base");
            return;
        }

        Log.i(TAG, "  [TOWER] " + towers.size() + " tour(s) de contrôle trouvée(s):");
        for (Contact t : towers) {
            Log.i(TAG, "    → " + t.name + " | tél: " + t.phoneNumber
                    + " | id=" + t.id + " | status=" + t.status);
        }

        sendToAllTowers(towers, content, messageType, callback);
    }

    // -------------------------------------------------------------------------
    // Résolution de clé avec sélection du préfixe (SL0 / SL1)
    // -------------------------------------------------------------------------

    /**
     * Résultat de la résolution de clé : clé publique PEM et préfixe à utiliser
     * (SL0 = clé personnelle, SL1 = clé partagée de l'unité).
     */
    private static class KeyInfo {
        final String publicKeyPem;
        final String prefix;

        KeyInfo(String publicKeyPem, String prefix) {
            this.publicKeyPem = publicKeyPem;
            this.prefix = prefix;
        }
    }

    /**
     * Résout la clé publique et le préfixe à utiliser pour chiffrer un message
     * destiné à {@code phoneNumber}.
     *
     * <ul>
     * <li>Si le contact possède une clé DEVICE_UNIQUE active → clé personnelle +
     * préfixe {@code SL0}.</li>
     * <li>Sinon → clé partagée de l'unité (second QR) + préfixe {@code SL1}.</li>
     * </ul>
     *
     * @return {@link KeyInfo} ou {@code null} si aucune clé n'est disponible.
     */
    private KeyInfo resolveKeyInfo(String phoneNumber) {
        Log.d(TAG, "  [KEY] Résolution clé pour: " + phoneNumber);

        // 1. Clé personnelle du contact (DEVICE_UNIQUE active) → SL0
        Contact contact = contactDao.findByPhoneNumber(phoneNumber);
        if (contact == null) {
            Log.w(TAG, "  [KEY] Contact introuvable en DB pour le numéro: " + phoneNumber);
        } else {
            Log.d(TAG, "  [KEY] Contact trouvé: " + contact.name + " (id=" + contact.id
                    + " | type=" + contact.type + " | status=" + contact.status + ")");
            EncryptionKey key = keyDao.getActiveKeyForContact(contact.id);
            if (key != null && key.value != null && !key.value.isEmpty()) {
                Log.i(TAG, "  [KEY] ✓ Clé personnelle DEVICE_UNIQUE trouvée pour " + phoneNumber
                        + " (keyId=" + key.id + " | " + key.value.length() + " chars) → préfixe "
                        + MessageConfig.PERSONAL_KEY_PREFIX);
                return new KeyInfo(key.value, MessageConfig.PERSONAL_KEY_PREFIX);
            } else {
                Log.w(TAG, "  [KEY] Pas de clé ACTIVE DEVICE_UNIQUE pour contact id=" + contact.id
                        + " — passage au fallback SL1");
            }
        }

        // 2. Clé partagée de l'unité (second QR code) → SL1
        String unitPublicKey = configRepository.getUnitPublicKey();
        if (unitPublicKey != null && !unitPublicKey.isEmpty()) {
            Log.w(TAG, "  [KEY] ⚠ Fallback SL1 pour " + phoneNumber
                    + " — clé partagée de l'unité (" + unitPublicKey.length() + " chars)"
                    + " → préfixe " + MessageConfig.SHARED_KEY_PREFIX);
            return new KeyInfo(unitPublicKey, MessageConfig.SHARED_KEY_PREFIX);
        }

        Log.e(TAG, "  [KEY] ✗ AUCUNE clé disponible pour " + phoneNumber
                + " (ni DEVICE_UNIQUE ni clé partagée d'unité) — message impossible");
        return null;
    }

    /**
     * @deprecated Utiliser {@link #resolveKeyInfo(String)}.
     */
    @Deprecated
    private String resolvePublicKey(String phoneNumber) {
        KeyInfo info = resolveKeyInfo(phoneNumber);
        return (info != null) ? info.publicKeyPem : null;
    }

    /**
     * @deprecated Utiliser {@link #resolveKeyInfo(String)}.
     */
    @Deprecated
    private void resolveRecipientKey(String phoneNumber, KeyCallback callback) {
        String pem = resolvePublicKey(phoneNumber);
        if (pem != null) {
            callback.onResolved(pem);
        } else {
            callback.onFallback();
        }
    }

    // -------------------------------------------------------------------------
    // Canal SMS
    // -------------------------------------------------------------------------

    /**
     * Construit le corps SMS chiffré+préfixé prêt à l'envoi. Doit être appelé avant
     * toute sauvegarde en base pour que la valeur stockée soit identique à ce qui
     * sera réellement transmis.
     *
     * <p>
     * Le préfixe est déterminé automatiquement :
     * <ul>
     * <li>{@code SL0} si le destinataire possède une clé personnelle
     * (DEVICE_UNIQUE).</li>
     * <li>{@code SL1} si seule la clé partagée de l'unité est disponible.</li>
     * </ul>
     */
    public String buildEncryptedSmsBody(String recipient, String content, MessageType messageType) throws Exception {
        KeyInfo keyInfo = resolveKeyInfo(recipient);
        if (keyInfo == null) {
            throw new RuntimeException("Aucune clé de chiffrement disponible pour " + recipient
                    + " (ni clé personnelle ni clé partagée d'unité)");
        }

        AuthenticatedUser user = userRepository.getCurrentUser();
        long unityId = (user != null) ? user.contactId : 0L;
        String formattedContent = MessageHelpers.formatMessage(unityId, messageType, content);
        Log.d(TAG, "SMS avant chiffrement → " + formattedContent);

        CryptoManager cryptoManager = new CryptoManager();
        PublicKey publicKey = CryptoManager.stringToPublicKey(keyInfo.publicKeyPem);
        String encrypted = cryptoManager.prepareSms(formattedContent, publicKey);
        if (encrypted == null || encrypted.isEmpty()) {
            throw new RuntimeException("Échec du chiffrement du message SMS (résultat vide)");
        }
        Log.d(TAG, "SMS après chiffrement → " + encrypted + " | préfixe : " + keyInfo.prefix);
        return keyInfo.prefix + encrypted;
    }

    /**
     * Envoie un corps SMS déjà chiffré (construit via
     * {@link #buildEncryptedSmsBody}). Évite le double-chiffrement quand l'appelant
     * a déjà le contenu chiffré.
     */
    public void dispatchPreEncrypted(String recipient, String encryptedBody, DispatchCallback callback) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(encryptedBody);
            PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, new Intent("SMS_SENT"),
                    PendingIntent.FLAG_IMMUTABLE);
            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            for (int i = 0; i < parts.size(); i++) {
                sentIntents.add(sentIntent);
            }
            smsManager.sendMultipartTextMessage(recipient, null, parts, sentIntents, null);
            Log.d(TAG, "SMS chiffré envoyé à " + recipient + " (" + parts.size() + " parties)");
            callback.onSuccess("SMS");
        } catch (Exception e) {
            Log.e(TAG, "Erreur envoi SMS pré-chiffré", e);
            callback.onFailure("Erreur SMS: " + e.getMessage());
        }
    }

    private void sendEncryptedSms(String phoneNumber, String content, MessageType messageType) throws Exception {
        KeyInfo keyInfo = resolveKeyInfo(phoneNumber);
        if (keyInfo == null) {
            throw new RuntimeException("Aucune clé de chiffrement disponible pour " + phoneNumber);
        }

        AuthenticatedUser user = userRepository.getCurrentUser();
        long unityId = (user != null) ? user.contactId : 0L;
        String formattedContent = MessageHelpers.formatMessage(unityId, messageType, content);
        Log.d(TAG, "  [ENCRYPT] Texte avant chiffrement (" + formattedContent.length() + " chars): "
                + formattedContent);

        CryptoManager cryptoManager = new CryptoManager();
        PublicKey publicKey = CryptoManager.stringToPublicKey(keyInfo.publicKeyPem);
        String encryptedSms = cryptoManager.prepareSms(formattedContent, publicKey);
        if (encryptedSms == null || encryptedSms.isEmpty()) {
            throw new RuntimeException("Échec du chiffrement du message SMS (résultat vide)");
        }

        String fullSms = keyInfo.prefix + encryptedSms;
        Log.d(TAG, "  [ENCRYPT] ✓ Chiffrement OK — payload: " + encryptedSms.length()
                + " chars | SMS complet avec préfixe: " + fullSms.length() + " chars"
                + " | préfixe: " + keyInfo.prefix);

        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(fullSms);

        Log.d(TAG, "  [SMS] Envoi multipart: " + parts.size() + " partie(s) → " + phoneNumber);

        PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, new Intent("SMS_SENT"),
                PendingIntent.FLAG_IMMUTABLE);
        ArrayList<PendingIntent> sentIntents = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            sentIntents.add(sentIntent);
        }

        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null);
        Log.i(TAG, "  [SMS] ✓ SMS chiffré envoyé à " + phoneNumber
                + " (" + parts.size() + " partie(s)) | préfixe: " + keyInfo.prefix);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void sendToAllTowers(List<Contact> towers, String content, MessageType messageType,
            DispatchCallback callback) {
        Log.i(TAG, "  [SEND] Envoi vers " + towers.size() + " tour(s) | type=" + messageType);
        int successCount = 0;
        int failCount = 0;

        for (Contact tower : towers) {
            Log.d(TAG, "  [SEND] ── Tour: " + tower.name + " | tél: " + tower.phoneNumber);
            try {
                sendEncryptedSms(tower.phoneNumber, content, messageType);
                successCount++;
                Log.i(TAG, "  [SEND]    ✓ SMS envoyé à " + tower.name + " (" + tower.phoneNumber + ")");
            } catch (Exception e) {
                failCount++;
                Log.e(TAG, "  [SEND]    ✗ Échec SMS vers " + tower.name
                        + " (" + tower.phoneNumber + "): " + e.getMessage(), e);
            }
        }

        Log.i(TAG, "  [SEND] Résultat: " + successCount + " succès, " + failCount + " échec(s) sur "
                + towers.size() + " tour(s)");

        if (successCount > 0) {
            callback.onSuccess("SMS");
        } else {
            callback.onFailure("Échec envoi SMS à toutes les tours (" + failCount + " erreur(s))");
        }
    }
}
