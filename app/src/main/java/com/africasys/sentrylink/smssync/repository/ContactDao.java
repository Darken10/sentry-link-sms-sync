package com.africasys.sentrylink.smssync.repository;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.africasys.sentrylink.smssync.models.Contact;

import java.util.List;

/**
 * DAO pour les contacts.
 * Upsert par {@code OnConflictStrategy.REPLACE} sur la clé primaire
 * ({@code id}).
 * L'index UNIQUE sur {@code uuid} prévient les doublons.
 */
@Dao
public interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<Contact> contacts);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(Contact contact);

    @Query("SELECT * FROM contact ORDER BY name ASC")
    List<Contact> getAll();

    @Query("SELECT * FROM contact WHERE status = 'ACTIVE' ORDER BY name ASC")
    List<Contact> getActiveContacts();

    @Query("SELECT * FROM contact WHERE uuid = :uuid LIMIT 1")
    Contact findByUuid(String uuid);

    @Query("SELECT * FROM contact WHERE identifier = :identifier LIMIT 1")
    Contact findByIdentifier(String identifier);

    @Query("SELECT * FROM contact WHERE id = :id LIMIT 1")
    Contact findById(long id);

    @Query("SELECT COUNT(*) FROM contact")
    int count();

    /** Recherche un contact par numéro de téléphone (correspondance exacte). */
    @Query("SELECT * FROM contact WHERE phone_number = :phoneNumber LIMIT 1")
    Contact findByPhoneNumber(String phoneNumber);

    /** Retourne les tours de contrôle actives (type CENTRAL_SERVER). */
    @Query("SELECT * FROM contact WHERE type = 'CENTRAL_SERVER' AND status = 'ACTIVE'")
    List<Contact> getActiveCentralServers();
}
