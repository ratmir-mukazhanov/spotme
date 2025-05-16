package pt.estga.spotme.ui.authentication;

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import pt.estga.spotme.MainActivity
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.database.repository.AuthRepository
import pt.estga.spotme.databinding.ResetPasswordViewBinding
import pt.estga.spotme.entities.User
import pt.estga.spotme.utils.UserSession
import java.util.concurrent.Executors

class ResetPassActivity: AppCompatActivity() {

    private lateinit var binding: ResetPasswordViewBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ResetPasswordViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val db = AppDatabase.getInstance(applicationContext)
        val repository = AuthRepository(db.userDao())

        binding.resetPasswordButton.setOnClickListener { handlePasswordReset() }
        binding.backToLoginText.setOnClickListener { navigateBackToLogin() }

    }

    private fun handlePasswordReset() {
        val email = binding.resetEmailEditText.text.toString().trim {it <= ' '}

        if (!isValidEmail(email)) {
            binding.resetEmailEditText.error = "Por favor, insira um email válido"
            return
        }

        sendPasswordResetEmail(email)
    }

    private fun isValidEmail(email: String): Boolean {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Link de recuperação enviado para o email.")
                    finish()
                } else {
                    showToast("Erro: ${task.exception?.message ?: "Tente novamente."}")
                }
            }
    }

    private fun navigateBackToLogin() {
        finish();
    }



    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "ResetActivity"
    }
}
