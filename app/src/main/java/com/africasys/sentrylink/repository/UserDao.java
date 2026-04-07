package com.africasys.sentrylink.repository;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.africasys.sentrylink.models.AuthenticatedUser;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AuthenticatedUser user);

    /** Retourne l'utilisateur actuellement authentifié (le plus récent). */
    @Query("SELECT * FROM authenticated_users ORDER BY createdAt DESC LIMIT 1")
    AuthenticatedUser getCurrentUser();

    @Query("DELETE FROM authenticated_users")
    void deleteAll();
}
