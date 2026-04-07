package com.africasys.sentrylink;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.africasys.sentrylink.models.AuthenticatedUser;
import com.africasys.sentrylink.network.NetworkMonitor;
import com.africasys.sentrylink.repository.ConfigRepository;
import com.africasys.sentrylink.repository.UserRepository;
import com.africasys.sentrylink.service.ContactService;
import com.africasys.sentrylink.service.SmsMonitorService;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Page d'accueil SentryLink avec navigation par cartes.
 */
public class HomeActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    private NetworkMonitor networkMonitor;
    private UserRepository userRepository;
    private ContactService contactService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Vérifier l'authentification via la base de données
        if (!UserRepository.getInstance(this).isAuthenticated()) {
            startActivity(new Intent(this, QrScanActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialiser les repositories
        networkMonitor = new NetworkMonitor(this);
        userRepository = UserRepository.getInstance(this);
        contactService = ContactService.getInstance(this);

        updateNetworkStatus();
        displayCurrentUser();

        // Configurer les cartes de navigation
        setupCards();

        // Demander les permissions au premier lancement
        requestAllPermissions();

        // Démarrer le service de surveillance en arrière-plan
        startMonitorService();

        // Synchroniser les contacts au lancement
        // syncContacts();

        // Synchroniser la clé privée au lancement
        // syncPrivateKey();
    }

    private void setupCards() {
        MaterialCardView cardMessaging = findViewById(R.id.cardMessaging);
        MaterialCardView cardLocation = findViewById(R.id.cardLocation);
        MaterialCardView cardSos = findViewById(R.id.cardSos);
        MaterialCardView cardSettings = findViewById(R.id.cardSettings);

        cardMessaging.setOnClickListener(v -> {
            startActivity(new Intent(this, ContactsActivity.class));
        });

        cardLocation.setOnClickListener(v -> {
            startActivity(new Intent(this, LocationActivity.class));
        });

        cardSos.setOnClickListener(v -> {
            startActivity(new Intent(this, SosActivity.class));
        });

        cardSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });
    }

    private void displayCurrentUser() {
        AuthenticatedUser user = userRepository.getCurrentUser();
        TextView userName = findViewById(R.id.tvUserName);
        if (user != null && userName != null) {
            userName.setText(user.name);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateNetworkStatus() {
        TextView networkStatus = findViewById(R.id.networkStatus);
        if (networkMonitor.isInternetAvailable()) {
            networkStatus.setText("● Connecté — Connexion Internet disponible");
            networkStatus.setTextColor(getColor(R.color.accent));
        } else if (networkMonitor.isSimAvailable()) {
            networkStatus.setText("● Hors ligne — Canal SMS actif");
            networkStatus.setTextColor(getColor(R.color.status_pending));
        } else {
            networkStatus.setText("● Déconnecté — Aucun canal disponible");
            networkStatus.setTextColor(getColor(R.color.status_failed));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNetworkStatus();
    }

    private void startMonitorService() {
        Intent serviceIntent = new Intent(this, SmsMonitorService.class);
        startForegroundService(serviceIntent);
    }

    private void requestAllPermissions() {
        List<String> permissions = new ArrayList<>();

        String[] required = { Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE, };

        for (String perm : required) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(perm);
            }
        }

        // Android 13+ notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    private void syncContacts() {
        contactService.syncContacts(new ContactService.Callback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                // Optionnel : afficher un toast ou mettre à jour l'UI
                Toast.makeText(HomeActivity.this, "la sync des contacts cest bien passe", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMessage, Throwable error) {
                // Optionnel : afficher un toast d'erreur
                Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void syncPrivateKey() {
        String existingKey = ConfigRepository.getInstance(this).getPrivateKey();
        if (existingKey != null && !existingKey.isEmpty()) {
            return; // Clé déjà disponible localement
        }
        /*
         * encryptionKeyService.syncPrivateKey(new
         * EncryptionKeyService.Callback<Boolean>() {
         * 
         * @Override public void onSuccess(Boolean result) { if (result) {
         * android.util.Log.d("HomeActivity", "Clé privée synchronisée avec succès"); }
         * }
         * 
         * @Override public void onError(String message, Throwable cause) {
         * android.util.Log.w("HomeActivity", "Sync clé privée échouée: " + message); }
         * });
         */
    }
}
