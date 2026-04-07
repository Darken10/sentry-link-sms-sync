package com.africasys.sentrylink.config;

import com.africasys.sentrylink.enums.EncryptionAlgo;

public final class CryptoConfig {

    public static final EncryptionAlgo USED_ENCRYPTION_ALGORITHM = EncryptionAlgo.ECC_ONLY;

    // Algorithmes de base
    public static final String KEY_TYPE = "EC";
    public static final String AGREEMENT_ALGO = "ECDH";
    public static final String SYMMETRIC_ALGO = "AES/GCM/NoPadding";
    public static final String SYMMETRIC_KEY_TYPE = "AES";

    // Paramètres techniques
    //public static final String EC_CURVE = "secp256r1";
    public static final String EC_CURVE = "secp384r1";
    public static final int AES_KEY_SIZE_BYTES = 16; // 128 bits pour l'efficacité SMS
    public static final int GCM_IV_LENGTH = 12;      // Recommandé pour GCM
    public static final int GCM_TAG_LENGTH = 128;    // Longueur du tag d'authentification

    // Encodage
    public static final String CHARSET = "UTF-8";


    // Nouvelles constantes RSA 512
    public static final String RSA_ALGO = "RSA/ECB/PKCS1Padding";
    public static final String RSA_KEY_TYPE = "RSA";
    public static final int RSA_512_BLOCK_SIZE = 64;// Nouvelles constantes RSA 512

    public static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    public static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    public static final int AES_KEY_SIZE = 256;


    private CryptoConfig() {} // Empêche l'instanciation

}