package com.africasys.sentrylink;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.africasys.sentrylink.models.LocationRecord;
import com.africasys.sentrylink.repository.ConfigRepository;
import com.africasys.sentrylink.service.LocationDispatchService;
import com.africasys.sentrylink.service.SentryLocationManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Page de localisation : détection automatique GPS puis fallback GSM.
 *
 * <p>À l'ouverture, la position est récupérée automatiquement.
 * Le bouton "Actualiser" (icône sync dans la toolbar) permet un nouveau scan.
 * Un toggle permet d'activer l'envoi périodique à la tour de contrôle.
 */
public class LocationActivity extends AppCompatActivity {

    private static final String TAG = "SL-LocationActivity";

    private SentryLocationManager locationManager;
    private LocationDispatchService locationDispatchService;
    private ConfigRepository configRepository;

    // UI
    private ProgressBar locationProgress;
    private TextView locationStatus;
    private TextView tvSourceBadge;
    private LinearLayout gpsInfoLayout;
    private TextView latitudeText;
    private TextView longitudeText;
    private TextView accuracyText;
    private TextView sourceText;
    private TextView tvLastUpdate;
    private MaterialCardView cellInfoCard;
    private TextView cellInfoText;
    private TextView tvCellCount;
    private MaterialButton btnSendLocation;
    private SwitchMaterial switchPeriodicLoc;
    private TextView tvPeriodicInterval;

    // State
    private LocationRecord currentLocation;
    private String currentCellInfo;

    // Periodic send
    private Handler periodicHandler;
    private Runnable periodicRunnable;
    private boolean periodicActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        locationManager = new SentryLocationManager(this);
        locationDispatchService = new LocationDispatchService(this);
        configRepository = ConfigRepository.getInstance(this);
        periodicHandler = new Handler(Looper.getMainLooper());

        initializeUI();
        loadPeriodicState();
        getLocation(); // auto-détection à l'ouverture
    }

    private void initializeUI() {
        locationProgress = findViewById(R.id.locationProgress);
        locationStatus = findViewById(R.id.locationStatus);
        tvSourceBadge = findViewById(R.id.tvSourceBadge);
        gpsInfoLayout = findViewById(R.id.gpsInfoLayout);
        latitudeText = findViewById(R.id.latitudeText);
        longitudeText = findViewById(R.id.longitudeText);
        accuracyText = findViewById(R.id.accuracyText);
        sourceText = findViewById(R.id.sourceText);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);
        cellInfoCard = findViewById(R.id.cellInfoCard);
        cellInfoText = findViewById(R.id.cellInfoText);
        tvCellCount = findViewById(R.id.tvCellCount);
        btnSendLocation = findViewById(R.id.btnSendLocation);
        switchPeriodicLoc = findViewById(R.id.switchPeriodicLoc);
        tvPeriodicInterval = findViewById(R.id.tvPeriodicInterval);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRefresh).setOnClickListener(v -> getLocation());
        btnSendLocation.setOnClickListener(v -> sendLocationToControlTower());

        // Toggle envoi automatique
        switchPeriodicLoc.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // seulement si c'est l'utilisateur qui toggle
                configRepository.setPeriodicLocEnabled(isChecked);
                if (isChecked) {
                    startPeriodicSend();
                    Toast.makeText(this, "Envoi automatique activé", Toast.LENGTH_SHORT).show();
                } else {
                    stopPeriodicSend();
                    Toast.makeText(this, "Envoi automatique désactivé", Toast.LENGTH_SHORT).show();
                }
            }
        });

        updateIntervalLabel();
    }

    /** Charge et applique le dernier état du toggle depuis la config. */
    private void loadPeriodicState() {
        boolean enabled = configRepository.isPeriodicLocEnabled();
        switchPeriodicLoc.setChecked(enabled);
        if (enabled) {
            startPeriodicSend();
        }
    }

    /** Met à jour le label "Toutes les X min" sous le toggle. */
    private void updateIntervalLabel() {
        long intervalMs = configRepository.getPeriodicLocInterval();
        long minutes = intervalMs / 60000;
        tvPeriodicInterval.setText(String.format(getString(R.string.loc_periodic_every), minutes));
    }

    // -------------------------------------------------------------------------
    // Récupération de position
    // -------------------------------------------------------------------------

    private void getLocation() {
        Log.i(TAG, "┌─────────────────────────────────────────────");
        Log.i(TAG, "│ RÉCUPÉRATION POSITION");
        Log.i(TAG, "└─────────────────────────────────────────────");
        showProgress(true);
        locationStatus.setText(R.string.location_searching);
        gpsInfoLayout.setVisibility(View.GONE);
        cellInfoCard.setVisibility(View.GONE);
        tvLastUpdate.setVisibility(View.GONE);
        tvSourceBadge.setText("--");

        locationManager.getCurrentLocation(new SentryLocationManager.LocationResultCallback() {
            @Override
            public void onLocationFound(LocationRecord record) {
                Log.i(TAG, "  [LOC] ✓ GPS — lat=" + record.getLatitude()
                        + " | lon=" + record.getLongitude()
                        + " | précision=" + record.getAccuracy() + " m");
                runOnUiThread(() -> {
                    currentLocation = record;
                    currentCellInfo = null;

                    showProgress(false);
                    btnSendLocation.setEnabled(true);
                    tvSourceBadge.setText("GPS");
                    locationStatus.setText(R.string.location_gps);

                    gpsInfoLayout.setVisibility(View.VISIBLE);
                    latitudeText.setText(String.format(Locale.US, "%.6f", record.getLatitude()));
                    longitudeText.setText(String.format(Locale.US, "%.6f", record.getLongitude()));
                    accuracyText.setText(String.format(getString(R.string.location_accuracy), record.getAccuracy()));
                    sourceText.setText("GPS");
                    showLastUpdate();

                    // Afficher aussi les pylônes GSM en complément
                    showCellInfo();
                });
            }

            @Override
            public void onCellInfoFound(String cellInfoJson) {
                Log.i(TAG, "  [LOC] ✓ GSM (pas de GPS) — cellInfo: "
                        + (cellInfoJson != null ? cellInfoJson.length() + " chars" : "null"));
                runOnUiThread(() -> {
                    currentCellInfo = cellInfoJson;
                    currentLocation = null;

                    showProgress(false);
                    btnSendLocation.setEnabled(true);
                    tvSourceBadge.setText("GSM");
                    locationStatus.setText(R.string.location_gsm);
                    gpsInfoLayout.setVisibility(View.GONE);
                    displayCellInfo(cellInfoJson);
                    showLastUpdate();
                });
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "  [LOC] ✗ Localisation indisponible : " + error);
                runOnUiThread(() -> {
                    showProgress(false);
                    tvSourceBadge.setText("N/A");
                    locationStatus.setText(R.string.location_none);
                    Toast.makeText(LocationActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showProgress(boolean show) {
        locationProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            btnSendLocation.setEnabled(false);
        }
    }

    private void showCellInfo() {
        String cellJson = locationManager.getCellTowersJson();
        if (cellJson != null) {
            currentCellInfo = cellJson;
            displayCellInfo(cellJson);
        }
    }

    private void displayCellInfo(String cellJson) {
        try {
            JSONArray towers = new JSONArray(cellJson);
            if (towers.length() == 0) return;

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < towers.length(); i++) {
                JSONObject t = towers.getJSONObject(i);
                String type = t.optString("type", "?");
                sb.append(type);
                if (t.has("cid"))  sb.append("  CID:").append(t.getInt("cid"));
                if (t.has("mcc"))  sb.append("  MCC:").append(t.optString("mcc"));
                if (t.has("mnc"))  sb.append("  MNC:").append(t.optString("mnc"));
                if (t.has("tac"))  sb.append("  TAC:").append(t.getInt("tac"));
                if (t.has("lac"))  sb.append("  LAC:").append(t.getInt("lac"));
                if (t.has("rssi")) sb.append("  ").append(t.getInt("rssi")).append(" dBm");
                if (t.optBoolean("registered")) sb.append("  [REG]");
                sb.append("\n");
            }

            tvCellCount.setText(towers.length() + " relai(s)");
            cellInfoText.setText(sb.toString().trim());
            cellInfoCard.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            cellInfoCard.setVisibility(View.GONE);
        }
    }

    private void showLastUpdate() {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        tvLastUpdate.setText("Mis à jour : " + time);
        tvLastUpdate.setVisibility(View.VISIBLE);
    }

    // -------------------------------------------------------------------------
    // Envoi de position
    // -------------------------------------------------------------------------

    private void sendLocationToControlTower() {
        LocationRecord location = buildLocationRecord();
        if (location == null) {
            Log.w(TAG, "  [SEND] Aucune position disponible — annulé");
            Toast.makeText(this, "Pas de position à envoyer", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG, "  [SEND] Envoi manuel → source=" + location.getSource());
        btnSendLocation.setEnabled(false);
        locationDispatchService.sendLocation(location, new LocationDispatchService.LocationCallback() {
            @Override
            public void onLocationSent(String channel) {
                Log.i(TAG, "  [SEND] ✓ Position envoyée via " + channel);
                runOnUiThread(() -> {
                    btnSendLocation.setEnabled(true);
                    Toast.makeText(LocationActivity.this,
                            "Position envoyée via " + channel, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onLocationFailed(String error) {
                Log.e(TAG, "  [SEND] ✗ Échec envoi position : " + error);
                runOnUiThread(() -> {
                    btnSendLocation.setEnabled(true);
                    Toast.makeText(LocationActivity.this,
                            "Échec envoi : " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private LocationRecord buildLocationRecord() {
        if (currentLocation != null) {
            return currentLocation;
        } else if (currentCellInfo != null) {
            LocationRecord gsmRecord = new LocationRecord(0, 0, 0, "GSM", System.currentTimeMillis());
            gsmRecord.setCellInfo(currentCellInfo);
            return gsmRecord;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Envoi périodique
    // -------------------------------------------------------------------------

    private void startPeriodicSend() {
        if (periodicActive) return;
        periodicActive = true;
        long intervalMs = configRepository.getPeriodicLocInterval();
        Log.i(TAG, "  [PERIODIC] Envoi automatique activé — intervalle: " + (intervalMs / 60000) + " min"
                + " (" + intervalMs + " ms)");

        periodicRunnable = new Runnable() {
            @Override
            public void run() {
                if (!periodicActive) return;
                Log.d(TAG, "  [PERIODIC] Déclenchement envoi automatique...");
                sendPeriodicLocation();
                periodicHandler.postDelayed(this, intervalMs);
            }
        };
        periodicHandler.postDelayed(periodicRunnable, intervalMs);
    }

    private void stopPeriodicSend() {
        periodicActive = false;
        if (periodicRunnable != null) {
            periodicHandler.removeCallbacks(periodicRunnable);
        }
        Log.i(TAG, "  [PERIODIC] Envoi automatique désactivé");
    }

    private void sendPeriodicLocation() {
        locationManager.getCurrentLocation(new SentryLocationManager.LocationResultCallback() {
            @Override
            public void onLocationFound(LocationRecord record) {
                Log.d(TAG, "  [PERIODIC] GPS — lat=" + record.getLatitude()
                        + " lon=" + record.getLongitude() + " → envoi LOC...");
                locationDispatchService.sendLocation(record, new LocationDispatchService.LocationCallback() {
                    @Override
                    public void onLocationSent(String channel) {
                        Log.i(TAG, "  [PERIODIC] ✓ LOC GPS envoyé via " + channel);
                    }
                    @Override
                    public void onLocationFailed(String error) {
                        Log.w(TAG, "  [PERIODIC] ✗ Échec envoi LOC GPS : " + error);
                    }
                });
            }

            @Override
            public void onCellInfoFound(String cellInfoJson) {
                Log.d(TAG, "  [PERIODIC] GSM — " + (cellInfoJson != null ? cellInfoJson.length() + " chars" : "null")
                        + " → envoi LOC...");
                LocationRecord gsm = new LocationRecord(0, 0, 0, "GSM", System.currentTimeMillis());
                gsm.setCellInfo(cellInfoJson);
                locationDispatchService.sendLocation(gsm, new LocationDispatchService.LocationCallback() {
                    @Override
                    public void onLocationSent(String channel) {
                        Log.i(TAG, "  [PERIODIC] ✓ LOC GSM envoyé via " + channel);
                    }
                    @Override
                    public void onLocationFailed(String error) {
                        Log.w(TAG, "  [PERIODIC] ✗ Échec envoi LOC GSM : " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "  [PERIODIC] ✗ Pas de position disponible : " + error);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();
        updateIntervalLabel(); // rafraîchit l'intervalle si modifié dans les paramètres
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.stopLocationUpdates();
        stopPeriodicSend();
    }
}
