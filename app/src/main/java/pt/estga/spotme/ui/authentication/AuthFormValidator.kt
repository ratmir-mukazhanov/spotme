package pt.estga.spotme.ui.authentication

object AuthFormValidator {

    fun validateLogin(email: String, password: String): String? {
        return when {
            email.isBlank() || password.isBlank() -> "Preencha o email e a senha."
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

            password != confirmPassword -> "As senhas não coincidem!"

            else -> null
        }
    }
}