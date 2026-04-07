package com.africasys.sentrylink;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.africasys.sentrylink.adapter.ConversationAdapter;
import com.africasys.sentrylink.models.Contact;
import com.africasys.sentrylink.models.ConversationSummary;
import com.africasys.sentrylink.models.EncryptionKey;
import com.africasys.sentrylink.repository.AppDatabase;
import com.africasys.sentrylink.repository.ContactDao;
import com.africasys.sentrylink.repository.ContactRepository;
import com.africasys.sentrylink.repository.SmsDao;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Page listant toutes les conversations SentryLink.
 *
 * <p>
 * Seuls les messages portant le préfixe {@code [SL]} sont pris en compte.
 * Pour chaque numéro de téléphone ayant au moins un message {@code [SL]},
 * la conversation est affichée avec le nom du contact (s'il est en base locale)
 * ou le numéro brut sinon.
 *
 * <p>
 * Cliquer sur une conversation ouvre {@link MessagingActivity} avec le
 * numéro de téléphone pré-renseigné.
 */
public class ContactsActivity extends AppCompatActivity {

    /** Extra passé à MessagingActivity pour pré-remplir le numéro. */
    public static final String EXTRA_PHONE_NUMBER = "phone_number";
    public static final String LOG_TAG = "SL-ContactsActivity";

    private RecyclerView rvConversations;
    private LinearLayout layoutEmpty;
    private TextView tvConvCount;

    private SmsDao smsDao;
    private ContactDao contactDao;
    private ContactRepository contactRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        AppDatabase db = AppDatabase.getInstance(this);
        smsDao = db.smsDao();
        contactDao = db.contactDao();
        contactRepository = ContactRepository.getInstance(this);

        rvConversations = findViewById(R.id.rvConversations);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvConvCount = findViewById(R.id.tvConvCount);

        rvConversations.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        FloatingActionButton fabNew = findViewById(R.id.fabNewConversation);
        fabNew.setOnClickListener(v -> startActivity(new Intent(this, NewConversationActivity.class)));

        loadConversations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rafraîchir la liste quand on revient de MessagingActivity
        loadConversations();
    }

    // -------------------------------------------------------------------------
    // Chargement des données
    // -------------------------------------------------------------------------

    private void loadConversations() {
        // 1. Tous les résumés de conversations [SL]
        List<ConversationSummary> summaries = smsDao.getConversationSummaries();

        // 2. Map phone_number → Contact pour la résolution des noms
        List<Contact> allContacts = contactDao.getAll();

        Map<String, Contact> contactMap = new HashMap<>();
        for (Contact c : allContacts) {
            if (c.phoneNumber != null) {
                contactMap.put(c.phoneNumber, c);
            }
        }

        // 3. Affichage
        if (summaries.isEmpty()) {
            rvConversations.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvConvCount.setText("");
        } else {
            rvConversations.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);

            int count = summaries.size();
            tvConvCount.setText(count + " conversation" + (count > 1 ? "s" : ""));

            ConversationAdapter adapter = new ConversationAdapter(
                    this,
                    summaries,
                    contactMap,
                    (summary, contact) -> openConversation(summary.phoneNumber));
            rvConversations.setAdapter(adapter);
        }
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    private void openConversation(String phoneNumber) {
        Intent intent = new Intent(this, MessagingActivity.class);
        intent.putExtra(EXTRA_PHONE_NUMBER, phoneNumber);
        startActivity(intent);
    }
}
