package com.africasys.sentrylink.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.africasys.sentrylink.HomeActivity;
import com.africasys.sentrylink.R;
import com.africasys.sentrylink.broadcast.SmsReceiver;
import com.africasys.sentrylink.config.MessageConfig;
import com.africasys.sentrylink.crypto.CryptoManager;
import com.africasys.sentrylink.dtos.SMSDecryptedDTO;
import com.africasys.sentrylink.enums.MessageType;
import com.africasys.sentrylink.models.SMSMessage;
import com.africasys.sentrylink.repository.AppDatabase;
import com.africasys.sentrylink.repository.ConfigRepository;
import com.africasys.sentrylink.repository.SMSRepository;
import com.africasys.sentrylink.repository.SmsDao;
import com.africasys.sentrylink.utils.MessageHelpers;

import java.security.PrivateKey;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service foreground — tourne en permanence en arrière-plan.
 *
 * Au démarrage : scanne l'inbox SMS natif pour récupérer tous les messages
 * SentryLink existants, les déchiffre (ECIES/clé privée EC) et crée les
 * conversations correspondantes en DB.
 */
public class SmsMonitorService extends Service {

    private static final String TAG = "SL-SmsMonitorService";
    private static final String CHANNEL_ID = "sentry_link_sms_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int SMS_TYPE_INBOX = 1;

    /**
     * Empêche deux scans simultanés si onStartCommand est appelé plusieurs fois.
     */
    private static final AtomicBoolean scanRunning = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.i(TAG, "  SmsMonitorService créé");
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "▶ SmsMonitorService démarré (startId=" + startId + ")");
        startForeground(NOTIFICATION_ID, buildNotification());

        if (scanRunning.compareAndSet(false, true)) {
            Log.d(TAG, "  → Résolution des clés privées sur le thread principal...");
            String personalKeyPem = resolvePersonalPrivateKey();
            String unitKeyPem = resolveUnitPrivateKey();

            Log.d(TAG, "  → Clé personnelle (SL0) : " + (personalKeyPem != null && !personalKeyPem.isEmpty() ? "PRÉSENTE" : "ABSENTE"));
            Log.d(TAG, "  → Clé partagée  (SL1) : " + (unitKeyPem != null && !unitKeyPem.isEmpty() ? "PRÉSENTE" : "ABSENTE"));

            new Thread(() -> {
                try {
                    scanNativeInboxForSentryLinkMessages(personalKeyPem, unitKeyPem);
                } finally {
                    scanRunning.set(false);
                    Log.d(TAG, "  → Verrou scanRunning libéré");
                }
            }).start();

            Log.i(TAG, "  → Scan inbox lancé sur thread de fond");
        } else {
            Log.w(TAG, "  → Scan déjà en cours — démarrage ignoré");
        }

        return START_STICKY;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Résolution des clés privées (thread principal)
    // ─────────────────────────────────────────────────────────────────────────

    private String resolvePersonalPrivateKey() {
        try {
            String key = ConfigRepository.getInstance(this).getPrivateKey();
            if (key == null || key.isEmpty()) {
                Log.w(TAG, "  [CLE] Clé privée personnelle (SL0) non configurée");
            } else {
                Log.d(TAG, "  [CLE] Clé privée personnelle (SL0) chargée (" + key.length() + " chars)");
            }
            return key;
        } catch (Exception e) {
            Log.e(TAG, "  [CLE] Erreur lecture clé personnelle (SL0)", e);
            return null;
        }
    }

    private String resolveUnitPrivateKey() {
        try {
            String key = ConfigRepository.getInstance(this).getUnitPrivateKey();
            if (key == null || key.isEmpty()) {
                Log.w(TAG, "  [CLE] Clé privée partagée (SL1) non configurée");
            } else {
                Log.d(TAG, "  [CLE] Clé privée partagée (SL1) chargée (" + key.length() + " chars)");
            }
            return key;
        } catch (Exception e) {
            Log.e(TAG, "  [CLE] Erreur lecture clé partagée (SL1)", e);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scan de l'inbox SMS natif (thread de fond)
    // ─────────────────────────────────────────────────────────────────────────

    private void scanNativeInboxForSentryLinkMessages(String personalKeyPem, String unitKeyPem) {
        Log.i(TAG, "┌─────────────────────────────────────────────");
        Log.i(TAG, "│ SCAN INBOX SMS NATIF (SL0 + SL1)");
        Log.i(TAG, "└─────────────────────────────────────────────");

        Cursor cursor = null;
        int total    = 0;
        int imported = 0;
        int skipped  = 0;
        int wrongType = 0;
        int decryptFailed = 0;
        int invalidSig = 0;
        int noKey    = 0;

        try {
            // ── Permission READ_SMS ────────────────────────────────────────────
            if (!hasReadSmsPermission()) {
                Log.e(TAG, "  [PERM] Permission READ_SMS MANQUANTE — scan annulé");
                return;
            }
            Log.d(TAG, "  [PERM] Permission READ_SMS : OK");

            // ── Clés privées disponibles ───────────────────────────────────────
            boolean hasPersonal = personalKeyPem != null && !personalKeyPem.isEmpty();
            boolean hasUnit     = unitKeyPem != null && !unitKeyPem.isEmpty();

            if (!hasPersonal && !hasUnit) {
                Log.e(TAG, "  [CLE] Aucune clé privée configurée — déchiffrement impossible, scan annulé");
                return;
            }

            // ── Chargement des PrivateKey ──────────────────────────────────────
            PrivateKey personalKey = null;
            PrivateKey unitKey = null;
            CryptoManager cryptoManager = new CryptoManager();

            if (hasPersonal) {
                try {
                    personalKey = CryptoManager.stringToPrivateKey(personalKeyPem);
                    Log.i(TAG, "  [CLE] Clé personnelle (SL0) parsée avec succès");
                } catch (Exception e) {
                    Log.e(TAG, "  [CLE] Échec parsing clé personnelle (SL0) — SL0 ignorés", e);
                }
            }
            if (hasUnit) {
                try {
                    unitKey = CryptoManager.stringToPrivateKey(unitKeyPem);
                    Log.i(TAG, "  [CLE] Clé partagée (SL1) parsée avec succès");
                } catch (Exception e) {
                    Log.e(TAG, "  [CLE] Échec parsing clé partagée (SL1) — SL1 ignorés", e);
                }
            }

            SmsDao smsDao = AppDatabase.getInstance(this).smsDao();
            SMSRepository repository = new SMSRepository(this);

            // ── Requête ContentProvider ────────────────────────────────────────
            Uri smsUri = Uri.parse("content://sms/inbox");
            String[] projection = { "address", "body", "date" };
            String selection = "body LIKE ? OR body LIKE ?";
            String[] selectionArgs = {
                MessageConfig.PERSONAL_KEY_PREFIX + "%",
                MessageConfig.SHARED_KEY_PREFIX + "%"
            };

            Log.d(TAG, "  [QUERY] Requête inbox: préfixes SL0='" + MessageConfig.PERSONAL_KEY_PREFIX
                    + "' SL1='" + MessageConfig.SHARED_KEY_PREFIX + "'");

            cursor = getContentResolver().query(smsUri, projection, selection, selectionArgs, "date ASC");

            if (cursor == null) {
                Log.e(TAG, "  [QUERY] ContentResolver a retourné null pour content://sms/inbox");
                return;
            }

            total = cursor.getCount();
            Log.i(TAG, "  [QUERY] " + total + " SMS SentryLink trouvé(s) dans l'inbox natif");

            if (total == 0) {
                Log.i(TAG, "  → Aucun SMS SentryLink à traiter");
                return;
            }

            // ── Traitement de chaque SMS ───────────────────────────────────────
            int index = 0;
            while (cursor.moveToNext()) {
                index++;
                String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String body    = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                long date      = cursor.getLong(cursor.getColumnIndexOrThrow("date"));

                String prefix = MessageHelpers.getMessagePrefix(body);
                Log.d(TAG, "  ┌── SMS [" + index + "/" + total + "] de: " + address
                        + " | préfixe: " + prefix + " | date: " + date);

                if (address == null || body == null) {
                    Log.w(TAG, "  │   address ou body null — SMS ignoré");
                    continue;
                }
                if (!MessageHelpers.isSentryLinkMessage(body)) {
                    Log.w(TAG, "  │   Préfixe non reconnu — ignoré");
                    continue;
                }

                // Anti-doublon
                int count = smsDao.countByAddressAndTimestamp(address, date, SMS_TYPE_INBOX);
                if (count > 0) {
                    Log.d(TAG, "  │   DOUBLON détecté (déjà en DB) — ignoré");
                    skipped++;
                    continue;
                }
                Log.d(TAG, "  │   Pas de doublon — traitement en cours...");

                // Sélection de la clé primaire selon le préfixe, et clé de fallback
                boolean isPrefixSL0 = MessageHelpers.isPersonalKeyMessage(body);
                PrivateKey primaryKey  = isPrefixSL0 ? personalKey : unitKey;
                PrivateKey fallbackKey = isPrefixSL0 ? unitKey     : personalKey;
                String primaryLabel    = isPrefixSL0 ? "SL0 (personnelle)" : "SL1 (partagée)";
                String fallbackLabel   = isPrefixSL0 ? "SL1 (partagée)"   : "SL0 (personnelle)";

                if (primaryKey == null && fallbackKey == null) {
                    Log.w(TAG, "  │   [CLE] Aucune clé disponible — SMS ignoré");
                    noKey++;
                    continue;
                }

                // Déchiffrement avec tentative primaire puis fallback
                String payload = MessageHelpers.getMessageContent(body);
                Log.d(TAG, "  │   [DECRYPT] Payload chiffré: " + payload.length() + " chars");

                String decrypted = null;
                String usedKeyLabel = null;

                if (primaryKey != null) {
                    try {
                        Log.d(TAG, "  │   [DECRYPT] Tentative avec clé " + primaryLabel + "...");
                        decrypted = cryptoManager.receiveSms(payload, primaryKey);
                        usedKeyLabel = primaryLabel;
                        Log.d(TAG, "  │   [DECRYPT] ✓ Succès avec clé " + primaryLabel);
                    } catch (Exception e) {
                        Log.w(TAG, "  │   [DECRYPT] ✗ Échec avec clé " + primaryLabel
                                + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
                        Log.w(TAG, "  │   [DECRYPT]   Cause probable: mauvaise clé (SMS chiffré avant"
                                + " la dernière régénération des clés ?)");
                    }
                }

                if (decrypted == null && fallbackKey != null) {
                    try {
                        Log.d(TAG, "  │   [DECRYPT] Tentative fallback avec clé " + fallbackLabel + "...");
                        decrypted = cryptoManager.receiveSms(payload, fallbackKey);
                        usedKeyLabel = fallbackLabel + " [FALLBACK]";
                        Log.i(TAG, "  │   [DECRYPT] ✓ Succès avec clé fallback " + fallbackLabel
                                + " (préfixe " + prefix + " incohérent avec la clé réellement utilisée)");
                    } catch (Exception e) {
                        Log.e(TAG, "  │   [DECRYPT] ✗ Échec fallback avec clé " + fallbackLabel
                                + " — " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
                    }
                }

                if (decrypted == null) {
                    Log.e(TAG, "  └── ✗ DÉCHIFFREMENT IMPOSSIBLE pour SMS de " + address
                            + " (SL0 et SL1 ont échoué — clés du QR différentes de celles utilisées"
                            + " lors de l'envoi de ce SMS historique)");
                    decryptFailed++;
                    continue;
                }

                try {
                    Log.d(TAG, "  │   [DECRYPT] Texte clair: " + decrypted.length() + " chars"
                            + " | clé utilisée: " + usedKeyLabel);

                    SMSDecryptedDTO dto = new SMSDecryptedDTO(decrypted);

                    if (!dto.isSignatureValid()) {
                        Log.w(TAG, "  │   [PARSE] Format header invalide après déchiffrement — SMS ignoré");
                        Log.w(TAG, "  │   [PARSE]   Texte brut: \"" + decrypted + "\"");
                        invalidSig++;
                        continue;
                    }

                    Log.d(TAG, "  │   [PARSE] type=" + dto.getType()
                            + " | unityId=" + dto.getUnityId()
                            + " | msgTimestamp=" + dto.getTimestamp()
                            + " | message=\"" + dto.getMessage() + "\"");

                    // Filtre type MSG uniquement
                    if (dto.getType() != MessageType.MSG) {
                        Log.i(TAG, "  │   [TYPE] Type " + dto.getType() + " → non stocké dans sms_messages");
                        wrongType++;
                        continue;
                    }

                    // Sauvegarde
                    long insertedId = repository.saveSMS(new SMSMessage(address, dto.getMessage(), date, SMS_TYPE_INBOX));
                    imported++;
                    Log.i(TAG, "  └── ✓ IMPORTÉ (id=" + insertedId + ") de: " + address
                            + " | \"" + dto.getMessage() + "\"");

                } catch (Exception e) {
                    Log.e(TAG, "  └── ✗ Erreur inattendue après déchiffrement pour SMS de " + address, e);
                    decryptFailed++;
                }
            }

            // ── Récapitulatif ──────────────────────────────────────────────────
            Log.i(TAG, "┌─────────────────────────────────────────────");
            Log.i(TAG, "│ SCAN TERMINÉ — RÉCAPITULATIF");
            Log.i(TAG, "│  Total trouvés    : " + total);
            Log.i(TAG, "│  ✓ Importés       : " + imported);
            Log.i(TAG, "│  ↩ Doublons       : " + skipped);
            Log.i(TAG, "│  ≠ Mauvais type   : " + wrongType + " (SOS/LOC/CMD)");
            Log.i(TAG, "│  ✗ Déchiff. échoué: " + decryptFailed);
            Log.i(TAG, "│  ✗ Sig. invalide  : " + invalidSig);
            Log.i(TAG, "│  ✗ Clé manquante  : " + noKey);
            Log.i(TAG, "└─────────────────────────────────────────────");

        } catch (Exception e) {
            Log.e(TAG, "  [FATAL] Erreur inattendue pendant le scan inbox natif", e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private boolean hasReadSmsPermission() {
        return ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification foreground
    // ─────────────────────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SentryLink - Surveillance SMS",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Surveillance des messages SMS chiffrés");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, HomeActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SentryLink actif")
                .setContentText("Surveillance des messages chiffrés en cours...")
                .setSmallIcon(R.drawable.ic_notification_logo)
                .setContentIntent(pi)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "⚠ SmsMonitorService détruit — redémarrage automatique");
        Intent restart = new Intent(this, SmsMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(restart);
        else
            startService(restart);
    }
}
