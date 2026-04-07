package com.africasys.sentrylink.smssync;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private ConfigRepository configRepository;
    private EditText etWebhookUrl;

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

        etWebhookUrl = findViewById(R.id.etWebhookUrl);
        String savedUrl = configRepository.getWebhookUrl();
        if (savedUrl != null) etWebhookUrl.setText(savedUrl);

        findViewById(R.id.btnSaveWebhook).setOnClickListener(v -> saveWebhookUrl());

        loadUserProfile();
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

