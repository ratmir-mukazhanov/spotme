package pt.estga.spotme.ui.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
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
import pt.estga.spotme.database.repository.AuthRepository
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.databinding.LoginViewBinding
import pt.estga.spotme.entities.User
import pt.estga.spotme.utils.UserSession
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginViewBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val db = AppDatabase.getInstance(applicationContext)
        val repository = AuthRepository(db.userDao())
        val factory = LoginViewModelFactory(repository)
        loginViewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        initGoogleSignIn()

        binding.loginButton.setOnClickListener { handleLogin() }
        binding.signupText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.googleButton.setOnClickListener { signInWithGoogle() }
        binding.forgotPasswordText.setOnClickListener { handleForgot() }
        observeLoginResult()
    }

    private fun handleForgot() {
        startActivity(Intent(this, ResetPassActivity::class.java));
    }

    private fun handleLogin() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        val validationError = AuthFormValidator.validateLogin(email, password)
        if (validationError != null) {
            showToast(validationError)
            return
        }

        binding.loginButton.isEnabled = false
        loginViewModel.login(email, password)
    }


    private fun observeLoginResult() {
        loginViewModel.loginResult.observe(this) { result ->
            binding.loginButton.isEnabled = true
            result.onSuccess { user ->
                val password = binding.passwordEditText.text.toString().trim()
                UserSession.getInstance(applicationContext).updateUserSession(user, password)
                showToast("Login bem-sucedido!")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }.onFailure { ex ->
                showToast(ex.message ?: "Erro desconhecido")
            }
        }
    }

    private fun initGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
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
                        handleGoogleUser(firebaseUser)
                    } else {
                        showToast("Erro: utilizador Firebase é nulo.")
                    }
                } else {
                    showToast("Erro: ${task.exception?.localizedMessage}")
                }
            }
    }

    private fun handleGoogleUser(firebaseUser: FirebaseUser) {
        val baseUser = convertFirebaseUserToUser(firebaseUser) ?: return showToast("Erro ao converter utilizador Firebase.")
        val db = AppDatabase.getInstance(applicationContext)
        val userDao = db.userDao()
        val session = UserSession.getInstance(applicationContext)

        Executors.newSingleThreadExecutor().execute {
            val existingUser = userDao.getUserByEmail(baseUser.email)

            val user = if (existingUser != null) {
                existingUser.apply {
                    username = firebaseUser.displayName ?: username
                    if (firebaseUser.photoUrl != null) {
                        profileImage = firebaseUser.photoUrl.toString()
                    }
                }
                userDao.update(existingUser)
                existingUser
            } else {
                val userId = userDao.insert(baseUser)
                baseUser.apply { id = userId }
            }

            session.user = user
            session.userId = user.id ?: -1
            session.isGoogleLogin = true

            runOnUiThread {
                showToast("Login com Google bem-sucedido!")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun convertFirebaseUserToUser(firebaseUser: FirebaseUser): User {
        return User(
            username = firebaseUser.displayName ?: "",
            password = "",
            email = firebaseUser.email ?: "",
            phone = "",
            profileImage = firebaseUser.photoUrl?.toString() ?: ""
        )
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "LoginActivity"
    }
}