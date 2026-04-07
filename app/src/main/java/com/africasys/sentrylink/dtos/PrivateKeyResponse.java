package com.africasys.sentrylink.dtos;

/**
 * DTO — réponse de GET /api/v1/my-private-key.
 *
 * La clé privée est chiffrée en AES-256-GCM par le serveur.
 * Format : {@code base64url(iv):base64url(ciphertext+tag)}
 * Clé de déchiffrement : SHA-256(credential) où credential = SHA-512(rawToken).
 */
public class PrivateKeyResponse {
    public String encryptedPrivateKey;
}
