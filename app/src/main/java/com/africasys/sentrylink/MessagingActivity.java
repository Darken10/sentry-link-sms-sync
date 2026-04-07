package com.africasys.sentrylink;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.africasys.sentrylink.adapter.MessageBubbleAdapter;
import com.africasys.sentrylink.enums.MessageType;
import com.africasys.sentrylink.mapper.SMSMapper;
import com.africasys.sentrylink.models.Contact;
import com.africasys.sentrylink.models.SMSMessage;
import com.africasys.sentrylink.repository.AppDatabase;
import com.africasys.sentrylink.repository.ContactDao;
import com.africasys.sentrylink.repository.SMSRepository;
import com.africasys.sentrylink.repository.SmsDao;
import com.africasys.sentrylink.service.MessageDispatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversation style messagerie instantanée (WhatsApp-like). Reçoit
 * {@link ContactsActivity#EXTRA_PHONE_NUMBER} pour identifier l'interlocuteur.
 */
public class MessagingActivity extends AppCompatActivity {

    private static final String TAG = "SL-MessagingActivity";

    private String phoneNumber;

    private RecyclerView rvMessages;
    private EditText messageInput;

    private SmsDao smsDao;
    private SMSRepository smsRepository;
    private MessageDispatcher messageDispatcher;

    private final List<SMSMessage> messages = new ArrayList<>();
    private MessageBubbleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        phoneNumber = getIntent().getStringExtra(ContactsActivity.EXTRA_PHONE_NUMBER);

        AppDatabase db = AppDatabase.getInstance(this);
        smsDao = db.smsDao();
        ContactDao contactDao = db.contactDao();
        smsRepository = new SMSRepository(this);
        messageDispatcher = new MessageDispatcher(this);

        // ── Toolbar ────────────────────────────────────────────────
        TextView tvName = findViewById(R.id.tvContactName);
        TextView tvPhone = findViewById(R.id.tvContactPhone);
        TextView tvAvatar = findViewById(R.id.tvContactAvatar);

        Contact contact = (phoneNumber != null) ? contactDao.findByPhoneNumber(phoneNumber) : null;
        if (contact != null && !TextUtils.isEmpty(contact.name)) {
            tvName.setText(contact.name);
            tvPhone.setText(phoneNumber);
            tvAvatar.setText(String.valueOf(contact.name.charAt(0)).toUpperCase());
        } else {
            tvName.setText(phoneNumber != null ? phoneNumber : getString(R.string.messaging_title));
            tvPhone.setText(getString(R.string.encryption_active));
            tvAvatar.setText("?");
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // ── RecyclerView ────────────────────────────────────────────
        rvMessages = findViewById(R.id.rvMessages);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true); // affichage du bas vers le haut
        rvMessages.setLayoutManager(llm);
        adapter = new MessageBubbleAdapter(messages);
        rvMessages.setAdapter(adapter);

        // ── Zone de saisie ──────────────────────────────────────────
        messageInput = findViewById(R.id.messageInput);
        ImageButton btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMessages();
    }

    // ─────────────────────────────────────────────────────────────────
    // Chargement des messages
    // ─────────────────────────────────────────────────────────────────

    private void loadMessages() {
        try {
            List<SMSMessage> loaded = (phoneNumber != null && !phoneNumber.isEmpty())
                    ? smsDao.getSlMessagesByPhone(phoneNumber)
                    : smsDao.getAllSMS();

            messages.clear();
            messages.addAll(loaded);
            adapter.notifyDataSetChanged();

            if (!messages.isEmpty()) {
                rvMessages.scrollToPosition(messages.size() - 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur chargement messages", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Envoi d'un message
    // ─────────────────────────────────────────────────────────────────

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(text))
            return;
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, R.string.msg_empty_phone, Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Chiffrer le message pour l'envoi SMS
        String encryptedBody;
        try {
            encryptedBody = messageDispatcher.buildEncryptedSmsBody(phoneNumber, text, MessageType.MSG);
        } catch (Exception e) {
            Log.e(TAG, "Erreur chiffrement message", e);
            Toast.makeText(this, getString(R.string.msg_error, e.getMessage()), Toast.LENGTH_LONG).show();
            return;
        }

        // Input effacé uniquement après un chiffrement réussi
        messageInput.setText("");

        // 2. Sauvegarder le message EN CLAIR en base (le chiffrement est réservé au
        // canal SMS)
        SMSMessage entity = SMSMapper.toEntity(phoneNumber, text, 2);
        Log.d(TAG, "Message en clair sauvegardé en BD: " + text);
        smsRepository.saveSMS(entity);
        loadMessages();

        // 3. Expédier le corps chiffré (pas de double-chiffrement)
        messageDispatcher.dispatchPreEncrypted(phoneNumber, encryptedBody, new MessageDispatcher.DispatchCallback() {
            @Override
            public void onSuccess(String channel) {
                Log.d(TAG, "SMS chiffré envoyé via " + channel);
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> Toast
                        .makeText(MessagingActivity.this, getString(R.string.msg_error, error), Toast.LENGTH_LONG)
                        .show());
            }
        });
        Log.d(TAG, "SMS chiffré envoyé à: " + phoneNumber);
    }

}
