package pt.estga.spotme.ui.authentication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pt.estga.spotme.MainActivity
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase.Companion.getInstance
import pt.estga.spotme.database.UserDao
import pt.estga.spotme.entities.User
import pt.estga.spotme.utils.PasswordUtils
import pt.estga.spotme.utils.UserSession
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Classe para Hashing
class RegisterActivity : AppCompatActivity() {
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginTextView: TextView
    private lateinit var userDao: UserDao
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_view)

        // Inicializar BD
        val db = getInstance(this)
        userDao = db.userDao()

        // Inicializar os elementos da UI
        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        phoneEditText = findViewById(R.id.phoneNumEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        registerButton = findViewById(R.id.registerButton)
        loginTextView = findViewById(R.id.loginText)

        // Ação do botão de registo
        registerButton.setOnClickListener { registerUser() }

        // Ir para a página de login
        loginTextView.setOnClickListener {
            startActivity(
                Intent(
                    this@RegisterActivity,
                    LoginActivity::class.java
                )
            )
            finish()
        }
    }

    private fun registerUser() {
        val username = usernameEditText.text.toString().trim { it <= ' ' }
        val email = emailEditText.text.toString().trim { it <= ' ' }
        val phone = phoneEditText.text.toString().trim { it <= ' ' }
        val password = passwordEditText.text.toString().trim { it <= ' ' }
        val confirmPassword = confirmPasswordEditText.text.toString().trim { it <= ' ' }

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty()) {
            showToast("Todos os campos devem ser preenchidos!")
            return
        }

        if (password != confirmPassword) {
            showToast("As senhas não coincidem!")
            return
        }

        // Verifica se o utilizador já existe e regista-o
        executorService.execute {
            val existingUser = userDao.getUserByEmail(email)
            if (existingUser != null) {
                runOnUiThread { showToast("Email já registado!") }
                return@execute
            }

            // Hash da senha antes de salvar
            val hashedPassword = PasswordUtils.hashPassword(password)

            // Criar e inserir novo usuário na BD
            val newUser =
                User(username, hashedPassword, email, phone)
            userDao.insert(newUser)

            // Procurar o ID do novo utilizador
            val registeredUser = userDao.getUserByEmail(email)
            if (registeredUser != null) {
                val session = UserSession.getInstance(applicationContext)
                session.user = registeredUser
                session.userProfileImage = registeredUser.profileImage // Caso tenha imagem
                session.userPassword = password

                runOnUiThread {
                    showToast("Conta registada com sucesso!")
                    startActivity(
                        Intent(
                            this@RegisterActivity,
                            MainActivity::class.java
                        )
                    )
                    finish()
                }
            }
        }
    }

    private fun showToast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }
}
