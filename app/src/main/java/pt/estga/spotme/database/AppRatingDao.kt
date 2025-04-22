package pt.estga.spotme.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import pt.estga.spotme.entities.AppRating

@Dao
interface AppRatingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appRating: AppRating): Long

    @Update
    suspend fun update(appRating: AppRating)

    @Query("SELECT * FROM app_ratings WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRatingByUser(userId: Long): LiveData<AppRating?>

    @Query("SELECT * FROM app_ratings WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRatingByUserSync(userId: Long): AppRating?

    @Query("SELECT * FROM app_ratings WHERE isSubmittedToServer = 0")
    fun getPendingRatings(): LiveData<List<AppRating>>

    @Query("DELETE FROM app_ratings WHERE userId = :userId")
    suspend fun deleteRatingsByUser(userId: Long)
}