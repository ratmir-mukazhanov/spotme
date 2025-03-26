package pt.estga.spotme.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import pt.estga.spotme.entities.User;

import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users")
    List<User> getAll();

    @Query("SELECT * FROM users WHERE id = :id")
    User getById(int id);

    @Query("SELECT * FROM users WHERE email = :email AND password = :password")
    User login(String email, String password);

    @Insert
    void insert(User user);

    @Update
    void update(User user);
    @Query("UPDATE users SET username = :username WHERE id = :id")
    void updateNome(String username, long id);

    @Query("UPDATE users SET email = :email WHERE id = :id")
    void updateEmail(String email, long id);

    @Query("UPDATE users SET password = :password WHERE id = :id")
    void updatePassword(String password, long id);

    @Query("UPDATE users SET profileImage = :profileImage WHERE id = :id")
    void updateProfileImage(String profileImage, long id);

    @Query("SELECT profileImage FROM users WHERE id = :id")
    String getProfileImage(long id);
    @Query("UPDATE users SET phone = :phone WHERE id = :id")
    void updatePhone(String phone, long id);

    @Query("DELETE FROM users WHERE id = :id")
    void delete(long id);

    @Query("SELECT * FROM users WHERE email = :email")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User findByEmail(String email);
}