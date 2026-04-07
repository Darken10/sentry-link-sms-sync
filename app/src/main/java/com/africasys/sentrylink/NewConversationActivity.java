package com.africasys.sentrylink;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.africasys.sentrylink.adapter.ContactPickerAdapter;
import com.africasys.sentrylink.models.AuthenticatedUser;
import com.africasys.sentrylink.models.Contact;
import com.africasys.sentrylink.repository.AppDatabase;
import com.africasys.sentrylink.repository.ContactDao;
import com.africasys.sentrylink.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Écran de sélection d'un contact pour initier une nouvelle conversation.
 *
 * <p>
 * Affiche tous les contacts présents en base locale (avec numéro de téléphone),
 * avec un champ de recherche permettant de filtrer par nom, identifiant ou
 * numéro.
 * Un clic ouvre {@link MessagingActivity} avec le numéro pré-renseigné.
 */
public class NewConversationActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvContacts;
    private LinearLayout layoutEmpty;

    private ContactDao contactDao;
    private List<Contact> allContacts = new ArrayList<>();
    private ContactPickerAdapter adapter;

    /** contactId de l'utilisateur connecté — pour l'exclure de la liste. */
    private int currentUserContactId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_conversation);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        contactDao = AppDatabase.getInstance(this).contactDao();

        // Récupérer le contactId de l'utilisateur connecté
        AuthenticatedUser currentUser = UserRepository.getInstance(this).getCurrentUser();
        if (currentUser != null) {
            currentUserContactId = currentUser.contactId;
        }

        etSearch = findViewById(R.id.etSearch);
        rvContacts = findViewById(R.id.rvContacts);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        rvContacts.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Bouton ajout contact hors répertoire
        ImageButton btnAddExternal = findViewById(R.id.btnAddExternal);
        btnAddExternal.setOnClickListener(v -> showAddExternalContactDialog());

        loadContacts();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContacts(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    // -------------------------------------------------------------------------
    // Données
    // -------------------------------------------------------------------------

    private void loadContacts() {
        // Uniquement les contacts qui ont un numéro de téléphone, sauf l'utilisateur
        // connecté
        List<Contact> all = contactDao.getAll();
        allContacts = new ArrayList<>();
        for (Contact c : all) {
            if (c.phoneNumber != null && !c.phoneNumber.isEmpty()) {
                // Exclure l'utilisateur actuellement connecté
                if (currentUserContactId >= 0 && c.id == currentUserContactId) {
                    continue;
                }
                allContacts.add(c);
            }
        }
        filterContacts("");
    }

    private void filterContacts(String query) {
        List<Contact> filtered;
        if (query.isEmpty()) {
            filtered = allContacts;
        } else {
            String lower = query.toLowerCase();
            filtered = new ArrayList<>();
            for (Contact c : allContacts) {
                boolean matchName = c.name != null && c.name.toLowerCase().contains(lower);
                boolean matchId = c.identifier != null && c.identifier.toLowerCase().contains(lower);
                boolean matchPhone = c.phoneNumber != null && c.phoneNumber.contains(lower);
                if (matchName || matchId || matchPhone) {
                    filtered.add(c);
                }
            }
        }

        if (filtered.isEmpty()) {
            rvContacts.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvContacts.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }

        if (adapter == null) {
            adapter = new ContactPickerAdapter(filtered, this::openConversation);
            rvContacts.setAdapter(adapter);
        } else {
            adapter.updateList(filtered);
        }
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    private void openConversation(Contact contact) {
        Intent intent = new Intent(this, MessagingActivity.class);
        intent.putExtra(ContactsActivity.EXTRA_PHONE_NUMBER, contact.phoneNumber);
        startActivity(intent);
    }

    // -------------------------------------------------------------------------
    // Ajout d'un contact hors répertoire
    // -------------------------------------------------------------------------

    private void showAddExternalContactDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint(R.string.hint_phone_number);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_external_contact_title)
                .setMessage(R.string.add_external_contact_desc)
                .setView(input)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    String phone = input.getText().toString().trim();
                    if (phone.isEmpty()) {
                        Toast.makeText(this, R.string.msg_empty_phone, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createExternalContactAndOpen(phone);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void createExternalContactAndOpen(String phoneNumber) {
        // Vérifier si le contact existe déjà
        Contact existing = contactDao.findByPhoneNumber(phoneNumber);
        if (existing != null) {
            openConversation(existing);
            return;
        }

        // Créer un contact hors répertoire
        Contact contact = new Contact();
        contact.id = System.currentTimeMillis(); // ID temporaire unique
        contact.uuid = UUID.randomUUID().toString();
        contact.name = phoneNumber;
        contact.phoneNumber = phoneNumber;
        contact.type = "UNIT";
        contact.status = "ACTIVE";
        contact.syncedAt = System.currentTimeMillis();
        contact.isInDirectory = false; // Ce contact n'existe pas dans la base de production

        contactDao.upsert(contact);

        // Ouvrir directement la conversation
        openConversation(contact);
    }
}
