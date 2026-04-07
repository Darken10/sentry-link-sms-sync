package com.africasys.sentrylink.smssync.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.africasys.sentrylink.smssync.enums.KeyStatus;
import com.africasys.sentrylink.smssync.enums.KeyType;

/**
 * Entité Room — clé de chiffrement RSA-4096.
 *
 * Correspond à la table {@code encryption_key} côté serveur.
 *
 * Relation : N clés → 1 contact (FK sur {@code contact_id}).
 * Un contact peut avoir plusieurs clés mais une seule ACTIVE à la fois.
 *
 * Le champ {@code uuid} est l'identifiant universel unique de la clé.
 * Le champ {@code value} contient la clé publique (PEM).
 * Le champ {@code private_key} contient la clé privée déchiffrée (stockée localement).
 */
@Entity(
    tableName = "encryption_key",
    indices = {
        @Index(value = "uuid", unique = true),
        @Index(value = "contact_id")
    },
    foreignKeys = {
        @ForeignKey(
            entity = Contact.class,
            parentColumns = "id",
            childColumns = "contact_id",
            onDelete = ForeignKey.CASCADE
        )
    }
)
public class EncryptionKey {

    /**
     * Identifiant serveur (bigint).
     * Peut être 0 si non encore connu (clé construite localement depuis le DTO contact).
     */
    @PrimaryKey
    public long id;

    /** UUID unique de la clé (36 caractères). */
    @NonNull
    public String uuid = "";

    /**
     * Clé publique RSA-4096 au format PEM.
     * Null pour les clés de type DEVICE_UNIQUE non encore publiées.
     */
    public String value;

    /**
     * Clé privée RSA-4096 déchiffrée (stockée uniquement en local, jamais envoyée au serveur).
     * Renseigné via GET /api/v1/my-private-key après déchiffrement AES-256-GCM.
     */
    @ColumnInfo(name = "private_key")
    public String privateKey;

    /**
     * Type de la clé.
     * Valeurs : {@link KeyType#DEVICE_UNIQUE}, {@link KeyType#UNIVERSAL}
     */
    @NonNull
    public String type = "";

    /**
     * Statut de la clé.
     * Valeurs : {@link KeyStatus#ACTIVE}, {@link KeyStatus#EXPIRED}, {@link KeyStatus#INACTIVE}
     */
    @NonNull
    public String status = "";

    /**
     * Date d'expiration en millisecondes (epoch).
     * Null si la clé n'a pas de date d'expiration.
     */
    @ColumnInfo(name = "expires_at")
    public Long expiresAt;

    /**
     * Identifiant du contact propriétaire.
     * Null pour les clés universelles ({@link KeyType#UNIVERSAL}) sans contact attaché.
     */
    @ColumnInfo(name = "contact_id")
    public Long contactId;

    /** Horodatage de la dernière synchronisation (ms). */
    @ColumnInfo(name = "synced_at")
    public long syncedAt;


    @NonNull
    public String toString() {
        return "EncryptionKey {" +
                "id=" + id +
                ", uuid='" + uuid +
                ", value='" + value +
                ", privateKey='" + privateKey +
                ", type='" + type +
                ", status='" + status +
                ", expiresAt=" + expiresAt +
                ", contactId=" + contactId +
                ", syncedAt=" + syncedAt +
                '}';
    }
}
