package pt.estga.spotme.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import pt.estga.spotme.entities.Parking
import java.util.Optional

@Dao
interface ParkingDao {
    @get:Query("SELECT * FROM parking")
    val all: List<Parking?>?

    @Query("SELECT * FROM parking WHERE userId = :userId")
    fun getParkingsByUserId(userId: Long): List<Parking?>?

    @Query("SELECT * FROM parking WHERE id = :id")
    fun getById(id: Int): Parking?

    @get:Query("SELECT * FROM parking WHERE endTime IS NULL")
    val current: Optional<Parking?>?

    @Query("SELECT * FROM parking WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    fun getLastParkingByUserId(userId: Long): Parking?

    @Insert
    fun insert(parking: Parking)

    @Update
    fun update(parking: Parking)

    @Delete
    fun delete(parking: Parking)

    @Query("SELECT * FROM parking WHERE userId = :userId ORDER BY id DESC LIMIT :limit OFFSET :offset")
    fun getParkingsByUserIdWithLimit(userId: Long, offset: Int, limit: Int): List<Parking?>?
}
