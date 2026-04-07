package com.africasys.sentrylink.smssync.crypto;

import android.util.Base64;
import android.util.Log;

import com.africasys.sentrylink.smssync.config.CryptoConfig;

import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class CryptoManager {
    private CryptoStrategy strategy;
    private static final String TAG = "SL-CryptoManager";

    public CryptoManager() {
        switch (CryptoConfig.USED_ENCRYPTION_ALGORITHM) {
            case ECC_HYBRID:
                this.strategy = new ECCHybrideStrategy();
                break;
            case ECC_ONLY:
                this.strategy = new ECCOnlyStrategy();
                break;
            case RSA_512_ONLY:
                this.strategy = new RSA512CryptoStrategy();
                break;
            default:
                throw new IllegalStateException("Unsupported encryption algorithm");
        }
    }

    public String prepareSms(String message, Key publicKey) throws Exception {
        return strategy.encrypt(message, publicKey);
    }

    public String receiveSms(String smsContent, Key privateKey) throws Exception {
        return strategy.decrypt(smsContent, privateKey);
    }

    public static PublicKey stringToPublicKey(String publicKeyString) throws Exception {
        Log.d(TAG, "publicKeyString: " + publicKeyString);
        // 1. Nettoyer le String (enlever les headers PEM si présents)
        String cleanKey = publicKeyString
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        // 2. Décoder le Base64 en octets
        byte[] keyBytes = Base64.decode(cleanKey, Base64.DEFAULT);

        // 3. Reconstruire la clé : essaie l'algo configuré en premier, puis le fallback.
        //    Permet de ne pas crasher si la clé en base ne correspond pas à l'algo actuel.
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        String preferred = getAlgorithmToTransformKey();
        String fallback = preferred.equals("RSA") ? "EC" : "RSA";
        try {
            return KeyFactory.getInstance(preferred).generatePublic(spec);
        } catch (Exception e) {
            Log.w(TAG, "Échec chargement clé avec algo '" + preferred + "', tentative avec '" + fallback + "'", e);
        }
        try {
            return KeyFactory.getInstance(fallback).generatePublic(spec);
        } catch (Exception e) {
            Log.e(TAG, "Échec chargement clé avec algo '" + fallback + "'", e);
        }
        throw new InvalidKeySpecException("Impossible de parser la clé publique (RSA et EC échoués)");
    }


    public static PrivateKey stringToPrivateKey(String privateKeyString) throws Exception {
        byte[] keyBytes = Base64.decode(privateKeyString, Base64.DEFAULT);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        String algorithm = getAlgorithmToTransformKey();
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        return kf.generatePrivate(spec);
    }

    private static String getAlgorithmToTransformKey() {
        switch (CryptoConfig.USED_ENCRYPTION_ALGORITHM) {
            case ECC_HYBRID:
            case ECC_ONLY:
                return "EC";
            case RSA_512_ONLY:
                return "RSA";
            default:
                throw new IllegalStateException("Unsupported encryption algorithm");
        }
    }

}
