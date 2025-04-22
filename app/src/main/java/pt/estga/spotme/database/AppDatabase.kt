package pt.estga.spotme.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import pt.estga.spotme.entities.AppRating
import pt.estga.spotme.entities.Parking
import pt.estga.spotme.entities.User

@Database(entities = [Parking::class, User::class, AppRating::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parkingDao(): ParkingDao
    abstract fun userDao(): UserDao
    abstract fun appRatingDao(): AppRatingDao

    companion object {
        private var instance: AppDatabase? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): AppDatabase {
            if (instance == null) {
                instance = databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "spotme_database"
                ) // .addMigrations(MIGRATION_1_2)
                    // .fallbackToDestructiveMigration()
                    .build()
            }
            return instance!!
        } //    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        //        @Override
        //        public void migrate(@NonNull SupportSQLiteDatabase database) {
        //            database.execSQL("ALTER TABLE User ADD COLUMN phone TEXT");
        //        }
        //    };
    }
}