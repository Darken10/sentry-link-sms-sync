package com.africasys.sentrylink.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.africasys.sentrylink.models.AuthenticatedUser;
import com.africasys.sentrylink.models.Contact;
import com.africasys.sentrylink.models.EncryptionKey;
import com.africasys.sentrylink.models.LocationRecord;
import com.africasys.sentrylink.models.SMSMessage;
import com.africasys.sentrylink.models.SosAlert;

@Database(
    entities = {SMSMessage.class, LocationRecord.class, SosAlert.class, AuthenticatedUser.class, Contact.class, EncryptionKey.class},
    version = 6,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    /** Migration v5 → v6 : ajout du champ is_in_directory dans la table contact. */
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "ALTER TABLE contact ADD COLUMN is_in_directory INTEGER NOT NULL DEFAULT 1"
            );
        }
    };

    public abstract SmsDao smsDao();
    public abstract LocationDao locationDao();
    public abstract SosDao sosDao();
    public abstract UserDao userDao();
    public abstract ContactDao contactDao();
    public abstract EncryptionKeyDao encryptionKeyDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // TODO: réactiver SQLCipher une fois le problème de chargement natif résolu
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "smssync_database"
                    )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
