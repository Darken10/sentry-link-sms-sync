package com.africasys.sentrylink.smssync.repository;

import android.content.Context;

import com.africasys.sentrylink.smssync.dtos.ContactAuthResponse;
import com.africasys.sentrylink.smssync.models.AuthenticatedUser;

/**
 * Repository pour l'utilisateur authentifié.
 * Source de vérité unique pour les données du contact connecté.
 */
public class UserRepository {

    private static volatile UserRepository INSTANCE;
    private final UserDao userDao;

    private UserRepository(Context context) {
        userDao = AppDatabase.getInstance(context).userDao();
    }

    public static UserRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (UserRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UserRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Persiste l'utilisateur issu de la réponse d'authentification.
     * createdAt est fixé à la première connexion ; updatedAt est mis à jour à chaque appel.
     */
    public void saveUser(ContactAuthResponse response) {
        AuthenticatedUser existing = getCurrentUser();
        long now = System.currentTimeMillis();

        AuthenticatedUser user = new AuthenticatedUser();
        user.contactId = response.contactId;
        user.name = response.name;
        user.identifier = response.identifier;
        user.status = response.status;
        user.credential = response.credential;
        user.createdAt = (existing != null) ? existing.createdAt : now;
        user.updatedAt = now;

        userDao.insert(user);
    }

    public AuthenticatedUser getCurrentUser() {
        return userDao.getCurrentUser();
    }

    public boolean isAuthenticated() {
        AuthenticatedUser user = getCurrentUser();
        return user != null && "ACTIVE".equals(user.status);
    }

    public void clearUser() {
        userDao.deleteAll();
    }
}
