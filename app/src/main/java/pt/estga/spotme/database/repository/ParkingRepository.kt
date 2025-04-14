package pt.estga.spotme.database.repository

import android.content.Context
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.entities.Parking

class ParkingRepository(context: Context) {
    private val parkingDao = AppDatabase.getInstance(context).parkingDao()

    suspend fun insertParking(parking: Parking) {
        parkingDao.insert(parking)
    }

    suspend fun updateParking(parking: Parking) {
        parkingDao.update(parking)
    }

    suspend fun deleteParking(parking: Parking) {
        parkingDao.delete(parking)
    }

    suspend fun getParkingsByUserId(userId: Long): List<Parking> {
        return parkingDao.getParkingsByUserId(userId)
    }

    suspend fun getParkingsByUserIdWithLimit(userId: Long, offset: Int, limit: Int): List<Parking> {
        return parkingDao.getParkingsByUserIdWithLimit(userId, offset, limit)
    }

    suspend fun getLastParkingByUserId(userId: Long): Parking? {
        return parkingDao.getLastParkingByUserId(userId)
    }
}
