package pt.estga.spotme.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    // Hashing da senha antes de armazenar na BD
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // Verifica se a senha fornecida corresponde ao hash armazenado
    public static boolean verifyPassword(String password, String storedHash) {
        return BCrypt.checkpw(password, storedHash);
    }
}
