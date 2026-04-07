package com.africasys.sentrylink.smssync.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.africasys.sentrylink.smssync.HomeActivity;
import com.africasys.sentrylink.smssync.R;
import com.africasys.sentrylink.smssync.repository.ConfigRepository;
import com.africasys.sentrylink.smssync.server.SmsHttpServer;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * Service foreground hébergeant la passerelle HTTP SMS.
 *
 * <p>Responsabilités :
 * <ol>
 *   <li>Génère un token API aléatoire sécurisé au premier démarrage.</li>
 *   <li>Lance {@link SmsHttpServer} sur le port configuré (défaut 8080).</li>
 *   <li>Acquiert un {@code WifiLock} (LOW_LATENCY) pour maintenir le Wi-Fi actif
 *       même écran éteint.</li>
 *   <li>Acquiert un {@code WakeLock} (PARTIAL) pour maintenir le CPU actif.</li>
 *   <li>Affiche une notification persistante avec l'adresse d'accès.</li>
 *   <li>Retourne {@code START_STICKY} : le système redémarre automatiquement
 *       le service s'il est tué.</li>
 * </ol>
 *
 * <p>L'arrêt volontaire (depuis les paramètres) doit d'abord désactiver le flag
 * {@code http_gw_enabled} pour éviter la boucle de redémarrage au boot.
 */
public class HttpGatewayService extends Service {

    private static final String TAG = "SL-HttpGateway";
    private static final String CHANNEL_ID   = "sentry_link_http_gw";
    private static final int    NOTIFICATION_ID = 1002;

    /** Accessible par SettingsActivity pour connaître l'état courant. */
    public static volatile boolean isRunning = false;

    private SmsHttpServer           httpServer;
    private PowerManager.WakeLock   wakeLock;
    private WifiManager.WifiLock    wifiLock;

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        createNotificationChannel();
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.i(TAG, "  HttpGatewayService créé");
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "▶ HttpGatewayService démarré (startId=" + startId + ")");
        startForeground(NOTIFICATION_ID, buildNotification("Démarrage de la passerelle..."));

        ConfigRepository config = ConfigRepository.getInstance(this);

        // ── Génération automatique du token si absent ──────────────────────
        String token = config.getHttpApiToken();
        if (token == null || token.isEmpty()) {
            token = generateSecureToken();
            config.setHttpApiToken(token);
            Log.i(TAG, "  → Token API généré: " + token);
        }

        int port = config.getHttpServerPort();

        // ── WakeLock : maintient le CPU éveillé ────────────────────────────
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SentryLink:HttpGateway");
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.d(TAG, "  → WakeLock acquis");
        }

        // ── WifiLock : maintient le Wi-Fi actif en pleine performance ──────
        WifiManager wm = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        int lockMode = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ? WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                : WifiManager.WIFI_MODE_FULL_HIGH_PERF;
        wifiLock = wm.createWifiLock(lockMode, "SentryLink:HttpGateway");
        if (!wifiLock.isHeld()) {
            wifiLock.acquire();
            Log.d(TAG, "  → WifiLock acquis (mode=" + lockMode + ")");
        }

        // ── Démarrage du serveur HTTP ──────────────────────────────────────
        if (httpServer != null && httpServer.isAlive()) {
            Log.w(TAG, "  → Serveur déjà actif — démarrage ignoré");
        } else {
            httpServer = new SmsHttpServer(this, port, token);
            try {
                httpServer.start();
                String address = "http://" + getWifiIpAddress() + ":" + port;
                Log.i(TAG, "✓ Serveur HTTP actif sur " + address);
                startForeground(NOTIFICATION_ID, buildNotification(address + "/send-sms"));
            } catch (IOException e) {
                Log.e(TAG, "✗ Impossible de démarrer le serveur HTTP sur le port " + port, e);
                startForeground(NOTIFICATION_ID,
                        buildNotification("ERREUR — port " + port + " occupé"));
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();

        if (httpServer != null && httpServer.isAlive()) {
            httpServer.stop();
            Log.i(TAG, "Serveur HTTP arrêté");
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "WakeLock libéré");
        }
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
            Log.d(TAG, "WifiLock libéré");
        }
        Log.w(TAG, "HttpGatewayService détruit");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilitaires
    // ─────────────────────────────────────────────────────────────────────────

    /** Génère un token hex 32 caractères (128 bits) cryptographiquement sûr. */
    private String generateSecureToken() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** Retourne l'adresse IPv4 Wi-Fi du téléphone (ex: 192.168.1.42). */
    @SuppressWarnings("deprecation")
    public static String getWifiIpAddress(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            int ip = wm.getConnectionInfo().getIpAddress();
            if (ip == 0) return "0.0.0.0";
            return (ip & 0xFF) + "."
                    + ((ip >> 8)  & 0xFF) + "."
                    + ((ip >> 16) & 0xFF) + "."
                    + ((ip >> 24) & 0xFF);
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

    private String getWifiIpAddress() {
        return getWifiIpAddress(this);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification
    // ─────────────────────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SentryLink — Passerelle HTTP",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Serveur HTTP local d'envoi de SMS");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, HomeActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SentryLink — HTTP Gateway")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification_logo)
                .setContentIntent(pi)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
