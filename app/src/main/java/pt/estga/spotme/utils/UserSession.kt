package pt.estga.spotme.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import pt.estga.spotme.entities.User

class UserSession private constructor(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    var user: User?
        get() {
            val userId: Long = sharedPreferences.getLong(KEY_USER_ID, -1)
            val userName: String? = sharedPreferences.getString(KEY_USER_NAME, null)
            val userEmail: String? = sharedPreferences.getString(KEY_USER_EMAIL, null)
            val userPhone: String? = sharedPreferences.getString(KEY_USER_PHONE, null)
            val userPassword: String? = sharedPreferences.getString(KEY_USER_PASSWORD, null)
            val userProfileImage: String? = sharedPreferences.getString(KEY_USER_PROFILE_IMAGE, null)

            return if (userId != -1L && userName != null && userEmail != null) {
                User(
                    username = userName,
                    password = userPassword ?: "",
                    email = userEmail,
                    phone = userPhone ?: "",
                    profileImage = userProfileImage ?: ""
                ).apply {
                    id = userId
                }
            } else null
        }
        set(user) {
            user?.let {
                editor.putLong(KEY_USER_ID, it.id ?: -1)
                editor.putString(KEY_USER_NAME, it.username)
                editor.putString(KEY_USER_EMAIL, it.email)
                editor.putString(KEY_USER_PHONE, it.phone)
                editor.putString(KEY_USER_PASSWORD, it.password)
                editor.putString(KEY_USER_PROFILE_IMAGE, it.profileImage)
                editor.apply()
            }
        }

    var userId: Long
        get() = sharedPreferences.getLong(KEY_USER_ID, -1)
        set(value) = editor.putLong(KEY_USER_ID, value).apply()

    var userName: String?
        get() = sharedPreferences.getString(KEY_USER_NAME, null)
        set(value) = editor.putString(KEY_USER_NAME, value).apply()

    var userEmail: String?
        get() = sharedPreferences.getString(KEY_USER_EMAIL, null)
        set(value) = editor.putString(KEY_USER_EMAIL, value).apply()

    var userPhone: String?
        get() = sharedPreferences.getString(KEY_USER_PHONE, null)
        set(value) = editor.putString(KEY_USER_PHONE, value).apply()

    var userPassword: String?
        get() = sharedPreferences.getString(KEY_USER_PASSWORD, null)
        set(value) = editor.putString(KEY_USER_PASSWORD, value).apply()

    var userProfileImage: String?
        get() = sharedPreferences.getString(KEY_USER_PROFILE_IMAGE, null)
        set(value) = editor.putString(KEY_USER_PROFILE_IMAGE, value).apply()

    fun clearSession() {
        editor.clear().apply()
    }

    fun updateUserSession(user: User, password: String) {
        editor.putLong(KEY_USER_ID, user.id ?: -1)
        editor.putString(KEY_USER_NAME, user.username)
        editor.putString(KEY_USER_EMAIL, user.email)
        editor.putString(KEY_USER_PHONE, user.phone)
        editor.putString(KEY_USER_PASSWORD, password)
        editor.putString(KEY_USER_PROFILE_IMAGE, user.profileImage)
        editor.apply()
    }

    fun isLoggedIn(): Boolean {
        return userId != -1L && userName != null && userEmail != null
    }

    companion object {
        @Volatile
        private var instance: UserSession? = null

        private const val KEY_USER_ID = "userId"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_USER_EMAIL = "userEmail"
        private const val KEY_USER_PHONE = "userPhone"
        private const val KEY_USER_PASSWORD = "userPassword"
        private const val KEY_USER_PROFILE_IMAGE = "userProfileImage"

        fun getInstance(context: Context): UserSession {
            return instance ?: synchronized(this) {
                instance ?: UserSession(context.applicationContext).also { instance = it }
            }
        }

        fun clearSession(context: Context) {
            getInstance(context).clearSession()
        }
    }
}