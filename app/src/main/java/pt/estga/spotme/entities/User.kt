package pt.estga.spotme.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
class User {
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
    var username: String? = null
    var password: String? = null
    var email: String? = null
    var phone: String? = null
    var profileImage: String? = null

    constructor()

    constructor(
        username: String?,
        password: String?,
        email: String?,
        phone: String?,
    ) {
        this.username = username
        this.password = password
        this.email = email
        this.phone = phone
    }
}