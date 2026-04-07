package com.africasys.sentrylink.smssync;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.africasys.sentrylink.smssync.models.AuthenticatedUser;
import com.africasys.sentrylink.smssync.repository.ConfigRepository;
import com.africasys.sentrylink.smssync.repository.UserRepository;
import com.africasys.sentrylink.smssync.service.HttpGatewayService;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private ConfigRepository configRepository;

    // Webhook
    private EditText etWebhookUrl;

    // HTTP Gateway
    private TextView tvGatewayAddress;
    private TextView tvApiToken;
    private EditText etServerPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        configRepository = ConfigRepository.getInstance(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnLogout).setOnClickListener(v -> confirmLogout());

        setupWebhookSection();
        setupGatewaySection();
        loadUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshGatewayStatus();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section Webhook
    // ─────────────────────────────────────────────────────────────────────────

    private void setupWebhookSection() {
        etWebhookUrl = findViewById(R.id.etWebhookUrl);
        String savedUrl = configRepository.getWebhookUrl();
        if (savedUrl != null) etWebhookUrl.setText(savedUrl);
        findViewById(R.id.btnSaveWebhook).setOnClickListener(v -> saveWebhookUrl());
    }

    private void saveWebhookUrl() {
        String url = etWebhookUrl.getText().toString().trim();
        if (!url.isEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "L'URL doit commencer par http:// ou https://", Toast.LENGTH_SHORT).show();
            return;
        }
        configRepository.setWebhookUrl(url.isEmpty() ? null : url);
        Toast.makeText(this,
                url.isEmpty() ? "Webhook désactivé" : "URL webhook enregistrée",
                Toast.LENGTH_SHORT).show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section Passerelle HTTP
    // ─────────────────────────────────────────────────────────────────────────

    private void setupGatewaySection() {
        tvGatewayAddress = findViewById(R.id.tvGatewayAddress);
        tvApiToken       = findViewById(R.id.tvApiToken);
        etServerPort     = findViewById(R.id.etServerPort);

        // Afficher le port sauvegardé
        etServerPort.setText(String.valueOf(configRepository.getHttpServerPort()));

        // Afficher le token (peut être null si pas encore démarré)
        refreshTokenDisplay();

        // Bouton Copier
        findViewById(R.id.btnCopyToken).setOnClickListener(v -> copyTokenToClipboard());

        // Bouton Start / Stop
        findViewById(R.id.btnToggleGateway).setOnClickListener(v -> toggleGateway());

        // Bouton Régénérer
        findViewById(R.id.btnRegenToken).setOnClickListener(v -> confirmRegenToken());
    }

    private void refreshGatewayStatus() {
        boolean running = HttpGatewayService.isRunning;
        int port = configRepository.getHttpServerPort();
        String ip = HttpGatewayService.getWifiIpAddress(this);

        if (running) {
            tvGatewayAddress.setText("http://" + ip + ":" + port + "/send-sms");
            ((com.google.android.material.button.MaterialButton)
                    findViewById(R.id.btnToggleGateway))
                    .setText("Arrêter la passerelle");
        } else {
            tvGatewayAddress.setText("Passerelle inactive");
            ((com.google.android.material.button.MaterialButton)
                    findViewById(R.id.btnToggleGateway))
                    .setText("Démarrer la passerelle");
        }
        refreshTokenDisplay();
    }

    private void refreshTokenDisplay() {
        String token = configRepository.getHttpApiToken();
        tvApiToken.setText(token != null ? token : "—  (générée au premier démarrage)");
    }

    private void toggleGateway() {
        if (HttpGatewayService.isRunning) {
            stopGateway();
        } else {
            startGateway();
        }
    }

    private void startGateway() {
        // Sauvegarder le port avant de démarrer
        String portStr = etServerPort.getText().toString().trim();
        int port = 8080;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                Toast.makeText(this, "Port invalide (1024–65535)", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Port invalide", Toast.LENGTH_SHORT).show();
            return;
        }
        configRepository.setHttpServerPort(port);
        configRepository.setHttpGatewayEnabled(true);

        Intent intent = new Intent(this, HttpGatewayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "Passerelle démarrée sur le port " + port, Toast.LENGTH_SHORT).show();

        // Rafraîchir l'UI après un court délai (le service prend ≈ 100 ms à démarrer)
        tvGatewayAddress.postDelayed(this::refreshGatewayStatus, 300);
    }

    private void stopGateway() {
        configRepository.setHttpGatewayEnabled(false);
        stopService(new Intent(this, HttpGatewayService.class));
        Toast.makeText(this, "Passerelle arrêtée", Toast.LENGTH_SHORT).show();
        tvGatewayAddress.postDelayed(this::refreshGatewayStatus, 200);
    }

    private void copyTokenToClipboard() {
        String token = configRepository.getHttpApiToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Aucun token à copier", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("API Token", token));
        Toast.makeText(this, "Token copié", Toast.LENGTH_SHORT).show();
    }

    private void confirmRegenToken() {
        new AlertDialog.Builder(this)
                .setTitle("Régénérer le token")
                .setMessage("Le nouveau token remplacera l'ancien. Votre PC devra être mis à jour.")
                .setPositiveButton("Régénérer", (d, w) -> regenToken())
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void regenToken() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        String newToken = sb.toString();

        configRepository.setHttpApiToken(newToken);
        refreshTokenDisplay();

        // Redémarrer le service avec le nouveau token si actif
        if (HttpGatewayService.isRunning) {
            stopService(new Intent(this, HttpGatewayService.class));
            tvGatewayAddress.postDelayed(() -> {
                Intent intent = new Intent(this, HttpGatewayService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(intent);
                else
                    startService(intent);
                tvGatewayAddress.postDelayed(this::refreshGatewayStatus, 300);
            }, 300);
        }
        Toast.makeText(this, "Nouveau token généré", Toast.LENGTH_SHORT).show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Profil & Déconnexion
    // ─────────────────────────────────────────────────────────────────────────

    private void loadUserProfile() {
        AuthenticatedUser user = UserRepository.getInstance(this).getCurrentUser();
        if (user == null) return;

        ((TextView) findViewById(R.id.tvProfileName)).setText(user.name);
        ((TextView) findViewById(R.id.tvProfileIdentifier)).setText(user.identifier);
        ((TextView) findViewById(R.id.tvProfileContactId)).setText(String.valueOf(user.contactId));

        TextView tvStatus = findViewById(R.id.tvProfileStatus);
        tvStatus.setText(user.status);
        if ("ACTIVE".equals(user.status)) {
            tvStatus.setTextColor(getColor(R.color.accent));
            tvStatus.setBackgroundResource(R.drawable.status_badge_active);
        } else {
            tvStatus.setTextColor(getColor(R.color.sos_red));
        }

        String date = new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault())
                .format(new Date(user.createdAt));
        ((TextView) findViewById(R.id.tvProfileCreatedAt)).setText(date);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vraiment vous déconnecter ? Vous devrez rescanner un QR code pour accéder à l'application.")
                .setPositiveButton("Se déconnecter", (dialog, which) -> logout())
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void logout() {
        configRepository.clearAuthentication();
        UserRepository.getInstance(this).clearUser();
        Intent intent = new Intent(this, QrScanActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
