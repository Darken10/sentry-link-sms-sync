package com.africasys.sentrylink.dtos;

/**
 * DTO — réponse de GET /api/v1/contacts.
 *
 * Contient les informations du contact et sa clé publique RSA-4096 active (si présente).
 * La clé privée n'est jamais retournée par cet endpoint.
 */
public class ContactDto {
    /** Identifiant serveur (bigint). */
    public long id;
    public String uuid;
    public String name;
    public String identifier;
    public String phoneNumber;
    /** Valeurs : ACTIVE, SUSPENDED, DESTROYED, AUTO_DESTROYED, INACTIVE */
    public String status;
    /** Valeurs : CENTRAL_SERVER, UNIT */
    public String type;

    // --- Clé publique active (null si aucune clé active) ---

    /** UUID de la clé publique active. */
    public String activeKeyUuid;
    /** Clé publique RSA-4096 au format PEM. */
    public String activePublicKey;
    /** Statut de la clé active (ACTIVE, EXPIRED, INACTIVE). */
    public String activeKeyStatus;
}
