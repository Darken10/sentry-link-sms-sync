package com.africasys.sentrylink.smssync.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Repository pour les paramètres de l'application. Utilise
 * EncryptedSharedPreferences pour un stockage chiffré.
 */
public class ConfigRepository {

    private static final String TAG = "SL-ConfigRepository";
    private static final String PREFS_NAME = "smssync_secure_prefs";

    // Clés de configuration
    public static final String KEY_CONTROL_TOWER_NUMBERS = "control_tower_numbers";
    public static final String KEY_API_BASE_URL = "api_base_url";
    public static final String KEY_LOCATION_INTERVAL = "location_interval_ms";
    public static final String KEY_SOS_SILENT_MODE = "sos_silent_mode";
    public static final String KEY_SIGNING_KEY = "signing_key";
    public static final String KEY_DEVICE_CALLSIGN = "device_callsign";
    public static final String KEY_FIRST_LAUNCH = "first_launch";
    public static final String KEY_AUTH_TOKEN = "auth_token";
    public static final String KEY_PRIVATE_KEY = "private_key";
    public static final String KEY_DEFAULT_PUBLIC_KEY = "default_public_key";
    public static final String KEY_MY_PUBLIC_KEY = "my_public_key";
    public static final String KEY_UNIT_PRIVATE_KEY = "unit_private_key";
    public static final String KEY_UNIT_PUBLIC_KEY = "unit_public_key";

    private static volatile ConfigRepository INSTANCE;
    private SharedPreferences prefs;

    private ConfigRepository(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();

            prefs = EncryptedSharedPreferences.create(context, PREFS_NAME, masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            Log.e(TAG, "Erreur création EncryptedSharedPreferences, fallback normal", e);
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    public static ConfigRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ConfigRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ConfigRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    // --- Numéros de la tour de contrôle ---

    public void setControlTowerNumbers(List<String> numbers) {
        Set<String> set = new HashSet<>(numbers);
        prefs.edit().putStringSet(KEY_CONTROL_TOWER_NUMBERS, set).apply();
    }

    public List<String> getControlTowerNumbers() {
        Set<String> set = prefs.getStringSet(KEY_CONTROL_TOWER_NUMBERS, new HashSet<>());
        return new java.util.ArrayList<>(set);
    }

    // --- URL de l'API ---

    public void setApiBaseUrl(String url) {
        prefs.edit().putString(KEY_API_BASE_URL, url).apply();
    }

    public String getApiBaseUrl() {
        return prefs.getString(KEY_API_BASE_URL, "https://api.sentrylink.mil");
    }

    // --- Intervalle de localisation ---

    public void setLocationInterval(long intervalMs) {
        prefs.edit().putLong(KEY_LOCATION_INTERVAL, intervalMs).apply();
    }

    public long getLocationInterval() {
        return prefs.getLong(KEY_LOCATION_INTERVAL, 60000); // 1 minute par défaut
    }

    // --- Mode SOS silencieux ---

    public void setSosSilentMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOS_SILENT_MODE, enabled).apply();
    }

    public boolean isSosSilentMode() {
        return prefs.getBoolean(KEY_SOS_SILENT_MODE, false);
    }

    // --- Clé de signature ---

    public void setSigningKey(String key) {
        prefs.edit().putString(KEY_SIGNING_KEY, key).apply();
    }

    public String getSigningKey() {
        return prefs.getString(KEY_SIGNING_KEY, "");
    }

    // --- Indicatif de l'appareil ---

    public void setDeviceCallsign(String callsign) {
        prefs.edit().putString(KEY_DEVICE_CALLSIGN, callsign).apply();
    }

    public String getDeviceCallsign() {
        return prefs.getString(KEY_DEVICE_CALLSIGN, "UNIT-000");
    }

    // --- Token d'authentification (depuis QR code) ---

    public void setAuthToken(String token) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }

    public boolean isAuthenticated() {
        String token = getAuthToken();
        return token != null && !token.isEmpty();
    }

    public void clearAuthentication() {
        prefs.edit().remove(KEY_AUTH_TOKEN).remove(KEY_API_BASE_URL).remove(KEY_PRIVATE_KEY).remove(KEY_MY_PUBLIC_KEY)
                .remove(KEY_UNIT_PRIVATE_KEY).remove(KEY_UNIT_PUBLIC_KEY).apply();
    }

    // --- Clé privée RSA-4096 (déchiffrée, stockée dans EncryptedSharedPreferences)
    // ---

    /**
     * Stocke la clé privée RSA-4096 déchiffrée. La valeur est protégée par
     * EncryptedSharedPreferences (AES-256-GCM au repos).
     */
    public void setPrivateKey(String privateKeyPem) {
        prefs.edit().putString(KEY_PRIVATE_KEY, privateKeyPem).apply();
    }

    /**
     * Retourne la clé privée RSA-4096, ou null si elle n'a pas encore été
     * synchronisée.
     */
    public String getPrivateKey() {
        return prefs.getString(KEY_PRIVATE_KEY, null);
    }

    // --- Clé publique par défaut (pour les destinataires hors répertoire) ---

    /**
     * Met en cache la clé publique RSA-4096 par défaut récupérée depuis le backend.
     */
    public void setDefaultPublicKey(String publicKeyPem) {
        prefs.edit().putString(KEY_DEFAULT_PUBLIC_KEY, publicKeyPem).apply();
    }

    /**
     * Retourne la clé publique par défaut en cache, ou null si elle n'a pas encore
     * été récupérée.
     */
    public String getDefaultPublicKey() {
        return prefs.getString(KEY_DEFAULT_PUBLIC_KEY, null);
    }

    // --- Clé publique personnelle (depuis QR code 1) ---

    public void setMyPublicKey(String publicKeyPem) {
        prefs.edit().putString(KEY_MY_PUBLIC_KEY, publicKeyPem).apply();
    }

    public String getMyPublicKey() {
        return prefs.getString(KEY_MY_PUBLIC_KEY, null);
    }

    // --- Clé privée commune à toutes les unités (depuis QR code 2) ---

    public void setUnitPrivateKey(String privateKeyPem) {
        prefs.edit().putString(KEY_UNIT_PRIVATE_KEY, privateKeyPem).apply();
    }

    public String getUnitPrivateKey() {
        return prefs.getString(KEY_UNIT_PRIVATE_KEY, null);
    }

    // --- Clé publique commune à toutes les unités (depuis QR code 2) ---

    public void setUnitPublicKey(String publicKeyPem) {
        prefs.edit().putString(KEY_UNIT_PUBLIC_KEY, publicKeyPem).apply();
    }

    public String getUnitPublicKey() {
        return prefs.getString(KEY_UNIT_PUBLIC_KEY, null);
    }

    // --- Envoi automatique de localisation ---

    public static final String KEY_PERIODIC_LOC_INTERVAL = "periodic_loc_interval_ms";
    public static final String KEY_PERIODIC_LOC_ENABLED = "periodic_loc_enabled";

    public void setPeriodicLocInterval(long intervalMs) {
        prefs.edit().putLong(KEY_PERIODIC_LOC_INTERVAL, intervalMs).apply();
    }

    /** Retourne l'intervalle d'envoi automatique de position (ms). Défaut : 60 min. */
    public long getPeriodicLocInterval() {
        return prefs.getLong(KEY_PERIODIC_LOC_INTERVAL, 3600000L); // 60 min
    }

    public void setPeriodicLocEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PERIODIC_LOC_ENABLED, enabled).apply();
    }

    public boolean isPeriodicLocEnabled() {
        return prefs.getBoolean(KEY_PERIODIC_LOC_ENABLED, false);
    }

    // --- Premier lancement ---

    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }
}
