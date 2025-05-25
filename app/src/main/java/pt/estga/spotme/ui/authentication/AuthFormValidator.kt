package pt.estga.spotme.ui.authentication

import android.util.Patterns

object AuthFormValidator {

    fun validateLogin(email: String, password: String): String? {
        return when {
            email.isBlank() || password.isBlank() -> "Preencha o email e a password."
            !isValidEmail(email) -> "Formato de email inválido."
            password.length < 6 -> "A password deve ter pelo menos 6 caracteres."
            else -> null
        }
    }

    fun validateRegistration(
        username: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): String? {
        return when {
            username.isBlank() || email.isBlank() || phone.isBlank() ||
                    password.isBlank() || confirmPassword.isBlank() -> "Todos os campos devem ser preenchidos!"

            username.length < 3 -> "O nome de utilizador deve ter pelo menos 3 caracteres."

            !isValidEmail(email) -> "Formato de email inválido."

            !isValidPhone(phone) -> "Formato de telefone inválido. Use apenas números (9 dígitos)."

            password.length < 6 -> "A password deve ter pelo menos 6 caracteres."

            !isStrongPassword(password) -> "A password deve conter letras e números."

            password != confirmPassword -> "As password's não coincidem!"

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