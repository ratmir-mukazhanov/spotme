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

    @Insert
    fun insert(parking: Parking)

    @Update
    fun update(parking: Parking)

    @Delete
    fun delete(parking: Parking)
}
