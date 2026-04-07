package com.africasys.sentrylink.smssync;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.africasys.sentrylink.smssync.config.QRCodeConfig;
import com.africasys.sentrylink.smssync.dtos.ContactAuthResponse;
import com.africasys.sentrylink.smssync.network.ApiClient;
import com.africasys.sentrylink.smssync.repository.ConfigRepository;
import com.africasys.sentrylink.smssync.repository.UserRepository;
import com.africasys.sentrylink.smssync.service.ContactService;
import com.google.android.material.button.MaterialButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Écran d'authentification — wizard 3 étapes.
 *
 * Étape 1 : QR code des clés personnelles → format
 * "SL::PUBLIC_KEY::PRIVATE_KEY" Étape 2 : QR code des clés communes unités →
 * format "SL::UNIT_PUBLIC_KEY::UNIT_PRIVATE_KEY" Étape 3 : QR code URL de
 * communication → format "BASE_URL"
 *
 * Un bip sonore est émis après chaque scan réussi. L'appel API
 * d'authentification est effectué à la fin de l'étape 3.
 */
public class QrScanActivity extends AppCompatActivity {

    private static final String TAG = "SL-QrScanActivity";

    private int currentStep = 1;

    // Vues principales
    private View viewWizard;
    private View viewNoInternet;
    private View viewLoading;

    // Éléments UI du wizard
    private TextView stepDot1;
    private TextView stepDot2;
    private TextView stepDot3;
    private TextView tvStepLabel;
    private TextView tvStepTitle;

    // Sons
    private SoundPool soundPool;
    private int soundSuccess = -1;
    private int soundError = -1;

    // Éléments UI de l'écran de chargement
    private ProgressBar pbAuthStep1, pbAuthStep2, pbAuthStep3, pbAuthStep4, pbAuthStep5, pbAuthStep6;
    private TextView tvAuthStep1Icon, tvAuthStep2Icon, tvAuthStep3Icon, tvAuthStep4Icon, tvAuthStep5Icon,
            tvAuthStep6Icon;
    private TextView tvAuthStep1Status, tvAuthStep2Status, tvAuthStep3Status, tvAuthStep4Status, tvAuthStep5Status,
            tvAuthStep6Status;
    private ProgressBar pbAuthOverall;
    private View viewAuthResult;
    private TextView tvAuthResultIcon, tvAuthResultMessage;
    private MaterialButton btnAuthRetry;

    private final String QR_SEPARATOR = QRCodeConfig.QR_SEPARATOR;
    private String user_public_key_value = "";

    private final ActivityResultLauncher<ScanOptions> scanLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    dispatchScanResult(result.getContents());
                } else {
                    showWizardView();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        // Vues principales
        viewWizard = findViewById(R.id.viewWizard);
        viewNoInternet = findViewById(R.id.viewNoInternet);
        viewLoading = findViewById(R.id.viewLoading);

        // Wizard
        stepDot1 = findViewById(R.id.stepDot1);
        stepDot2 = findViewById(R.id.stepDot2);
        stepDot3 = findViewById(R.id.stepDot3);
        tvStepLabel = findViewById(R.id.tvStepLabel);
        tvStepTitle = findViewById(R.id.tvStepTitle);

        ((MaterialButton) findViewById(R.id.btnScan)).setOnClickListener(v -> openScanner());
        ((MaterialButton) findViewById(R.id.btnRetry)).setOnClickListener(v -> showWizardView());

        // Écran de chargement
        pbAuthStep1 = findViewById(R.id.pbAuthStep1);
        pbAuthStep2 = findViewById(R.id.pbAuthStep2);
        pbAuthStep3 = findViewById(R.id.pbAuthStep3);
        pbAuthStep4 = findViewById(R.id.pbAuthStep4);
        pbAuthStep5 = findViewById(R.id.pbAuthStep5);
        pbAuthStep6 = findViewById(R.id.pbAuthStep6);
        tvAuthStep1Icon = findViewById(R.id.tvAuthStep1Icon);
        tvAuthStep2Icon = findViewById(R.id.tvAuthStep2Icon);
        tvAuthStep3Icon = findViewById(R.id.tvAuthStep3Icon);
        tvAuthStep4Icon = findViewById(R.id.tvAuthStep4Icon);
        tvAuthStep5Icon = findViewById(R.id.tvAuthStep5Icon);
        tvAuthStep6Icon = findViewById(R.id.tvAuthStep6Icon);
        tvAuthStep1Status = findViewById(R.id.tvAuthStep1Status);
        tvAuthStep2Status = findViewById(R.id.tvAuthStep2Status);
        tvAuthStep3Status = findViewById(R.id.tvAuthStep3Status);
        tvAuthStep4Status = findViewById(R.id.tvAuthStep4Status);
        tvAuthStep5Status = findViewById(R.id.tvAuthStep5Status);
        tvAuthStep6Status = findViewById(R.id.tvAuthStep6Status);
        pbAuthOverall = findViewById(R.id.pbAuthOverall);
        viewAuthResult = findViewById(R.id.viewAuthResult);
        tvAuthResultIcon = findViewById(R.id.tvAuthResultIcon);
        tvAuthResultMessage = findViewById(R.id.tvAuthResultMessage);
        btnAuthRetry = findViewById(R.id.btnAuthRetry);
        btnAuthRetry.setOnClickListener(v -> {
            clearAndReset();
            resetToStep1();
        });

        soundPool = new SoundPool.Builder().setMaxStreams(2)
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .build();
        soundSuccess = soundPool.load(this, R.raw.scan_success_bip, 1);
        soundError = soundPool.load(this, R.raw.scanne_errors_bip, 1);

        updateStepUI();
        showWizardView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    // -------------------------------------------------------------------------
    // Dispatch des résultats de scan
    // -------------------------------------------------------------------------

    private void dispatchScanResult(String content) {
        switch (currentStep) {
        case 1:
            handleStep1(content);
            break;
        case 2:
            handleStep2(content);
            break;
        case 3:
            handleStep3(content);
            break;
        }
    }

    /** Étape 1 : clés personnelles — format : "SL::PUBLIC_KEY::PRIVATE_KEY" */
    private void handleStep1(String content) {
        String PERSONNAL_QR_PREFIX = QRCodeConfig.PERSONNAL_QR_PREFIX;
        if (content.startsWith(PERSONNAL_QR_PREFIX)) {
            content = content.substring(PERSONNAL_QR_PREFIX.length());
        } else {
            playErrorBeep();
            Toast.makeText(this, "QR code invalide — Veuiller réessayez ", Toast.LENGTH_LONG).show();
            return;
        }
        String[] parts = content.split(QR_SEPARATOR, 2);
        if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
            playErrorBeep();
            Toast.makeText(this, "QR code invalide — Veuiller réessayez ", Toast.LENGTH_LONG).show();
            return;
        }

        playSuccessBeep(1);
        ConfigRepository config = ConfigRepository.getInstance(this);
        config.setPrivateKey(parts[1].trim());
        config.setMyPublicKey(parts[0].trim());
        user_public_key_value = config.getMyPublicKey();
        currentStep = 2;
        updateStepUI();
    }

    /**
     * Étape 2 : clés communes des unités — format :
     * "SL::UNIT_PUBLIC_KEY::UNIT_PRIVATE_KEY"
     */
    private void handleStep2(String content) {
        String COMMON_QR_PREFIX = QRCodeConfig.COMMON_QR_PREFIX;
        if (content.startsWith(COMMON_QR_PREFIX)) {
            content = content.substring(COMMON_QR_PREFIX.length());
        } else {
            playErrorBeep();
            Toast.makeText(this, "QR code invalide — attendu ", Toast.LENGTH_LONG).show();
            return;
        }

        String[] parts = content.split(QR_SEPARATOR, 2);
        if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
            playErrorBeep();
            Toast.makeText(this, "QR code invalide — attendu : clé publique unité ", Toast.LENGTH_LONG).show();
            return;
        }
        playSuccessBeep(2);
        ConfigRepository config = ConfigRepository.getInstance(this);
        config.setUnitPrivateKey(parts[1].trim());
        config.setUnitPublicKey(parts[0].trim());
        currentStep = 3;
        updateStepUI();
    }

    /** Étape 3 : URL de communication — format : "BASE_URL" */
    private void handleStep3(String content) {
        String QR_URL_PREFIX_SECURE = QRCodeConfig.QR_URL_PREFIX_SECURE;
        String QR_URL_PREFIX = QRCodeConfig.QR_URL_PREFIX;
        if (!content.startsWith(QR_URL_PREFIX) && !content.startsWith(QR_URL_PREFIX_SECURE)) {
            playErrorBeep();
            Toast.makeText(this, "URL invalide dans le QR code", Toast.LENGTH_LONG).show();
            return;
        }
        var baseUrl = content.trim();
        var credential = this.user_public_key_value;
        if (credential.isBlank()) {
            playErrorBeep();
            Toast.makeText(this, "Les credentials de l'utilisateur non trouvée", Toast.LENGTH_LONG).show();
            return;
        }
        playSuccessBeep(3);

        ConfigRepository config = ConfigRepository.getInstance(this);
        config.setApiBaseUrl(baseUrl);
        config.setAuthToken(credential);
        ApiClient.getInstance(this).rebuild(this);

        showLoadingView();
        setAuthStepLoading(1);

        // Étape 1 : initialisation locale, puis étape 2 via API (Authentification)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setAuthStepSuccess(1);
            updateOverallProgress(17);
            setAuthStepLoading(2);
            callAuthApi(credential);
        }, 700);
    }

    // -------------------------------------------------------------------------
    // Scanner
    // -------------------------------------------------------------------------

    private void openScanner() {
        PortraitCaptureActivity.scanStep = currentStep;
        ScanOptions options = new ScanOptions();
        options.setPrompt(""); // description affichée dans l'overlay du haut
        options.setBeepEnabled(false);
        options.setOrientationLocked(false);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setCaptureActivity(PortraitCaptureActivity.class);
        scanLauncher.launch(options);
    }

    private String getScanPromptForStep(int step) {
        switch (step) {
        case 1:
            return "Scannez votre QR code de clés personnelles";
        case 2:
            return "Scannez le QR code des clés communes des unités";
        case 3:
            return "Scannez votre QR code de connexion SentryLink";
        default:
            return "Scannez le QR code";
        }
    }

    // -------------------------------------------------------------------------
    // Mise à jour de l'UI wizard
    // -------------------------------------------------------------------------

    private void updateStepUI() {
        tvStepLabel.setText("Étape " + currentStep + " sur 3");

        switch (currentStep) {
        case 1:
            tvStepTitle.setText("CONFIGURATION DE L'UNITÉ");
            stepDot1.setBackgroundResource(R.drawable.step_dot_active);
            stepDot2.setBackgroundResource(R.drawable.step_dot_inactive);
            stepDot3.setBackgroundResource(R.drawable.step_dot_inactive);
            break;
        case 2:
            tvStepTitle.setText("PROTOCOLE DE GROUPES");
            stepDot1.setBackgroundResource(R.drawable.step_dot_done);
            stepDot2.setBackgroundResource(R.drawable.step_dot_active);
            stepDot3.setBackgroundResource(R.drawable.step_dot_inactive);
            break;
        case 3:
            tvStepTitle.setText("TUNNEL DE COMMUNICATION");
            stepDot1.setBackgroundResource(R.drawable.step_dot_done);
            stepDot2.setBackgroundResource(R.drawable.step_dot_done);
            stepDot3.setBackgroundResource(R.drawable.step_dot_active);
            break;
        }
    }

    // -------------------------------------------------------------------------
    // Appels API (séquentiels après étape 3)
    // -------------------------------------------------------------------------

    private void callAuthApi(String contact_token) {
        ApiClient.getInstance(this).getApi().contactAuth(contact_token).enqueue(new Callback<ContactAuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<ContactAuthResponse> call,
                    @NonNull Response<ContactAuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ContactAuthResponse auth = response.body();
                    if (!"ACTIVE".equals(auth.status)) {
                        setAuthStepError(2, "Compte inactif");
                        showAuthFailure("Compte inactif. Contactez votre administrateur.");
                        clearAndReset();
                        return;
                    }
                    UserRepository.getInstance(QrScanActivity.this).saveUser(auth);
                    setAuthStepSuccess(2);
                    updateOverallProgress(33);
                    setAuthStepLoading(3);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        setAuthStepSuccess(3);
                        updateOverallProgress(50);
                        setAuthStepLoading(4);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            setAuthStepSuccess(4);
                            updateOverallProgress(67);
                            setAuthStepLoading(5);
                            ContactService.getInstance(QrScanActivity.this)
                                    .syncContacts(new ContactService.Callback<Integer>() {
                                        @Override
                                        public void onSuccess(Integer result) {
                                            if (result == 0) {
                                                setAuthStepError(5, "Aucune unité disponible");
                                                showAuthFailure("Aucune unité n'est disponible dans le système.");
                                                clearAndReset();
                                                return;
                                            }
                                            setAuthStepSuccess(5);
                                            updateOverallProgress(83);
                                            setAuthStepLoading(6);
                                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                setAuthStepSuccess(6);
                                                updateOverallProgress(100);
                                                showAuthSuccess("Authentification réussie !");
                                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                    startActivity(new Intent(QrScanActivity.this, HomeActivity.class));
                                                    finish();
                                                }, 1500);
                                            }, 400);
                                        }

                                        @Override
                                        public void onError(String errorMessage, Throwable error) {
                                            setAuthStepError(5, errorMessage);
                                            showAuthFailure("Erreur de récupération des unités");
                                            clearAndReset();
                                        }
                                    });
                        }, 400);
                    }, 400);
                } else {
                    setAuthStepError(2, "Code " + response.code());
                    showAuthFailure("Authentification refusée (code " + response.code() + ")");
                    Log.e(TAG, "Authentification refusée : " + response.code() + " - " + response.message());
                    clearAndReset();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ContactAuthResponse> call, @NonNull Throwable t) {
                setAuthStepError(2, "Erreur réseau");
                showAuthFailure("Une erreur réseau est survenue");
                Log.e(TAG, "Auth API call failed", t);
                clearAndReset();
            }
        });
    }

    private void clearAndReset() {
        ConfigRepository.getInstance(this).clearAuthentication();
        UserRepository.getInstance(this).clearUser();
    }

    private void resetToStep1() {
        currentStep = 1;
        updateStepUI();
        showWizardView();
    }

    // -------------------------------------------------------------------------
    // Gestion des vues
    // -------------------------------------------------------------------------

    private void showWizardView() {
        viewWizard.setVisibility(View.VISIBLE);
        viewNoInternet.setVisibility(View.GONE);
        viewLoading.setVisibility(View.GONE);
    }

    private void showNoInternetView() {
        viewWizard.setVisibility(View.GONE);
        viewNoInternet.setVisibility(View.VISIBLE);
        viewLoading.setVisibility(View.GONE);
    }

    private void showLoadingView() {
        viewWizard.setVisibility(View.GONE);
        viewNoInternet.setVisibility(View.GONE);
        viewLoading.setVisibility(View.VISIBLE);

        // Réinitialiser toutes les étapes à l'état "en attente"
        resetAuthStepToPending(1);
        resetAuthStepToPending(2);
        resetAuthStepToPending(3);
        resetAuthStepToPending(4);
        resetAuthStepToPending(5);
        resetAuthStepToPending(6);
        pbAuthOverall.setProgress(0);
        viewAuthResult.setVisibility(View.GONE);
        btnAuthRetry.setVisibility(View.GONE);
    }

    // -------------------------------------------------------------------------
    // État des étapes de chargement
    // -------------------------------------------------------------------------

    private void resetAuthStepToPending(int step) {
        ProgressBar pb = getStepProgressBar(step);
        TextView icon = getStepIcon(step);
        TextView status = getStepStatus(step);
        if (pb == null || icon == null || status == null)
            return;

        pb.setVisibility(View.GONE);
        icon.setVisibility(View.VISIBLE);
        icon.setText("○");
        icon.setTextColor(Color.parseColor("#44FFFFFF"));
        status.setText("En attente…");
        status.setTextColor(Color.parseColor("#88FFFFFF"));
    }

    private void setAuthStepLoading(int step) {
        ProgressBar pb = getStepProgressBar(step);
        TextView icon = getStepIcon(step);
        TextView status = getStepStatus(step);
        if (pb == null || icon == null || status == null)
            return;

        pb.setVisibility(View.VISIBLE);
        icon.setVisibility(View.GONE);
        String message;
        switch (step) {
        case 1:
            message = "Initialisation du protocole sécurisée…";
            break;
        case 2:
            message = "Authentification…";
            break;
        case 3:
            message = "Établissement du canal sécurisé…";
            break;
        case 4:
            message = "Chargement des modules cryptographiques…";
            break;
        case 5:
            message = "Récupération des unités…";
            break;
        case 6:
            message = "Finalisation du protocole de communication…";
            break;
        default:
            message = "Configuration sécurisée…";
        }
        status.setText(message);
        status.setTextColor(Color.parseColor("#00D4FF"));
    }

    private void setAuthStepSuccess(int step) {
        ProgressBar pb = getStepProgressBar(step);
        TextView icon = getStepIcon(step);
        TextView status = getStepStatus(step);
        if (pb == null || icon == null || status == null)
            return;

        pb.setVisibility(View.GONE);
        icon.setVisibility(View.VISIBLE);
        icon.setText("✓");
        icon.setTextColor(Color.parseColor("#4CAF50"));
        status.setText("Terminé");
        status.setTextColor(Color.parseColor("#4CAF50"));
    }

    private void setAuthStepError(int step, String message) {
        ProgressBar pb = getStepProgressBar(step);
        TextView icon = getStepIcon(step);
        TextView status = getStepStatus(step);
        if (pb == null || icon == null || status == null)
            return;

        pb.setVisibility(View.GONE);
        icon.setVisibility(View.VISIBLE);
        icon.setText("✗");
        icon.setTextColor(Color.parseColor("#F44336"));
        status.setText(message);
        status.setTextColor(Color.parseColor("#F44336"));
    }

    private void updateOverallProgress(int progress) {
        pbAuthOverall.setProgress(progress, true);
    }

    private void showAuthSuccess(String message) {
        viewAuthResult.setVisibility(View.VISIBLE);
        tvAuthResultIcon.setText("✓");
        tvAuthResultIcon.setTextColor(Color.parseColor("#4CAF50"));
        tvAuthResultMessage.setText(message);
        tvAuthResultMessage.setTextColor(Color.parseColor("#4CAF50"));
        btnAuthRetry.setVisibility(View.GONE);
    }

    private void showAuthFailure(String message) {
        viewAuthResult.setVisibility(View.VISIBLE);
        pbAuthOverall.setProgress(0, true);
        tvAuthResultIcon.setText("✗");
        tvAuthResultIcon.setTextColor(Color.parseColor("#F44336"));
        tvAuthResultMessage.setText(message);
        tvAuthResultMessage.setTextColor(Color.parseColor("#F44336"));
        btnAuthRetry.setVisibility(View.VISIBLE);
    }

    // -------------------------------------------------------------------------
    // Accesseurs des vues d'étape
    // -------------------------------------------------------------------------

    private ProgressBar getStepProgressBar(int step) {
        switch (step) {
        case 1:
            return pbAuthStep1;
        case 2:
            return pbAuthStep2;
        case 3:
            return pbAuthStep3;
        case 4:
            return pbAuthStep4;
        case 5:
            return pbAuthStep5;
        case 6:
            return pbAuthStep6;
        default:
            return null;
        }
    }

    private TextView getStepIcon(int step) {
        switch (step) {
        case 1:
            return tvAuthStep1Icon;
        case 2:
            return tvAuthStep2Icon;
        case 3:
            return tvAuthStep3Icon;
        case 4:
            return tvAuthStep4Icon;
        case 5:
            return tvAuthStep5Icon;
        case 6:
            return tvAuthStep6Icon;
        default:
            return null;
        }
    }

    private TextView getStepStatus(int step) {
        switch (step) {
        case 1:
            return tvAuthStep1Status;
        case 2:
            return tvAuthStep2Status;
        case 3:
            return tvAuthStep3Status;
        case 4:
            return tvAuthStep4Status;
        case 5:
            return tvAuthStep5Status;
        case 6:
            return tvAuthStep6Status;
        default:
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Bip sonore
    // -------------------------------------------------------------------------

    private void playSuccessBeep(int times) {
        playPoolSound(soundSuccess, times);
    }

    private void playErrorBeep() {
        playPoolSound(soundError, 1);
    }

    private void playPoolSound(int soundId, int remaining) {
        if (remaining <= 0 || soundPool == null || soundId == -1)
            return;
        soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        if (remaining > 1) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> playPoolSound(soundId, remaining - 1), 600);
        }
    }

}
