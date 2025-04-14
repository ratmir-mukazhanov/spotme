package pt.estga.spotme.database.repository

import pt.estga.spotme.database.UserDao
import pt.estga.spotme.entities.User
import pt.estga.spotme.utils.PasswordUtils

class AuthRepository(private val userDao: UserDao) {

    fun login(email: String, password: String): User? {
        val user = userDao.getUserByEmail(email)
        return if (user != null && PasswordUtils.verifyPassword(password, user.password)) user else null
    }

    fun register(user: User): Boolean {
        if (userDao.getUserByEmail(user.email) != null) return false
        userDao.insert(user)
        return true
    }

    fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
}
