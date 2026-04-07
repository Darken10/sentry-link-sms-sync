package com.africasys.sentrylink.smssync.dtos;

/**
 * DTO — réponse de GET /api/v1/default-public-key.
 *
 * Clé publique RSA-4096 par défaut, utilisée pour chiffrer les messages
 * destinés à un numéro non enregistré dans la base de contacts.
 */
public class DefaultKeyResponse {
    /** Clé publique RSA-4096 au format PEM. */
    public String publicKey;
    /** UUID de la clé par défaut. */
    public String keyUuid;
}
