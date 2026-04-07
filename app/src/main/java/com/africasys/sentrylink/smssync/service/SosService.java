package com.africasys.sentrylink.smssync.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.africasys.sentrylink.smssync.enums.MessageType;
import com.africasys.sentrylink.smssync.models.LocationRecord;
import com.africasys.sentrylink.smssync.models.SosAlert;
import com.africasys.sentrylink.smssync.repository.AppDatabase;
import com.africasys.sentrylink.smssync.repository.ConfigRepository;
import com.africasys.sentrylink.smssync.repository.SosDao;

import org.json.JSONObject;

/**
 * Service SOS : envoie une alerte d'urgence avec la localisation à la tour de
 * contrôle via SMS chiffré.
 */
public class SosService {

    private static final String TAG = "SL-SosService";

    private final Context context;
    private final SentryLocationManager locationManager;
    private final MessageDispatcher messageDispatcher;
    private final SosDao sosDao;
    private final ConfigRepository configRepository;
    private Handler silentHandler;
    private boolean silentModeActive = false;

    public interface SosCallback {
        void onSosSent(String channel, SosAlert alert);

        void onSosFailed(String error);
    }

    public SosService(Context context) {
        this.context = context;
        this.locationManager = new SentryLocationManager(context);
        this.messageDispatcher = new MessageDispatcher(context);
        this.sosDao = AppDatabase.getInstance(context).sosDao();
        this.configRepository = ConfigRepository.getInstance(context);
    }

    /**
     * Envoie une alerte SOS avec la position courante.
     */
    public void sendSosAlert(SosCallback callback) {
        String callsign = configRepository.getDeviceCallsign();
        Log.i(TAG, "┌─────────────────────────────────────────────");
        Log.i(TAG, "│ ENVOI ALERTE SOS");
        Log.i(TAG, "│  Callsign : " + callsign);
        Log.i(TAG, "└─────────────────────────────────────────────");

        Log.d(TAG, "  [LOC] Démarrage récupération localisation...");

        locationManager.getCurrentLocation(new SentryLocationManager.LocationResultCallback() {
            @Override
            public void onLocationFound(LocationRecord record) {
                Log.i(TAG, "  [LOC] ✓ GPS — lat=" + record.getLatitude() + " lon=" + record.getLongitude() + " acc="
                        + record.getAccuracy() + "m");
                sendAlertWithLocation(record, callsign, callback);
            }

            @Override
            public void onCellInfoFound(String cellInfoJson) {
                Log.i(TAG, "  [LOC] ✓ GSM (pas de GPS) — info pylônes: "
                        + (cellInfoJson != null ? cellInfoJson.length() + " chars" : "null"));
                LocationRecord record = new LocationRecord(0, 0, 0, "GSM", System.currentTimeMillis());
                record.setCellInfo(cellInfoJson);
                sendAlertWithLocation(record, callsign, callback);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "  [LOC] ✗ Localisation indisponible: " + error + " — envoi SOS sans coordonnées");
                LocationRecord record = new LocationRecord(0, 0, 0, "UNKNOWN", System.currentTimeMillis());
                sendAlertWithLocation(record, callsign, callback);
            }
        });
    }

    private void sendAlertWithLocation(LocationRecord location, String callsign, SosCallback callback) {
        // Ce handler garantit que tous les callbacks UI sont sur le thread principal,
        // peu importe le thread appelant (GPS callback, thread GSM, etc.)
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            Log.d(TAG, "  [BUILD] Construction de l'alerte SOS...");

            SosAlert alert = new SosAlert(location.getLatitude(), location.getLongitude(), location.getSource(),
                    System.currentTimeMillis());
            alert.setCellInfo(location.getCellInfo());

            // Corps SOS — format JSON compact
            JSONObject sosBody = new JSONObject();
            sosBody.put("src", location.getSource());
            if ("GPS".equals(location.getSource())) {
                sosBody.put("lat", location.getLatitude());
                sosBody.put("lon", location.getLongitude());
                sosBody.put("acc", location.getAccuracy());
            } else if ("GSM".equals(location.getSource()) && location.getCellInfo() != null) {
                sosBody.put("cell", location.getCellInfo());
            }
            sosBody.put("msg", "ALERTE SOS - " + callsign + " demande assistance immédiate");
            String messageContent = sosBody.toString();

            Log.d(TAG, "  [BUILD] Corps SOS: " + messageContent);
            alert.setMessage(messageContent);

            // Sauvegarde en DB (statut PENDING)
            long alertId = sosDao.insert(alert);
            alert.setId(alertId);
            Log.i(TAG, "  [DB] ✓ Alerte SOS enregistrée (id=" + alertId + " | statut=PENDING | source="
                    + location.getSource() + ")");

            // Dispatch vers toutes les tours de contrôle CENTRAL_SERVER
            Log.d(TAG, "  [DISPATCH] Envoi vers les tours de contrôle (CENTRAL_SERVER actives)...");

            messageDispatcher.dispatchToControlTower(messageContent, MessageType.SOS,
                    new MessageDispatcher.DispatchCallback() {
                        @Override
                        public void onSuccess(String channel) {
                            alert.setStatus("SENT");
                            alert.setSentVia(channel);
                            sosDao.update(alert);
                            Log.i(TAG, "  [DISPATCH] ✓ SOS envoyé via " + channel + " | statut mis à jour → SENT (id="
                                    + alertId + ")");
                            mainHandler.post(() -> callback.onSosSent(channel, alert));
                        }

                        @Override
                        public void onFailure(String error) {
                            alert.setStatus("FAILED");
                            sosDao.update(alert);
                            Log.e(TAG, "  [DISPATCH] ✗ Échec envoi SOS: " + error + " | statut mis à jour → FAILED (id="
                                    + alertId + ")");
                            mainHandler.post(() -> callback.onSosFailed(error));
                        }
                    });

        } catch (Exception e) {
            // BUG FIX: le catch doit aussi passer par le Handler principal
            // (ce bloc peut s'exécuter sur un thread de fond via locationManager)
            Log.e(TAG, "  [FATAL] Erreur inattendue lors de la construction du SOS", e);
            mainHandler.post(() -> callback.onSosFailed("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Active le mode SOS silencieux : envoi périodique de la position.
     */
    public void startSilentMode() {
        if (silentModeActive)
            return;
        silentModeActive = true;
        silentHandler = new Handler(Looper.getMainLooper());
        long interval = configRepository.getLocationInterval();

        Log.i(TAG, "  [SILENT] Mode SOS silencieux activé — intervalle: " + interval + "ms");

        Runnable silentRunnable = new Runnable() {
            @Override
            public void run() {
                if (!silentModeActive)
                    return;
                Log.d(TAG, "  [SILENT] Envoi SOS silencieux périodique...");
                sendSosAlert(new SosCallback() {
                    @Override
                    public void onSosSent(String channel, SosAlert alert) {
                        Log.d(TAG, "  [SILENT] ✓ SOS silencieux envoyé via " + channel);
                    }

                    @Override
                    public void onSosFailed(String error) {
                        Log.e(TAG, "  [SILENT] ✗ SOS silencieux échoué: " + error);
                    }
                });
                silentHandler.postDelayed(this, interval);
            }
        };
        silentHandler.post(silentRunnable);
    }

    /**
     * Désactive le mode SOS silencieux.
     */
    public void stopSilentMode() {
        silentModeActive = false;
        if (silentHandler != null) {
            silentHandler.removeCallbacksAndMessages(null);
        }
        Log.i(TAG, "  [SILENT] Mode SOS silencieux désactivé");
    }

    public boolean isSilentModeActive() {
        return silentModeActive;
    }
}
