package com.africasys.sentrylink.smssync.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.africasys.sentrylink.smssync.enums.ContactStatus;
import com.africasys.sentrylink.smssync.enums.ContactType;

import java.util.ArrayList;
import java.util.List;

/**
 * Entité Room — contact.
 *
 * Correspond à la table {@code contact} côté serveur.
 * Les clés de chiffrement sont stockées dans {@link EncryptionKey} (relation
 * 1→N).
 *
 * Règle métier : un contact peut avoir plusieurs clés mais une seule ACTIVE à
 * la fois.
 * L'index UNIQUE sur {@code uuid} garantit l'absence de doublons.
 */
@Entity(tableName = "contact", indices = { @Index(value = "uuid", unique = true) })
public class Contact {

    /** Identifiant serveur (bigint). */
    @PrimaryKey
    public long id;

    /** UUID du contact — unique, non null. */
    @NonNull
    public String uuid = "";

    /** Nom complet. */
    public String name;

    /** Numéro de téléphone. */
    @ColumnInfo(name = "phone_number")
    public String phoneNumber;

    /** Identifiant métier (ex: CONT-001). */
    public String identifier;

    /**
     * Type du contact.
     * Valeurs : {@link ContactType#CENTRAL_SERVER}, {@link ContactType#UNIT}
     */
    public String type;

    /**
     * Statut du contact.
     * Valeurs : {@link ContactStatus#ACTIVE}, {@link ContactStatus#SUSPENDED},
     * {@link ContactStatus#DESTROYED}, {@link ContactStatus#AUTO_DESTROYED},
     * {@link ContactStatus#INACTIVE}
     */
    public String status;

    /** Horodatage de la dernière synchronisation (ms). */
    @ColumnInfo(name = "synced_at")
    public long syncedAt;

    /**
     * Indique si le contact est enregistré dans la base de données de production.
     * {@code false} pour les contacts ajoutés hors répertoire (numéro saisi
     * manuellement).
     */
    @ColumnInfo(name = "is_in_directory", defaultValue = "1")
    public boolean isInDirectory = true;

    @NonNull
    @Override
    public String toString() {
        return "Contact {" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", identifier='" + identifier + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", syncedAt=" + syncedAt +
                ", isInDirectory=" + isInDirectory +
                '}';
    }

}
