package com.africasys.sentrylink.smssync.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.africasys.sentrylink.smssync.models.LocationRecord;
import com.africasys.sentrylink.smssync.network.NetworkMonitor;
import com.africasys.sentrylink.smssync.repository.AppDatabase;
import com.africasys.sentrylink.smssync.repository.LocationDao;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Gestionnaire de localisation avec fallback : 1. GPS (FusedLocationProvider)
 * si GPS actif 2. Réseau si Internet disponible 3. Triangulation GSM (infos
 * pylônes) si SIM insérée
 */
public class SentryLocationManager {

    private static final String TAG = "SL-LocationManager";

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final NetworkMonitor networkMonitor;
    private final LocationDao locationDao;
    private LocationCallback locationCallback;

    public interface LocationResultCallback {
        void onLocationFound(LocationRecord record);

        void onCellInfoFound(String cellInfoJson);

        void onError(String error);
    }

    public SentryLocationManager(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.networkMonitor = new NetworkMonitor(context);
        this.locationDao = AppDatabase.getInstance(context).locationDao();
    }

    /** Durée maximale d'attente d'un fix GPS avant de basculer en GSM (ms). */
    private static final long GPS_TIMEOUT_MS = 10_000;

    /**
     * Récupère la position selon la meilleure méthode disponible.
     *
     * <ol>
     *   <li>Si le GPS est désactivé → GSM immédiat.</li>
     *   <li>Si le GPS est activé → dernière position connue, sinon fix frais
     *       avec timeout {@value #GPS_TIMEOUT_MS} ms.</li>
     *   <li>Si GPS échoue ou timeout → triangulation GSM.</li>
     * </ol>
     */
    public void getCurrentLocation(LocationResultCallback callback) {
        Log.d(TAG, "  [LOC] Tentative de récupération de position...");

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "  [LOC] ✗ Permission ACCESS_FINE_LOCATION non accordée");
            callback.onError("Permission de localisation non accordée");
            return;
        }

        // Vérifier si le GPS hardware est activé
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d(TAG, "  [LOC] GPS hardware : " + (gpsEnabled ? "activé" : "désactivé"));

        if (!gpsEnabled) {
            Log.i(TAG, "  [LOC] GPS désactivé — passage direct triangulation GSM");
            fallbackToCellInfo(callback);
            return;
        }

        // GPS activé — essayer la dernière position connue
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Log.i(TAG, "  [LOC] ✓ Dernière position GPS connue — lat=" + location.getLatitude()
                        + " lon=" + location.getLongitude());
                LocationRecord record = new LocationRecord(location.getLatitude(), location.getLongitude(),
                        location.getAccuracy(), "GPS", System.currentTimeMillis());
                locationDao.insert(record);
                callback.onLocationFound(record);
            } else {
                Log.d(TAG, "  [LOC] Pas de position GPS en cache — demande fix frais (timeout "
                        + (GPS_TIMEOUT_MS / 1000) + "s)");
                requestFreshGpsLocation(callback);
            }
        }).addOnFailureListener(e -> {
            Log.w(TAG, "  [LOC] getLastLocation() échoué — fallback GSM", e);
            fallbackToCellInfo(callback);
        });
    }

    /**
     * Demande un fix GPS frais. Si aucun résultat après {@value #GPS_TIMEOUT_MS} ms,
     * bascule automatiquement en triangulation GSM.
     */
    private void requestFreshGpsLocation(LocationResultCallback callback) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fallbackToCellInfo(callback);
            return;
        }

        try {
            LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMaxUpdates(1)
                    .setWaitForAccurateLocation(false)
                    .build();

            // Timeout : si aucun fix GPS dans le délai, on bascule en GSM
            Handler timeoutHandler = new Handler(Looper.getMainLooper());
            Runnable timeoutRunnable = () -> {
                Log.w(TAG, "  [LOC] GPS timeout (" + (GPS_TIMEOUT_MS / 1000)
                        + "s) sans fix — fallback triangulation GSM");
                stopLocationUpdates();
                fallbackToCellInfo(callback);
            };
            timeoutHandler.postDelayed(timeoutRunnable, GPS_TIMEOUT_MS);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    timeoutHandler.removeCallbacks(timeoutRunnable); // annuler le timeout
                    stopLocationUpdates();
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        Log.i(TAG, "  [LOC] ✓ Fix GPS frais — lat=" + location.getLatitude()
                                + " lon=" + location.getLongitude()
                                + " acc=" + location.getAccuracy() + "m");
                        LocationRecord record = new LocationRecord(location.getLatitude(),
                                location.getLongitude(), location.getAccuracy(),
                                "GPS", System.currentTimeMillis());
                        locationDao.insert(record);
                        callback.onLocationFound(record);
                    } else {
                        Log.w(TAG, "  [LOC] Fix GPS vide — fallback GSM");
                        fallbackToCellInfo(callback);
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());

        } catch (SecurityException e) {
            Log.e(TAG, "  [LOC] Erreur permission GPS", e);
            fallbackToCellInfo(callback);
        }
    }

    /**
     * Fallback : triangulation GSM via les informations des relais radio visibles.
     */
    private void fallbackToCellInfo(LocationResultCallback callback) {
        Log.d(TAG, "  [LOC] Triangulation GSM — scan des relais radio...");
        String cellJson = getCellTowersJson();
        if (cellJson != null && !cellJson.equals("[]")) {
            Log.i(TAG, "  [LOC] ✓ Relais GSM détectés — " + cellJson.length() + " chars");
            LocationRecord record = new LocationRecord(0, 0, 0, "GSM", System.currentTimeMillis());
            record.setCellInfo(cellJson);
            locationDao.insert(record);
            callback.onCellInfoFound(cellJson);
        } else {
            Log.e(TAG, "  [LOC] ✗ Aucune source de position disponible (ni GPS ni GSM)");
            callback.onError("Aucune source de localisation disponible");
        }
    }

    /**
     * Récupère les informations de tous les pylônes visibles en format JSON.
     */
    public String getCellTowersJson() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            List<CellInfo> cellInfoList = tm.getAllCellInfo();

            if (cellInfoList == null || cellInfoList.isEmpty())
                return null;

            JSONArray towers = new JSONArray();

            for (CellInfo info : cellInfoList) {
                JSONObject tower = new JSONObject();

                if (info instanceof CellInfoLte) {
                    CellInfoLte lte = (CellInfoLte) info;
                    CellIdentityLte identity = lte.getCellIdentity();
                    CellSignalStrengthLte signal = lte.getCellSignalStrength();

                    tower.put("type", "LTE");
                    tower.put("cid", identity.getCi());
                    tower.put("mcc", identity.getMccString());
                    tower.put("mnc", identity.getMncString());
                    tower.put("tac", identity.getTac());
                    tower.put("rssi", signal.getDbm());
                    tower.put("registered", lte.isRegistered());

                } else if (info instanceof CellInfoGsm) {
                    CellInfoGsm gsm = (CellInfoGsm) info;
                    CellIdentityGsm identity = gsm.getCellIdentity();
                    CellSignalStrengthGsm signal = gsm.getCellSignalStrength();

                    tower.put("type", "GSM");
                    tower.put("cid", identity.getCid());
                    tower.put("mcc", identity.getMccString());
                    tower.put("mnc", identity.getMncString());
                    tower.put("lac", identity.getLac());
                    tower.put("rssi", signal.getDbm());
                    tower.put("registered", gsm.isRegistered());

                } else if (info instanceof CellInfoWcdma) {
                    CellInfoWcdma wcdma = (CellInfoWcdma) info;
                    CellIdentityWcdma identity = wcdma.getCellIdentity();
                    CellSignalStrengthWcdma signal = wcdma.getCellSignalStrength();

                    tower.put("type", "WCDMA");
                    tower.put("cid", identity.getCid());
                    tower.put("mcc", identity.getMccString());
                    tower.put("mnc", identity.getMncString());
                    tower.put("lac", identity.getLac());
                    tower.put("rssi", signal.getDbm());
                    tower.put("registered", wcdma.isRegistered());

                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info instanceof CellInfoNr) {
                    CellInfoNr nr = (CellInfoNr) info;
                    CellIdentityNr identity = (CellIdentityNr) nr.getCellIdentity();
                    CellSignalStrengthNr signal = (CellSignalStrengthNr) nr.getCellSignalStrength();

                    tower.put("type", "5G_NR");
                    tower.put("mcc", identity.getMccString());
                    tower.put("mnc", identity.getMncString());
                    tower.put("rssi", signal.getDbm());
                    tower.put("registered", nr.isRegistered());
                }

                if (tower.length() > 0) {
                    towers.put(tower);
                }
            }

            return towers.toString();

        } catch (Exception e) {
            Log.e(TAG, "Erreur récupération pylônes", e);
            return null;
        }
    }

    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }
}
