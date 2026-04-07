package com.africasys.sentrylink;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.africasys.sentrylink.adapter.SmsListAdapter;
import com.africasys.sentrylink.dtos.SmsRequestDTO;
import com.africasys.sentrylink.dtos.SmsResponseDTO;
import com.africasys.sentrylink.dtos.SmsSendResultDTO;
import com.africasys.sentrylink.service.LocationService;
import com.africasys.sentrylink.service.SmsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;
    private static final String TAG = "SL-MainActivity";

    // SMS UI Components
    private EditText phoneNumberInput;
    private EditText messageInput;
    private Button sendButton;
    private Button refreshButton;
    private TextView statusTextView;
    private ListView smsListView;
    private TextView countTextView;

    // Service
    private SmsService smsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize SMS service
        smsService = new SmsService(this);

        // Initialize UI components
        initializeUI();

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Request permissions
        requestSmsPermissions();
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        try {
            phoneNumberInput = findViewById(R.id.phoneNumberInput);
            messageInput = findViewById(R.id.messageInput);
            sendButton = findViewById(R.id.sendButton);
            refreshButton = findViewById(R.id.refreshButton);
            statusTextView = findViewById(R.id.statusTextView);
            smsListView = findViewById(R.id.smsListView);
            countTextView = findViewById(R.id.countTextView);

            // Set button listeners
            sendButton.setOnClickListener(v -> sendSMS());
            refreshButton.setOnClickListener(v -> refreshSMSList());

            // Make status text scrollable
            statusTextView.setMovementMethod(new ScrollingMovementMethod());

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation de l'interface", e);
            Toast.makeText(this, "Erreur d'initialisation", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Request SMS permissions
     */
    private void requestSmsPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            // Permissions already granted
            refreshSMSList();
        } else {
            // Request permissions
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS }, REQUEST_CODE);
        }
    }

    /**
     * Send SMS
     */
    private void sendSMS() {
        try {
            String phoneNumber = phoneNumberInput.getText().toString().trim();
            String message = messageInput.getText().toString().trim();

            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer un numéro de téléphone", Toast.LENGTH_SHORT).show();
                return;
            }

            if (message.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer un message", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create request DTO
            SmsRequestDTO request = new SmsRequestDTO(phoneNumber, message);

            // Send SMS using service
            SmsSendResultDTO result = smsService.sendSMS(request);

            if (result.isSuccess()) {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                phoneNumberInput.setText("");
                messageInput.setText("");
                refreshSMSList();
                addStatusMessage("✓ SMS envoyé à: " + phoneNumber);
            } else {
                Toast.makeText(this, "Erreur: " + result.getMessage(), Toast.LENGTH_LONG).show();
                addStatusMessage("✗ Erreur: " + result.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'envoi du SMS", e);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
            addStatusMessage("✗ Exception: " + e.getMessage());
        }
    }

    /**
     * Refresh SMS list
     */
    private void refreshSMSList() {
        try {
            // Get all SMS from service
            List<SmsResponseDTO> smsList = smsService.getAllSMS();

            // Update counts
            int totalCount = smsService.getTotalSMSCount();
            int receivedCount = smsService.getReceivedSMSCount();
            int sentCount = smsService.getSentSMSCount();

            countTextView.setText(
                    String.format("Total: %d | Reçus: %d | Envoyés: %d", totalCount, receivedCount, sentCount));

            // Use custom adapter
            SmsListAdapter adapter = new SmsListAdapter(this, smsList);
            smsListView.setAdapter(adapter);

            addStatusMessage("✓ Liste mise à jour: " + totalCount + " SMS");

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la mise à jour de la liste", e);
            addStatusMessage("✗ Erreur lors de la mise à jour");
        }
    }

    /**
     * Add status message to status view
     */
    private void addStatusMessage(String message) {
        String currentText = statusTextView.getText().toString();
        String newText = message + "\n" + currentText;
        statusTextView.setText(newText);
    }

    /**
     * Handle permission results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                Log.d(TAG, "Permissions accordées");
                refreshSMSList();
            } else {
                Toast.makeText(this, "Permissions refusées", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Permissions refusées");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh SMS list when activity is resumed
        if (smsService != null) {
            refreshSMSList();
        }
    }
}