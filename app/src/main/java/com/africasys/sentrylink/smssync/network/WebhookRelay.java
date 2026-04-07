package com.africasys.sentrylink.smssync.network;

import android.content.Context;
import android.util.Log;

import com.africasys.sentrylink.smssync.repository.ConfigRepository;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Relaie les messages SMS déchiffrés vers un serveur web (webhook).
 *
 * <p>Comportement :
 * <ol>
 *   <li>Construit un payload JSON avec les données du message déchiffré.</li>
 *   <li>Envoie une requête HTTP POST vers l'URL webhook configurée.</li>
 *   <li>En cas d'échec, relance jusqu'à {@value MAX_RETRIES} fois avec un
 *       délai exponentiel (2 s → 5 s → 10 s).</li>
 *   <li>L'appel est entièrement non-bloquant : il s'exécute sur un thread de
 *       fond indépendant, sans impacter le BroadcastReceiver.</li>
 * </ol>
 *
 * <p>Format du payload JSON envoyé :
 * <pre>
 * {
 *   "sender"    : "+33612345678",
 *   "message"   : "Texte déchiffré",
 *   "timestamp" : 1712000000000,
 *   "prefix"    : "SL0",
 *   "device"    : "UNIT-042"
 * }
 * </pre>
 */
public class WebhookRelay {

    private static final String TAG = "SL-WebhookRelay";
    private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {2_000L, 5_000L, 10_000L};

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    /**
     * Relaie un message SMS déchiffré vers le webhook configuré.
     * Non-bloquant : lancé sur un thread de fond.
     *
     * @param context   Contexte Android
     * @param sender    Numéro de téléphone de l'expéditeur
     * @param message   Contenu déchiffré du message
     * @param timestamp Horodatage du SMS (ms depuis epoch)
     * @param prefix    Préfixe SentryLink utilisé ({@code SL0} ou {@code SL1})
     */
    public static void relay(Context context, String sender, String message,
                             long timestamp, String prefix) {
        ConfigRepository config = ConfigRepository.getInstance(context);
        String webhookUrl = config.getWebhookUrl();

        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            Log.d(TAG, "Webhook non configuré — relais ignoré pour: " + sender);
            return;
        }

        String device = config.getDeviceCallsign();
        final String url = webhookUrl.trim();

        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("sender", sender);
                payload.put("message", message);
                payload.put("timestamp", timestamp);
                payload.put("prefix", prefix);
                payload.put("device", device);
                postWithRetry(url, payload.toString());
            } catch (Exception e) {
                Log.e(TAG, "Erreur construction payload webhook pour: " + sender, e);
            }
        }, "SL-WebhookRelay").start();
    }

    private static void postWithRetry(String url, String jsonPayload) {
        RequestBody body = RequestBody.create(jsonPayload, MEDIA_TYPE_JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "SentryLink-SMSSync/1.0")
                .build();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            Log.d(TAG, "┌── Envoi webhook (tentative " + attempt + "/" + MAX_RETRIES + ") → " + url);
            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "└── ✓ Webhook relayé avec succès (HTTP " + response.code() + ")");
                    return;
                }
                Log.w(TAG, "└── ✗ Réponse HTTP " + response.code() + " — tentative " + attempt);
            } catch (IOException e) {
                Log.w(TAG, "└── ✗ I/O error (tentative " + attempt + "): " + e.getMessage());
            }

            if (attempt < MAX_RETRIES) {
                long delay = RETRY_DELAYS_MS[attempt - 1];
                Log.d(TAG, "    Prochaine tentative dans " + (delay / 1000) + "s...");
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    Log.w(TAG, "Relais webhook interrompu avant la tentative " + (attempt + 1));
                    return;
                }
            }
        }

        Log.e(TAG, "✗ Webhook définitivement échoué après " + MAX_RETRIES + " tentatives → " + url);
    }
}
