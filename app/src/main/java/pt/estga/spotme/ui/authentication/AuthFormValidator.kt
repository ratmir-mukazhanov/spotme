package pt.estga.spotme.ui.authentication

import android.content.Context
import android.util.Patterns
import pt.estga.spotme.R

object AuthFormValidator {

    fun validateLogin(context: Context, email: String, password: String): String? {
        return when {
            email.isBlank() || password.isBlank() -> context.getString(R.string.error_email_password_required)
            !isValidEmail(email) -> context.getString(R.string.error_invalid_email)
            password.length < 6 -> context.getString(R.string.error_password_too_short)
            else -> null
        }
    }

    fun validateRegistration(
        context: Context,
        username: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): String? {
        return when {
            username.isBlank() || email.isBlank() || phone.isBlank() ||
                    password.isBlank() || confirmPassword.isBlank() -> context.getString(R.string.error_empty_fields)

            username.length < 3 -> context.getString(R.string.error_username_too_short)

            username.length > 30 -> context.getString(R.string.name_too_long)

            !isValidEmail(email) -> context.getString(R.string.error_invalid_email)

            !isValidPhone(phone) -> context.getString(R.string.error_invalid_phone)

            password.length < 6 -> context.getString(R.string.error_password_too_short)

            !isStrongPassword(password) -> context.getString(R.string.error_password_strength)

            password != confirmPassword -> context.getString(R.string.error_password_mismatch)

            else -> null
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        // Validação para telefones portugueses (9 dígitos)
        val phoneRegex = "^[0-9]{9}$"
        return phone.matches(phoneRegex.toRegex())
    }

    private fun isStrongPassword(password: String): Boolean {
        // Password deve conter pelo menos uma letra e um número
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }
}