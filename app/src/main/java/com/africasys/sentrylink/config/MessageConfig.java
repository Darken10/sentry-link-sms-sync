package com.africasys.sentrylink.config;

public final class MessageConfig {
    private MessageConfig() {
    }

    /**
     * Préfixe pour les messages chiffrés avec la clé personnelle du destinataire.
     */
    public static final String PERSONAL_KEY_PREFIX = "SL0";

    /**
     * Préfixe pour les messages chiffrés avec la clé partagée de l'unité
     * (fallback).
     */
    public static final String SHARED_KEY_PREFIX = "SL1";

    /**
     * @deprecated Utiliser {@link #PERSONAL_KEY_PREFIX} ou
     *             {@link #SHARED_KEY_PREFIX}.
     */
    @Deprecated
    public static final String MESSAGE_PREFIX = SHARED_KEY_PREFIX;

    public static final String MESSAGE_SEPARATOR = "::";
    public static final String MESSAGE_HEADER_SEPARATOR = ";";

    /** Longueur fixe des préfixes SL0 / SL1. */
    public static final int PREFIX_LENGTH = 3;

}
