package com.africasys.sentrylink.smssync.crypto;

import android.util.Base64;

import com.africasys.sentrylink.smssync.config.CryptoConfig;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.Cipher;

public class RSA512CryptoStrategy implements CryptoStrategy {

        @Override
        public String encrypt(String plainText, Key recipientKey) throws Exception {
            if (!(recipientKey instanceof PublicKey)) {
                throw new IllegalArgumentException("Clé publique requise");
            }

            Cipher cipher = Cipher.getInstance(CryptoConfig.RSA_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, recipientKey);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(CryptoConfig.CHARSET));

            // Résultat : exactement 64 octets
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
        }

        @Override
        public String decrypt(String encryptedText, Key myPrivateKey) throws Exception {
            if (!(myPrivateKey instanceof PrivateKey)) {
                throw new IllegalArgumentException("Clé privée requise");
            }

            byte[] decodedBytes = Base64.decode(encryptedText, Base64.NO_WRAP);

            Cipher cipher = Cipher.getInstance(CryptoConfig.RSA_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, myPrivateKey);

            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, CryptoConfig.CHARSET);
        }
}
