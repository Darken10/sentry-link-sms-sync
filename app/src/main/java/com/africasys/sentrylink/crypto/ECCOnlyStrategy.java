package com.africasys.sentrylink.crypto;

import android.util.Base64;

import com.africasys.sentrylink.config.CryptoConfig;

import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Key;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Chiffrement asymétrique ECC (ECIES).
 *
 * - Chiffrement : clé publique EC du destinataire
 * - Déchiffrement : clé privée EC du destinataire
 *
 * Algorithme interne (ECIES) :
 *   1. Génère une paire éphémère EC (même courbe que la clé du destinataire)
 *   2. ECDH(clé_privée_éphémère, clé_publique_destinataire) → shared secret
 *   3. SHA-256(shared secret) → clé AES-256
 *   4. AES-256-GCM → chiffrement du message
 *
 * Format paquet Base64 :
 *   [4 octets taille clé éphémère][clé pub éphémère X.509][ciphertext + tag GCM]
 */
public class ECCOnlyStrategy implements CryptoStrategy {

    @Override
    public String encrypt(String plainText, Key recipientPublicKey) throws Exception {
        if (!(recipientPublicKey instanceof PublicKey)) {
            throw new IllegalArgumentException("Clé publique EC requise pour le chiffrement.");
        }

        // 1. Paire éphémère EC (même courbe que la clé du destinataire)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(CryptoConfig.KEY_TYPE);
        kpg.initialize(new ECGenParameterSpec(CryptoConfig.EC_CURVE));
        KeyPair ephemeral = kpg.generateKeyPair();

        // 2. ECDH : shared secret entre clé privée éphémère et clé publique destinataire
        KeyAgreement ecdh = KeyAgreement.getInstance(CryptoConfig.AGREEMENT_ALGO);
        ecdh.init(ephemeral.getPrivate());
        ecdh.doPhase(recipientPublicKey, true);
        byte[] sharedSecret = ecdh.generateSecret();

        // 3. KDF : SHA-256(shared secret) → clé AES-256
        byte[] aesKeyBytes = MessageDigest.getInstance("SHA-256").digest(sharedSecret);
        SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, CryptoConfig.SYMMETRIC_KEY_TYPE);

        // 4. AES-256-GCM : IV=0 sécurisé car la clé AES change à chaque message
        byte[] iv = new byte[CryptoConfig.GCM_IV_LENGTH];
        Cipher cipher = Cipher.getInstance(CryptoConfig.SYMMETRIC_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(CryptoConfig.GCM_TAG_LENGTH, iv));
        byte[] cipherText = cipher.doFinal(plainText.getBytes(CryptoConfig.CHARSET));

        // 5. Paquet : [taille(4b)][clé éphémère][ciphertext+tag]
        byte[] ephemeralPubKeyBytes = ephemeral.getPublic().getEncoded();
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + ephemeralPubKeyBytes.length + cipherText.length);
        buffer.putInt(ephemeralPubKeyBytes.length);
        buffer.put(ephemeralPubKeyBytes);
        buffer.put(cipherText);

        return Base64.encodeToString(buffer.array(), Base64.NO_WRAP);
    }

    @Override
    public String decrypt(String encryptedText, Key myPrivateKey) throws Exception {
        if (!(myPrivateKey instanceof PrivateKey)) {
            throw new IllegalArgumentException("Clé privée EC requise pour le déchiffrement.");
        }

        byte[] data = Base64.decode(encryptedText, Base64.NO_WRAP);
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // 1. Extraction de la clé publique éphémère
        int keyLen = buffer.getInt();
        byte[] ephemeralPubKeyBytes = new byte[keyLen];
        buffer.get(ephemeralPubKeyBytes);
        PublicKey ephemeralPubKey = KeyFactory.getInstance(CryptoConfig.KEY_TYPE)
                .generatePublic(new X509EncodedKeySpec(ephemeralPubKeyBytes));

        // 2. ECDH : shared secret entre clé privée destinataire et clé publique éphémère
        KeyAgreement ecdh = KeyAgreement.getInstance(CryptoConfig.AGREEMENT_ALGO);
        ecdh.init(myPrivateKey);
        ecdh.doPhase(ephemeralPubKey, true);
        byte[] sharedSecret = ecdh.generateSecret();

        // 3. KDF : SHA-256(shared secret) → clé AES-256
        byte[] aesKeyBytes = MessageDigest.getInstance("SHA-256").digest(sharedSecret);
        SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, CryptoConfig.SYMMETRIC_KEY_TYPE);

        // 4. AES-256-GCM déchiffrement
        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);
        Cipher cipher = Cipher.getInstance(CryptoConfig.SYMMETRIC_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, aesKey,
                new GCMParameterSpec(CryptoConfig.GCM_TAG_LENGTH, new byte[CryptoConfig.GCM_IV_LENGTH]));

        return new String(cipher.doFinal(cipherText), CryptoConfig.CHARSET);
    }
}
