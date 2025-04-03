    package pt.estga.spotme.utils

    import android.content.Context
    import android.content.SharedPreferences
    import android.preference.PreferenceManager
    import pt.estga.spotme.entities.User

    class UserSession private constructor(context: Context) {
        // Construtor privado (Singleton)
        init {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            editor = sharedPreferences.edit()
        }

        var user: User?
            // Obter o objeto completo do utilizador
            get() {
                val userId: Long = sharedPreferences.getLong(KEY_USER_ID, -1)
                val userName: String? = sharedPreferences.getString(KEY_USER_NAME, null)
                val userEmail: String? = sharedPreferences.getString(KEY_USER_EMAIL, null)
                val userPhone: String? = sharedPreferences.getString(KEY_USER_PHONE, null)
                val userPassword: String? = sharedPreferences.getString(KEY_USER_PASSWORD, null) // Se necessário
                val userProfileImage: String? = sharedPreferences.getString(KEY_USER_PROFILE_IMAGE, null)

                // Verifica se os dados do utilizador estão completos
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
                } else {
                    null
                }
            }
            // Salvar todos os detalhes do utilizador
            set(user) {
                user?.let {
                    editor.putLong(KEY_USER_ID, it.id ?: -1)
                    editor.putString(KEY_USER_NAME, it.username)
                    editor.putString(KEY_USER_EMAIL, it.email)
                    editor.putString(KEY_USER_PHONE, it.phone)
                    editor.putString(KEY_USER_PASSWORD, it.password) // Apenas se necessário
                    editor.putString(KEY_USER_PROFILE_IMAGE, it.profileImage)
                    editor.apply()
                }
            }

        var userId: Long
            get() = sharedPreferences.getLong(KEY_USER_ID, -1)
            // Métodos individuais para definir e recuperar os atributos
            set(userId) {
                editor.putLong(KEY_USER_ID, userId).apply()
            }

        var userName: String?
            get() = sharedPreferences.getString(KEY_USER_NAME, null)
            set(userName) {
                editor.putString(KEY_USER_NAME, userName).apply()
            }

        var userEmail: String?
            get() = sharedPreferences.getString(KEY_USER_EMAIL, null)
            set(userEmail) {
                editor.putString(KEY_USER_EMAIL, userEmail).apply()
            }

        var userPhone: String?
            get() = sharedPreferences.getString(KEY_USER_PHONE, null)
            set(userPhone) {
                editor.putString(KEY_USER_PHONE, userPhone).apply()
            }

        var userPassword: String?
            get() = sharedPreferences.getString(KEY_USER_PASSWORD, null)
            set(userPassword) {
                editor.putString(KEY_USER_PASSWORD, userPassword).apply()
            }

        var userProfileImage: String?
            get() = sharedPreferences.getString(KEY_USER_PROFILE_IMAGE, null)
            set(profileImage) {
                editor.putString(KEY_USER_PROFILE_IMAGE, profileImage).apply()
            }

        // Limpar sessão do utilizador
        fun clearSession() {
            editor.clear()
            editor.apply()
        }

        companion object {
            private var instance: UserSession? = null
            private lateinit var sharedPreferences: SharedPreferences
            private lateinit var editor: SharedPreferences.Editor

            // Keys para armazenar dados do utilizador
            private const val KEY_USER_ID: String = "userId"
            private const val KEY_USER_NAME: String = "userName"
            private const val KEY_USER_EMAIL: String = "userEmail"
            private const val KEY_USER_PHONE: String = "userPhone"
            private const val KEY_USER_PASSWORD: String = "userPassword" // Se precisares armazenar senha
            private const val KEY_USER_PROFILE_IMAGE: String = "userProfileImage"

            @Synchronized
            fun getInstance(context: Context): UserSession {
                if (instance == null) {
                    instance = UserSession(context.applicationContext)
                }
                return instance!!
            }
        }
    }