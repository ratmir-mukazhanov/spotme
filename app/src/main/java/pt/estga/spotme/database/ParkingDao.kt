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

    @Query("SELECT COUNT(*) FROM parking WHERE userId = :userId AND startTime >= :timestamp")
    fun getParkingCountByUserIdAfterTimestamp(userId: Long, timestamp: Long): Int

    @Query("SELECT * FROM parking WHERE userId = :userId")
    fun getUserParkingsById(userId: Long): List<Parking>

    // Métodos para estatísticas da semana atual (últimos 7 dias)
    @Query("SELECT COUNT(*) FROM parking WHERE userId = :userId AND startTime >= :startOfWeek")
    fun getWeeklyParkingCount(userId: Long, startOfWeek: Long): Int

    @Query("SELECT AVG(allowedTime) FROM parking WHERE userId = :userId AND startTime >= :startOfWeek AND allowedTime > 0")
    fun getWeeklyParkingTimeAvg(userId: Long, startOfWeek: Long): Long?

    // Métodos para estatísticas do mês atual (últimos 30 dias)
    @Query("SELECT COUNT(*) FROM parking WHERE userId = :userId AND startTime >= :startOfMonth")
    fun getMonthlyParkingCount(userId: Long, startOfMonth: Long): Int

    @Query("SELECT AVG(allowedTime) FROM parking WHERE userId = :userId AND startTime >= :startOfMonth AND allowedTime > 0")
    fun getMonthlyParkingTimeAvg(userId: Long, startOfMonth: Long): Long?

    // Métodos para estatísticas gerais (all time)
    @Query("SELECT COUNT(*) FROM parking WHERE userId = :userId")
    fun getAllTimeParkingCount(userId: Long): Int

    @Query("SELECT AVG(allowedTime) FROM parking WHERE userId = :userId AND allowedTime > 0")
    fun getAllTimeParkingTimeAvg(userId: Long): Long?

    @Query("DELETE FROM parking WHERE userId = :userId")
    suspend fun deleteParkingsByUserId(userId: Long): Int

    @Insert
    fun insert(parking: Parking)

    @Update
    fun update(parking: Parking)

    @Delete
    fun delete(parking: Parking)
}
