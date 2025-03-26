package pt.estga.spotme.utils

import org.mindrot.jbcrypt.BCrypt

object PasswordUtils {
    // Hashing da senha antes de armazenar na BD
    fun hashPassword(password: String?): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    // Verifica se a senha fornecida corresponde ao hash armazenado
    fun verifyPassword(password: String?, storedHash: String?): Boolean {
        return BCrypt.checkpw(password, storedHash)
    }
}
