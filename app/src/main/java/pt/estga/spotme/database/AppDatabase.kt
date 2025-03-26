package pt.estga.spotme.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.content.Context;

import pt.estga.spotme.entities.Parking;
import pt.estga.spotme.entities.User;

@Database(entities = {Parking.class, User.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract ParkingDao parkingDao();
    public abstract UserDao userDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "spotme_database")
                    // .addMigrations(MIGRATION_1_2)
                    // .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

//    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database) {
//            database.execSQL("ALTER TABLE User ADD COLUMN phone TEXT");
//        }
//    };

}