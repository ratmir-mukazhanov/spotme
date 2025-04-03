package pt.estga.spotme.ui.authentication

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import pt.estga.spotme.MainActivity
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase.Companion.getInstance
import pt.estga.spotme.database.UserDao
import pt.estga.spotme.entities.User
import pt.estga.spotme.utils.PasswordUtils
import pt.estga.spotme.utils.UserSession
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var googleButton: Button
    private lateinit var signUpButton: TextView
    private lateinit var userDao: UserDao
    private lateinit var executorService: ExecutorService
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_view)

        // Inicializar elementos da UI
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signupText)
        googleButton = findViewById(R.id.googleButton)

        // Inicializar a base de dados e o DAO
        val db = getInstance(applicationContext)
        userDao = db.userDao()

        // Inicializar o executor para operações assíncronas
        executorService = Executors.newSingleThreadExecutor()

        auth = FirebaseAuth.getInstance()
        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
        googleButton.setOnClickListener { signInWithGoogle() }
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

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed: ${e.statusCode}", e)
                showToast("Google sign in failed: ${e.localizedMessage}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val user = convertFirebaseUserToUser(firebaseUser)
                        if (user != null) {
                            val db = getInstance(applicationContext)
                            val session = UserSession.getInstance(applicationContext)

                            Executors.newSingleThreadExecutor().execute {
                                if (user.email.isNotEmpty()) {
                                    val existingUser = db.userDao().getUserByEmail(user.email)
                                    if (existingUser == null) {
                                        // Inserir novo utilizador e obter o ID gerado
                                        val userId = db.userDao().insert(user)
                                        user.id = userId
                                        session.userId = userId
                                    } else {
                                        user.id = existingUser.id
                                        session.userId = existingUser.id!!
                                    }

                                    // Atualizar sessão com todos os dados
                                    session.user = user

                                    runOnUiThread {
                                        showToast("Login com Google bem-sucedido!")
                                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                        finish()
                                    }
                                } else {
                                    runOnUiThread {
                                        showToast("Email do utilizador inválido.")
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "convertFirebaseUserToUser returned null")
                            showToast("Autenticação com Google falhou: conversão do usuário Firebase falhou.")
                        }
                    } else {
                        Log.w(TAG, "firebaseUser is null")
                        showToast("Autenticação com Google falhou: usuário Firebase é nulo.")
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    showToast("Autenticação com Google falhou: ${task.exception?.localizedMessage}")
                }
            }
    }

    private fun convertFirebaseUserToUser(firebaseUser: FirebaseUser): User? {
        return User(
            username = firebaseUser.displayName ?: "",
            password = "", // Handle the password appropriately
            email = firebaseUser.email ?: "",
            phone = "", // Handle the phone appropriately
            profileImage = firebaseUser.photoUrl?.toString() ?: ""
        ).apply {
            id = firebaseUser.uid.hashCode().toLong() // Ensure id is properly set
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

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "LoginActivity"
    }
}