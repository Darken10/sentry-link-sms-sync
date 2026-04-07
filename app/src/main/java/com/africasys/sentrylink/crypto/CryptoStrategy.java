package com.africasys.sentrylink.crypto;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

public interface CryptoStrategy {
    /**
     * @param plainText Le message en clair
     * @param key La clé publique du destinataire
     * @return Le message chiffré et encodé pour le SMS
     */
    String encrypt(String plainText, Key key) throws Exception;

    /**
     * @param encryptedText Le texte reçu par SMS
     * @param key Votre clé privée
     * @return Le message déchiffré
     */
    String decrypt(String encryptedText, Key key) throws Exception;
}