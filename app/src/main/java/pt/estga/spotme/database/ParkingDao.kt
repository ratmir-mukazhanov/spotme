package pt.estga.spotme.database

import androidx.room.*
import pt.estga.spotme.entities.Parking

@Dao
interface ParkingDao {

    @Query("SELECT * FROM parking")
    fun getAll(): List<Parking>

    @Query("SELECT * FROM parking WHERE userId = :userId")
    fun getParkingsByUserId(userId: Long): List<Parking>

    @Query("SELECT * FROM parking WHERE id = :id")
    fun getById(id: Int): Parking?

    @Query("SELECT * FROM parking WHERE endTime IS NULL LIMIT 1")
    fun getCurrent(): Parking?

    @Query("SELECT * FROM parking WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    fun getLastParkingByUserId(userId: Long): Parking?

    @Query("SELECT * FROM parking WHERE userId = :userId ORDER BY id DESC LIMIT :limit OFFSET :offset")
    fun getParkingsByUserIdWithLimit(userId: Long, offset: Int, limit: Int): List<Parking>

    @Query("SELECT COUNT(*) FROM parking WHERE userId = :userId")
    fun getParkingCountByUserId(userId: Long): Int

    @Query("SELECT AVG(CASE WHEN endTime > 0 THEN endTime - startTime ELSE allowedTime END) FROM parking WHERE userId = :userId AND (endTime > 0 OR allowedTime > 0)")
    fun getAverageParkingTimeByUserId(userId: Long): Long?

    @Query("SELECT * FROM parking WHERE userId = :userId AND startTime >= :timestamp")
    fun getParkingsByUserIdAfterTimestamp(userId: Long, timestamp: Long): List<Parking>

    @Query("DELETE FROM parking WHERE userId = :userId")
    suspend fun deleteParkingsByUserId(userId: Long): Int

    @Insert
    fun insert(parking: Parking)

    @Update
    fun update(parking: Parking)

    @Delete
    fun delete(parking: Parking)
}
