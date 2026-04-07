package com.africasys.sentrylink;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.africasys.sentrylink.models.SosAlert;
import com.africasys.sentrylink.repository.AppDatabase;
import com.africasys.sentrylink.repository.SosDao;
import com.africasys.sentrylink.service.SosService;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Page SOS : envoi d'alertes d'urgence avec localisation.
 */
public class SosActivity extends AppCompatActivity {

    private static final String TAG = "SL-SosActivity";

    private SosService sosService;
    private SosDao sosDao;

    private Button btnSos;
    private ProgressBar sosProgress;
    private TextView sosStatus;
    private SwitchMaterial switchSilentMode;
    private ListView sosHistoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sos);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sosService = new SosService(this);
        sosDao = AppDatabase.getInstance(this).sosDao();

        initializeUI();
        loadHistory();
    }

    private void initializeUI() {
        btnSos = findViewById(R.id.btnSos);
        sosProgress = findViewById(R.id.sosProgress);
        sosStatus = findViewById(R.id.sosStatus);
        switchSilentMode = findViewById(R.id.switchSilentMode);
        sosHistoryList = findViewById(R.id.sosHistoryList);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSos.setOnClickListener(v -> showSosConfirmation());

        switchSilentMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                sosService.startSilentMode();
                sosStatus.setText(R.string.sos_silent_active);
                sosStatus.setTextColor(getColor(R.color.status_pending));
            } else {
                sosService.stopSilentMode();
                sosStatus.setText("");
            }
        });
    }

    private void showSosConfirmation() {
        new AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle(R.string.sos_confirm_title).setMessage(R.string.sos_confirm_message)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> sendSos())
                .setNegativeButton(R.string.btn_cancel, null).setIcon(R.drawable.ic_sos).show();
    }

    private void sendSos() {
        btnSos.setEnabled(false);
        sosProgress.setVisibility(View.VISIBLE);
        sosStatus.setText(R.string.sos_sending);
        sosStatus.setTextColor(getColor(R.color.status_pending));

        sosService.sendSosAlert(new SosService.SosCallback() {
            @Override
            public void onSosSent(String channel, SosAlert alert) {
                btnSos.setEnabled(true);
                sosProgress.setVisibility(View.GONE);
                sosStatus.setText(String.format(getString(R.string.sos_sent), channel));
                sosStatus.setTextColor(getColor(R.color.status_sent));
                loadHistory();
            }

            @Override
            public void onSosFailed(String error) {
                btnSos.setEnabled(true);
                sosProgress.setVisibility(View.GONE);
                sosStatus.setText(String.format(getString(R.string.sos_failed), error));
                sosStatus.setTextColor(getColor(R.color.status_failed));
                loadHistory();
            }
        });
    }

    private void loadHistory() {
        try {
            List<SosAlert> alerts = sosDao.getAll();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            List<String> items = new ArrayList<>();

            for (SosAlert alert : alerts) {
                String date = sdf.format(new Date(alert.getTimestamp()));
                String status = alert.getStatus() != null ? alert.getStatus() : "?";
                String via = alert.getSentVia() != null ? alert.getSentVia() : "?";
                String source = alert.getLocationSource() != null ? alert.getLocationSource() : "?";

                String item = String.format("⚠ %s | %s | Via: %s | Loc: %s", date, status, via, source);
                if ("GPS".equals(source) && alert.getLatitude() != 0) {
                    item += String.format("\n  %.4f, %.4f", alert.getLatitude(), alert.getLongitude());
                }
                items.add(item);
            }

            if (items.isEmpty()) {
                items.add("Aucune alerte SOS enregistrée");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
            sosHistoryList.setAdapter(adapter);

        } catch (Exception e) {
            // BDD pas encore prête
        }
    }
}
