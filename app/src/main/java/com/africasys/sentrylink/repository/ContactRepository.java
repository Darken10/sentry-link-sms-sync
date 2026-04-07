package com.africasys.sentrylink.repository;

import android.content.Context;

import com.africasys.sentrylink.dtos.ContactDto;
import com.africasys.sentrylink.models.Contact;
import com.africasys.sentrylink.models.EncryptionKey;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repository pour les contacts et leurs clés de chiffrement.
 *
 * La synchronisation depuis l'API (GET /api/v1/contacts) effectue deux upserts
 * atomiques :
 * <ol>
 * <li>Upsert des contacts (table {@code contact}) — dédup par {@code id} +
 * index unique sur {@code uuid}.</li>
 * <li>Upsert des clés publiques actives (table {@code encryption_key}) — dédup
 * par {@code uuid}.</li>
 * </ol>
 *
 * Règle : un contact ne peut avoir qu'une seule clé ACTIVE à la fois. Avant
 * d'insérer une nouvelle clé ACTIVE, les clés ACTIVE précédentes du contact
 * sont passées à INACTIVE ({@link EncryptionKeyDao#deactivateAllForContact}).
 */
public class ContactRepository {

    private static volatile ContactRepository INSTANCE;
    private final ContactDao contactDao;
    private final EncryptionKeyDao keyDao;

    private ContactRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        contactDao = db.contactDao();
        keyDao = db.encryptionKeyDao();
    }

    public static ContactRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ContactRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ContactRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Synchronisation
    // -------------------------------------------------------------------------

    /**
     * Synchronise la liste des contacts reçus de l'API dans la base locale. Pour
     * chaque contact ayant une clé active, la clé publique est également upsertée.
     *
     * @param dtos liste issue de GET /api/v1/contacts
     */
    public void syncFromApi(List<ContactDto> dtos) {
        if (dtos == null || dtos.isEmpty())
            return;

        long now = System.currentTimeMillis();
        List<Contact> contacts = new ArrayList<>(dtos.size());
        List<EncryptionKey> keys = new ArrayList<>();

        for (ContactDto dto : dtos) {
            // --- Contact ---
            Contact contact = new Contact();
            contact.id = dto.id;
            contact.uuid = dto.uuid != null ? dto.uuid : "";
            contact.name = dto.name;
            contact.identifier = dto.identifier;
            contact.phoneNumber = dto.phoneNumber;
            contact.status = dto.status;
            contact.type = dto.type;
            contact.syncedAt = now;
            contacts.add(contact);

            // --- Clé publique active (si présente dans la réponse) ---
            if (dto.activeKeyUuid != null && !dto.activeKeyUuid.isEmpty() && dto.activePublicKey != null
                    && !dto.activePublicKey.isEmpty()) {
                EncryptionKey key = new EncryptionKey();
                // Dériver un id long unique depuis l'uuid pour éviter le conflit PK (id=0 par
                // défaut).
                UUID parsed = UUID.fromString(dto.activeKeyUuid);
                key.id = parsed.getMostSignificantBits() ^ parsed.getLeastSignificantBits();
                key.uuid = dto.activeKeyUuid;
                key.value = dto.activePublicKey;
                key.status = dto.activeKeyStatus != null ? dto.activeKeyStatus : "ACTIVE";
                key.type = "DEVICE_UNIQUE";
                key.contactId = dto.id;
                key.syncedAt = now;
                keys.add(key);
            }
        }

        // Upsert contacts
        contactDao.upsertAll(contacts);

        // Upsert clés : garantir une seule clé ACTIVE par contact
        for (EncryptionKey key : keys) {
            if ("ACTIVE".equals(key.status) && key.contactId != null) {
                keyDao.deactivateAllForContact(key.contactId);
            }
            keyDao.upsert(key);
        }
    }

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    /** Retourne tous les contacts locaux. */
    public List<Contact> getAll() {
        return contactDao.getAll();
    }

    /** Retourne uniquement les contacts ACTIVE. */
    public List<Contact> getActiveContacts() {
        return contactDao.getActiveContacts();
    }

    /**
     * Retourne la clé publique PEM active d'un contact (identifié par son UUID).
     * Null si le contact n'existe pas ou n'a pas de clé active.
     */
    public String getActivePublicKey(String contactUuid) {
        Contact contact = contactDao.findByUuid(contactUuid);
        if (contact == null)
            return null;
        EncryptionKey key = keyDao.getActiveKeyForContact(contact.id);
        return (key != null) ? key.value : null;
    }

    /**
     * Retourne la clé active d'un contact (identifié par son id serveur).
     */
    public EncryptionKey getActiveKeyForContact(long contactId) {
        return keyDao.getActiveKeyForContact(contactId);
    }

    /** Nombre de contacts en base. */
    public int count() {
        return contactDao.count();
    }
}
