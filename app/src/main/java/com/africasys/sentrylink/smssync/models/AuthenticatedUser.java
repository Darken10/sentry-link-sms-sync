package com.africasys.sentrylink.smssync.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Utilisateur authentifié via QR code.
 * Le credential (SHA-512 du token brut) est le jeton utilisé pour tous les appels API.
 */
@Entity(tableName = "authenticated_users")
public class AuthenticatedUser {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int contactId;
    public String name;
    public String identifier;
    public String status;

    /** Jeton SHA-512 utilisé pour tous les appels API. */
    public String credential;

    /** Timestamp de création (ms depuis epoch). */
    public long createdAt;

    /** Timestamp de dernière mise à jour (ms depuis epoch). */
    public long updatedAt;
}
