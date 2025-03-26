package pt.estga.spotme.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import pt.estga.spotme.entities.Parking;

import java.util.List;
import java.util.Optional;

@Dao
public interface ParkingDao {
    @Query("SELECT * FROM parking")
    List<Parking> getAll();

    @Query("SELECT * FROM parking WHERE userId = :userId")
    List<Parking> getParkingsByUserId(long userId);

    @Query("SELECT * FROM parking WHERE id = :id")
    Parking getById(int id);

    @Query("SELECT * FROM parking WHERE endTime IS NULL")
    Optional<Parking> getCurrent();

    @Query("SELECT * FROM parking WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    Parking getLastParkingByUserId(long userId);

    @Insert
    void insert(Parking parking);

    @Update
    void update(Parking parking);

    @Delete
    void delete(Parking parking);

    @Query("SELECT * FROM parking WHERE userId = :userId ORDER BY id DESC LIMIT :limit OFFSET :offset")
    List<Parking> getParkingsByUserIdWithLimit(long userId, int offset, int limit);

}
