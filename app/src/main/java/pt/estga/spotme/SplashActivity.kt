package pt.estga.spotme

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import pt.estga.spotme.databinding.ActivitySplashBinding
import pt.estga.spotme.ui.authentication.LoginActivity
import pt.estga.spotme.utils.UserSession

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, 1500)
    }

    private fun checkUserSession() {
        val userSession = UserSession.getInstance(applicationContext)

        val destinationActivity = if (userSession.isLoggedIn()) {
            // Utilizador já tem sessão iniciada
            MainActivity::class.java
        } else {
            // Utilizador não está logged_in
            LoginActivity::class.java
        }

        startActivity(Intent(this, destinationActivity))
        finish()
    }
}