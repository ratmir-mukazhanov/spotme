package pt.estga.spotme.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import pt.estga.spotme.entities.User

@Dao
interface UserDao {
    @get:Query("SELECT * FROM users")
    val all: List<User?>?

    @Query("SELECT * FROM users WHERE id = :id")
    fun getById(id: Int): User?

    @Query("SELECT * FROM users WHERE email = :email AND password = :password")
    fun login(email: String?, password: String?): User?

    @Insert
    fun insert(user: User)

    @Update
    fun update(user: User)

    @Query("UPDATE users SET username = :username WHERE id = :id")
    fun updateNome(username: String?, id: Long)

    @Query("UPDATE users SET email = :email WHERE id = :id")
    fun updateEmail(email: String?, id: Long)

    @Query("UPDATE users SET password = :password WHERE id = :id")
    fun updatePassword(password: String?, id: Long)

    @Query("UPDATE users SET profileImage = :profileImage WHERE id = :id")
    fun updateProfileImage(profileImage: String?, id: Long)

    @Query("SELECT profileImage FROM users WHERE id = :id")
    fun getProfileImage(id: Long): String?

    @Query("UPDATE users SET phone = :phone WHERE id = :id")
    fun updatePhone(phone: String?, id: Long)

    @Query("DELETE FROM users WHERE id = :id")
    fun delete(id: Long)

    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserByEmail(email: String?): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun findByEmail(email: String?): User?
}