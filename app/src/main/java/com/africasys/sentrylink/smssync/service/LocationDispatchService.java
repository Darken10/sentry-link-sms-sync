package com.africasys.sentrylink.smssync.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.africasys.sentrylink.smssync.enums.MessageType;
import com.africasys.sentrylink.smssync.models.LocationRecord;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service d'envoi de localisation — canal unique SMS chiffré vers les tours de
 * contrôle.
 *
 * Délègue le chiffrement et l'acheminement à {@link MessageDispatcher}. Le body
 * est au format JSON compact, aligné avec le reste du protocole SentryLink.
 */
public class LocationDispatchService {

    private static final String TAG = "SL-LocationDispatch";

    private final MessageDispatcher messageDispatcher;

    public interface LocationCallback {
        void onLocationSent(String channel);

        void onLocationFailed(String error);
    }

    public LocationDispatchService(Context context) {
        this.messageDispatcher = new MessageDispatcher(context);
    }

    /**
     * Envoie la position courante à toutes les tours de contrôle (CENTRAL_SERVER
     * actifs). Format du message : {unityId};LOC;{timestamp}::{body JSON}
     */
    public void sendLocation(LocationRecord location, LocationCallback callback) {
        Log.i(TAG, "┌─────────────────────────────────────────────");
        Log.i(TAG, "│ ENVOI LOCALISATION");
        Log.i(TAG, "│  Source  : " + location.getSource());
        if ("GPS".equals(location.getSource())) {
            Log.i(TAG, "│  Lat     : " + location.getLatitude());
            Log.i(TAG, "│  Lon     : " + location.getLongitude());
            Log.i(TAG, "│  Précision : " + location.getAccuracy() + " m");
        } else if (location.getCellInfo() != null) {
            Log.i(TAG, "│  CellInfo : " + location.getCellInfo().length() + " chars");
        }
        Log.i(TAG, "└─────────────────────────────────────────────");

        try {
            String body = buildLocationBody(location);
            Log.d(TAG, "  [BUILD] Corps JSON : " + body);

            messageDispatcher.dispatchToControlTower(body, MessageType.LOC, new MessageDispatcher.DispatchCallback() {
                @Override
                public void onSuccess(String channel) {
                    Log.i(TAG, "  [DISPATCH] ✓ Localisation envoyée via " + channel);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onLocationSent(channel));
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "  [DISPATCH] ✗ Échec envoi localisation : " + error);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onLocationFailed(error));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "  [BUILD] ✗ Erreur construction corps localisation", e);
            callback.onLocationFailed("Erreur: " + e.getMessage());
        }
    }

    /**
     * Construit le body JSON compact de la localisation. GPS →
     * {"src":"GPS","lat":...,"lon":...,"acc":...} GSM →
     * {"src":"GSM","cell_towers":[...]}
     */
    private String buildLocationBody(LocationRecord location) throws Exception {
        JSONObject body = new JSONObject();
        body.put("src", location.getSource());

        if ("GPS".equals(location.getSource())) {
            body.put("lat", location.getLatitude());
            body.put("lon", location.getLongitude());
            body.put("acc", location.getAccuracy());
        }

        if (location.getCellInfo() != null) {
            body.put("cell_towers", new JSONArray(location.getCellInfo()));
        }

        return body.toString();
    }
}
