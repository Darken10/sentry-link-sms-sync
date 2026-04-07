package com.africasys.sentrylink;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.africasys.sentrylink.models.AuthenticatedUser;
import com.africasys.sentrylink.network.NetworkMonitor;
import com.africasys.sentrylink.repository.AppDatabase;
import com.africasys.sentrylink.repository.UserRepository;
import com.africasys.sentrylink.service.SmsMonitorService;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Tableau de bord principal — Sentry Link SMS Sync.
 * Affiche le statut du service, les statistiques SMS et navigue vers le log et les paramètres.
 */
public class HomeActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    private NetworkMonitor networkMonitor;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        networkMonitor = new NetworkMonitor(this);
        userRepository = UserRepository.getInstance(this);

        displayCurrentUser();
        updateNetworkStatus();

        MaterialCardView cardSmsLog = findViewById(R.id.cardSmsLog);
        MaterialCardView cardSettings = findViewById(R.id.cardSettings);

        cardSmsLog.setOnClickListener(v -> startActivity(new Intent(this, SmsLogActivity.class)));
        cardSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        requestAllPermissions();
        startMonitorService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNetworkStatus();
        updateStats();
    }

    private void displayCurrentUser() {
        AuthenticatedUser user = userRepository.getCurrentUser();
        TextView tvUserName = findViewById(R.id.tvUserName);
        if (user != null && tvUserName != null) {
            tvUserName.setText(user.name != null ? user.name : "");
        }
    }

    private void updateStats() {
        new Thread(() -> {
            int total = AppDatabase.getInstance(this).smsDao().getTotalCount();
            runOnUiThread(() -> {
                TextView tvTotal = findViewById(R.id.tvStatTotal);
                if (tvTotal != null) tvTotal.setText(String.valueOf(total));
            });
        }).start();
    }

    private void updateNetworkStatus() {
        TextView serviceStatus = findViewById(R.id.tvServiceStatus);
        TextView networkStatus = findViewById(R.id.networkStatus);

        if (serviceStatus != null) {
            serviceStatus.setText(R.string.home_service_running);
            serviceStatus.setTextColor(getColor(R.color.accent));
        }

        if (networkStatus != null) {
            if (networkMonitor.isInternetAvailable()) {
                networkStatus.setText("● API disponible");
                networkStatus.setTextColor(getColor(R.color.accent));
            } else {
                networkStatus.setText("● Hors ligne — synchronisation en attente");
                networkStatus.setTextColor(getColor(R.color.status_pending));
            }
        }
    }

    private void startMonitorService() {
        Intent serviceIntent = new Intent(this, SmsMonitorService.class);
        startForegroundService(serviceIntent);
    }

    private void requestAllPermissions() {
        List<String> permissions = new ArrayList<>();

        String[] required = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE,
        };

        for (String perm : required) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(perm);
            }
        }

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
}

