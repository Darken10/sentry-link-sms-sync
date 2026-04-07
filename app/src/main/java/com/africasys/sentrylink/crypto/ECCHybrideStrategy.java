package com.africasys.sentrylink.crypto;

import com.africasys.sentrylink.config.CryptoConfig;
import java.security.*;
import javax.crypto.Cipher;
import android.util.Base64;
import java.nio.ByteBuffer;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * ECIES — chiffrement asymétrique ECC.
 *
 * Algorithme :
 *   1. Génération d'une paire éphémère EC (même courbe que la clé du destinataire)
 *   2. ECDH → shared secret
 *   3. SHA-256 du shared secret → clé AES-256 (KDF minimal)
 *   4. AES-256-GCM (IV=0, sécurisé car clé différente à chaque message)
 *
 * Format du paquet SMS :
 *   [4 octets : taille clé éphémère][clé pub éphémère][ciphertext+tag GCM] → Base64
 */
public class ECCHybrideStrategy implements CryptoStrategy {

    @Override
    public String encrypt(String plainText, Key recipientKey) throws Exception {
        if (!(recipientKey instanceof PublicKey)) {
            throw new IllegalArgumentException("Une clé publique EC est requise pour le chiffrement.");
        }

        // 1. Paire éphémère sur la même courbe que la clé du destinataire
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(CryptoConfig.KEY_TYPE);
        kpg.initialize(new ECGenParameterSpec(CryptoConfig.EC_CURVE));
        KeyPair ephemeralPair = kpg.generateKeyPair();

        // 2. Accord de clé ECDH
        KeyAgreement agreement = KeyAgreement.getInstance(CryptoConfig.AGREEMENT_ALGO);
        agreement.init(ephemeralPair.getPrivate());
        agreement.doPhase(recipientKey, true);
        byte[] sharedSecret = agreement.generateSecret();

        // 3. KDF : SHA-256 du shared secret → clé AES-256
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] aesKeyBytes = sha256.digest(sharedSecret);
        SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, CryptoConfig.SYMMETRIC_KEY_TYPE);

        // 4. AES-256-GCM (IV=0 sécurisé car clé éphémère unique par message)
        byte[] iv = new byte[CryptoConfig.GCM_IV_LENGTH];
        Cipher cipher = Cipher.getInstance(CryptoConfig.SYMMETRIC_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(CryptoConfig.GCM_TAG_LENGTH, iv));
        byte[] cipherText = cipher.doFinal(plainText.getBytes(CryptoConfig.CHARSET));

        // 5. Construction du paquet : [taille(4b)][clé éphémère][ciphertext]
        byte[] ephemeralPubKey = ephemeralPair.getPublic().getEncoded();
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + ephemeralPubKey.length + cipherText.length);
        buffer.putInt(ephemeralPubKey.length);
        buffer.put(ephemeralPubKey);
        buffer.put(cipherText);

        return Base64.encodeToString(buffer.array(), Base64.NO_WRAP);
    }

    @Override
    public String decrypt(String encryptedText, Key myPrivateKey) throws Exception {
        if (!(myPrivateKey instanceof PrivateKey)) {
            throw new IllegalArgumentException("Une clé privée EC est requise pour le déchiffrement.");
        }

        byte[] data = Base64.decode(encryptedText, Base64.NO_WRAP);
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // 1. Extraction de la clé éphémère
        int keyLen = buffer.getInt();
        byte[] pubKeyBytes = new byte[keyLen];
        buffer.get(pubKeyBytes);
        PublicKey ephemeralPubKey = KeyFactory.getInstance(CryptoConfig.KEY_TYPE)
                .generatePublic(new X509EncodedKeySpec(pubKeyBytes));

        // 2. ECDH
        KeyAgreement agreement = KeyAgreement.getInstance(CryptoConfig.AGREEMENT_ALGO);
        agreement.init(myPrivateKey);
        agreement.doPhase(ephemeralPubKey, true);
        byte[] sharedSecret = agreement.generateSecret();

        // 3. KDF : SHA-256 du shared secret → clé AES-256
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] aesKeyBytes = sha256.digest(sharedSecret);
        SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, CryptoConfig.SYMMETRIC_KEY_TYPE);

        // 4. AES-256-GCM déchiffrement
        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);
        Cipher cipher = Cipher.getInstance(CryptoConfig.SYMMETRIC_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(CryptoConfig.GCM_TAG_LENGTH, new byte[CryptoConfig.GCM_IV_LENGTH]));

        return new String(cipher.doFinal(cipherText), CryptoConfig.CHARSET);
    }
}
