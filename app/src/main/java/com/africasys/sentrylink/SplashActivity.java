package com.africasys.sentrylink;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.africasys.sentrylink.crypto.CryptoStrategy;
import com.africasys.sentrylink.dtos.ContactDto;
import com.africasys.sentrylink.dtos.PrivateKeyResponse;
import com.africasys.sentrylink.network.ApiClient;
import com.africasys.sentrylink.network.NetworkMonitor;
import com.africasys.sentrylink.repository.ConfigRepository;
import com.africasys.sentrylink.repository.ContactRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

/**
 * Écran de démarrage SentryLink.
 *
 * Si une connexion internet est disponible, deux tâches E2E sont lancées en
 * arrière-plan dès l'affichage de l'écran :
 * <ol>
 * <li>Synchronisation des contacts (GET /api/v1/contacts) — upsert en base
 * locale.</li>
 * <li>Récupération + déchiffrement de la clé privée RSA-4096 (GET
 * /api/v1/my-private-key).</li>
 * </ol>
 * Ces tâches sont asynchrones et ne bloquent pas la navigation vers
 * HomeActivity.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SL-SplashActivity";
    private static final long SPLASH_DURATION = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mode plein écran (API 30+)
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        setContentView(R.layout.activity_splash);

        // Animation de fondu sur le logo
        ImageView logo = findViewById(R.id.splashLogo);
        if (logo != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(1000);
            fadeIn.setFillAfter(true);
            logo.startAnimation(fadeIn);
        }

        // Démarrage des tâches E2E en arrière-plan si internet disponible
        if (new NetworkMonitor(this).isInternetAvailable()) {
            startE2eSyncInBackground(getApplicationContext());
        }

        // Navigation vers HomeActivity après la durée du splash
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            finish();
        }, SPLASH_DURATION);
    }

    // -------------------------------------------------------------------------
    // Synchronisation E2E en arrière-plan
    // -------------------------------------------------------------------------

    private static void startE2eSyncInBackground(Context context) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                syncContacts(context);
                // syncPrivateKey(context);
            } catch (Exception e) {
                Log.e(TAG, "Erreur inattendue lors de la sync E2E", e);
            }
        });
        executor.shutdown();
    }

    /**
     * GET /api/v1/contacts → upsert dans la base locale. Garantit l'absence de
     * doublons via {@link ContactRepository#syncFromApi}.
     */
    private static void syncContacts(Context context) {
        try {
            String credential = ConfigRepository.getInstance(context).getAuthToken();
            if (credential == null || credential.isEmpty())
                return;

            Response<List<ContactDto>> response = ApiClient.getInstance(context).getApi().getContacts(credential)
                    .execute();

            if (response.isSuccessful() && response.body() != null) {
                List<ContactDto> contacts = response.body();
                ContactRepository.getInstance(context).syncFromApi(contacts);
                Log.d(TAG, "Contacts synchronisés : " + contacts.size());
            } else {
                Log.w(TAG, "Sync contacts — réponse inattendue : HTTP " + response.code());
            }
        } catch (Exception e) {
            Log.w(TAG, "Sync contacts échouée (réseau ?)", e);
        }
    }

    /**
     * GET /api/v1/my-private-key → déchiffrement AES-256-GCM → stockage local
     * chiffré. Schéma : clé AES = SHA-256(credential), format =
     * base64url(iv):base64url(ciphertext+tag). Réponse 204 = aucune clé active,
     * ignorée silencieusement.
     */
    /**
     * private static void syncPrivateKey(Context context) { try { String credential
     * = ConfigRepository.getInstance(context).getAuthToken(); if (credential ==
     * null || credential.isEmpty()) return;
     * 
     * Response<PrivateKeyResponse> response =
     * ApiClient.getInstance(context).getApi().getMyPrivateKey(credential)
     * .execute();
     * 
     * if (response.code() == 204) { Log.d(TAG, "Aucune clé privée active sur le
     * serveur (204)"); return; }
     * 
     * if (response.isSuccessful() && response.body() != null) { String encryptedKey
     * = response.body().encryptedPrivateKey; if (encryptedKey != null &&
     * !encryptedKey.isEmpty()) { // Déchiffrer avec AES-256-GCM (clé =
     * SHA-256(credential)) CryptoStrategy CryptoManager = new CryptoStrategy();
     * String privateKeyPem = CryptoManager.decrypt( credential);
     * ConfigRepository.getInstance(context).setPrivateKey(privateKeyPem);
     * Log.d(TAG, "Clé privée déchiffrée et stockée"); } } else { Log.w(TAG, "Sync
     * clé privée — réponse inattendue : HTTP " + response.code()); } } catch
     * (Exception e) { Log.w(TAG, "Sync clé privée échouée", e); } }
     */
}
