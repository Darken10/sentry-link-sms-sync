package com.africasys.sentrylink;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.africasys.sentrylink.models.AuthenticatedUser;
import com.africasys.sentrylink.repository.ConfigRepository;
import com.africasys.sentrylink.repository.UserRepository;
import com.africasys.sentrylink.service.ContactService;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SL-SettingsActivity";

    private ConfigRepository configRepository;
    private EditText inputPeriodicLocInterval;

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
        inputPeriodicLocInterval = findViewById(R.id.inputPeriodicLocInterval);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((MaterialButton) findViewById(R.id.btnSave)).setOnClickListener(v -> saveSettings());
        ((MaterialButton) findViewById(R.id.btnSyncContacts)).setOnClickListener(v -> syncContacts());
        ((MaterialButton) findViewById(R.id.btnLogout)).setOnClickListener(v -> confirmLogout());

        loadUserProfile();
        loadSettings();
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

    private void loadSettings() {
        // Intervalle envoi auto LOC — affiché en minutes
        long intervalMs = configRepository.getPeriodicLocInterval();
        long minutes = intervalMs / 60000;
        inputPeriodicLocInterval.setText(String.valueOf(minutes));
    }

    private void saveSettings() {
        String intervalStr = inputPeriodicLocInterval.getText().toString().trim();
        if (!intervalStr.isEmpty()) {
            try {
                long minutes = Long.parseLong(intervalStr);
                if (minutes < 1) {
                    Toast.makeText(this, "L'intervalle minimum est 1 minute", Toast.LENGTH_SHORT).show();
                    return;
                }
                configRepository.setPeriodicLocInterval(minutes * 60000L);
                Log.i(TAG, "Intervalle localisation auto sauvegardé : " + minutes + " min");
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Valeur d'intervalle invalide", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
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

    private void syncContacts() {
        Toast.makeText(this, "Synchronisation en cours...", Toast.LENGTH_SHORT).show();
        MaterialButton btnSync = findViewById(R.id.btnSyncContacts);
        btnSync.setEnabled(false);

        ContactService.getInstance(this).syncContacts(new ContactService.Callback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                runOnUiThread(() -> {
                    btnSync.setEnabled(true);
                    String message = count + " contact(s) synchronisé(s)";
                    Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, message);
                });
            }

            @Override
            public void onError(String message, Throwable cause) {
                runOnUiThread(() -> {
                    btnSync.setEnabled(true);
                    Toast.makeText(SettingsActivity.this, "Erreur sync: " + message, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Sync error: " + message, cause);
                });
            }
        });
    }
}
