package pt.estga.spotme.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var username: String,
    var password: String,
    var email: String,
    var phone: String,
    var profileImage: String? = null
)