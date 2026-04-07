package com.africasys.sentrylink.repository;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.africasys.sentrylink.models.EncryptionKey;

import java.util.List;

/**
 * DAO pour les clés de chiffrement.
 *
 * L'upsert par {@code OnConflictStrategy.REPLACE} sur la clé primaire
 * ({@code id}) permet de mettre à jour une clé existante (ex: passage de ACTIVE
 * à EXPIRED). L'index UNIQUE sur {@code uuid} empêche les doublons par UUID.
 */
@Dao
public interface EncryptionKeyDao {

    /** Insère ou remplace une clé. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(EncryptionKey key);

    /** Insère ou remplace un lot de clés. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<EncryptionKey> keys);

    /**
     * Retourne la clé ACTIVE d'un contact. Un contact ne peut avoir qu'une seule
     * clé ACTIVE à la fois.
     */
    @Query("SELECT * FROM encryption_key WHERE contact_id = :contactId AND status = 'ACTIVE' LIMIT 1")
    EncryptionKey getActiveKeyForContact(long contactId);

    /** Retourne toutes les clés d'un contact, triées par statut. */
    @Query("SELECT * FROM encryption_key WHERE contact_id = :contactId ORDER BY status ASC")
    List<EncryptionKey> getKeysForContact(long contactId);

    /** Retourne une clé par son UUID. */
    @Query("SELECT * FROM encryption_key WHERE uuid = :uuid LIMIT 1")
    EncryptionKey findByUuid(String uuid);

    /**
     * Désactive toutes les clés ACTIVE d'un contact avant d'en activer une
     * nouvelle. Utilisé pour garantir qu'un contact n'a qu'une seule clé ACTIVE à
     * la fois.
     */
    @Query("UPDATE encryption_key SET status = 'INACTIVE' WHERE contact_id = :contactId AND status = 'ACTIVE'")
    void deactivateAllForContact(long contactId);

    /**
     * Retourne toutes les clés universelles (partagées par défaut) actives.
     * Utilisé comme fallback quand le contact n'a pas de clé dédiée.
     */
    @Query("SELECT * FROM encryption_key WHERE type = 'UNIVERSAL' AND status = 'ACTIVE'")
    List<EncryptionKey> getActiveUniversalKeys();

    /** Retourne la clé privée du contact courant (la seule stockée localement). */
    @Query("SELECT * FROM encryption_key WHERE private_key IS NOT NULL LIMIT 1")
    EncryptionKey getLocalPrivateKey();

    /**
     * Retourne la clé privée par défaut du contact courant (la seule stockée
     * localement).
     */
    @Query("SELECT * FROM encryption_key WHERE private_key IS NOT NULL LIMIT 1")
    EncryptionKey getDefaultPrivateKey();

    /**
     * Retourne la clé publique par défaut du contact courant (la seule stockée
     * localement).
     */
    @Query("SELECT * FROM encryption_key WHERE value IS NOT NULL LIMIT 1")
    EncryptionKey getDefaultPublicKey();

}
