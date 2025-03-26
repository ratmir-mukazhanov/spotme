package pt.estga.spotme.ui.authentication

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
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
import pt.estga.spotme.utils.PasswordUtils
import pt.estga.spotme.utils.UserSession
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpButton: TextView
    private lateinit var userDao: UserDao
    private lateinit var executorService: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_view)

        // Inicializar elementos da UI
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signupText)

        // Inicializar a base de dados e o DAO
        val db = getInstance(applicationContext)
        userDao = db.userDao()

        // Inicializar o executor para operações assíncronas
        executorService = Executors.newSingleThreadExecutor()

        // Definir ações dos botões
        loginButton.setOnClickListener { loginUser() }
        signUpButton.setOnClickListener {
            startActivity(
                Intent(
                    this@LoginActivity,
                    RegisterActivity::class.java
                )
            )
        }
    }

    private fun loginUser() {
        val email = emailEditText.text.toString().trim { it <= ' ' }
        val password = passwordEditText.text.toString().trim { it <= ' ' }

        // Validação dos campos
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("Preencha o email e a senha.")
            return
        }

        // Desativar botão para evitar múltiplos cliques
        loginButton.isEnabled = false

        // Autenticar utilizador em background
        executorService.execute { authenticateUser(email, password) }
    }

    private fun authenticateUser(email: String, password: String) {
        val user = userDao.getUserByEmail(email)

        if (user == null) {
            runOnUiThread {
                showToast("Utilizador não encontrado.")
                loginButton.isEnabled = true
            }
            return
        }

        // Verificar a senha encriptada
        if (PasswordUtils.verifyPassword(password, user.password)) {
            // Guardar o ID do user na sessão
            val session = UserSession.getInstance(applicationContext)
            session.user = user
            session.userProfileImage = user.profileImage // Atualizar imagem de perfil
            session.userPassword = password


            runOnUiThread {
                showToast("Login bem-sucedido!")
                startActivity(
                    Intent(
                        this@LoginActivity,
                        MainActivity::class.java
                    )
                )
                finish()
            }
        } else {
            runOnUiThread {
                showToast("Senha incorreta.")
                loginButton.isEnabled = true
            }
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(
                this@LoginActivity,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdown() // Fecha o executor para evitar vazamento de memória
    }
}
