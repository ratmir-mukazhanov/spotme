package pt.estga.spotme.ui.authentication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import pt.estga.spotme.MainActivity
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.database.repository.AuthRepository
import pt.estga.spotme.databinding.RegisterViewBinding
import pt.estga.spotme.entities.User
import pt.estga.spotme.utils.PasswordUtils
import pt.estga.spotme.utils.UserSession

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: RegisterViewBinding
    private lateinit var registerViewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getInstance(applicationContext)
        val factory = RegisterViewModelFactory(AuthRepository(db.userDao()))
        registerViewModel = ViewModelProvider(this, factory)[RegisterViewModel::class.java]

        binding.registerButton.setOnClickListener { handleRegister() }
        binding.loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        observeRegistrationResult()
    }

    private fun handleRegister() {
        val username = binding.usernameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val phone = binding.phoneNumEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        val validationError = AuthFormValidator.validateRegistration(
            username, email, phone, password, confirmPassword
        )

        if (validationError != null) {
            showToast(validationError)
            return
        }

        val hashedPassword = PasswordUtils.hashPassword(password)
        val newUser = User(
            username = username,
            email = email,
            phone = phone,
            password = hashedPassword,
            profileImage = ""
        )

        registerViewModel.register(newUser)
    }

    private fun observeRegistrationResult() {
        registerViewModel.registrationResult.observe(this) { result ->
            result.onSuccess { user ->
                UserSession.getInstance(applicationContext).updateUserSession(user, user.password)
                showToast("Conta registada com sucesso!")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            result.onFailure { ex ->
                showToast(ex.message ?: "Erro desconhecido")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}