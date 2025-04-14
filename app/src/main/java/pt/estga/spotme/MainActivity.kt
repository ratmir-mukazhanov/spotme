package pt.estga.spotme

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUI.navigateUp
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pt.estga.spotme.databinding.ActivityMainBinding
import pt.estga.spotme.navigation.NavigationManager
import pt.estga.spotme.ui.authentication.LoginActivity
import pt.estga.spotme.utils.NotificationHelper
import pt.estga.spotme.utils.UserSession
import pt.estga.spotme.viewmodels.UserViewModel
import java.io.File

class MainActivity : AppCompatActivity() {
    private var mAppBarConfiguration: AppBarConfiguration? = null
    private var binding: ActivityMainBinding? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var userViewModel: UserViewModel
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        FirebaseApp.initializeApp(this)
        db = Firebase.firestore

        // Configurar o GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        NotificationHelper.createChannel(
            this,
            CHANNEL_ID,
            "Parking Timer Channel",
            "Canal para notificações do temporizador de estacionamento"
        )

        setupToolbar()
        setupNavigationDrawer()
        setupFAB()
        observeUserData()

        if (savedInstanceState == null) {
            navigateToHome()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding!!.appBarMain.toolbar)
    }

    private fun setupFAB() {
        binding!!.appBarMain.fab.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.parkingFormFragment) {
                navController.navigate(R.id.parkingFormFragment)
            }
        }
    }

    private fun setupNavigationDrawer() {
        val drawer = binding!!.drawerLayout
        val navigationView = binding!!.navView

        mAppBarConfiguration = AppBarConfiguration.Builder(
            R.id.nav_home,
            R.id.nav_parking_history,
            R.id.nav_account,
            R.id.nav_settings,
            R.id.nav_logout
        ).setOpenableLayout(drawer).build()

        val navController = findNavController()
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration!!)
        setupWithNavController(navigationView, navController)

        navigationView.setNavigationItemSelectedListener { item ->
            val handled = NavigationManager.handleNavigationItem(item, navController)
            if (!handled && item.itemId == R.id.nav_logout) logout()
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            true
        }
    }

    private fun observeUserData() {
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        val headerView = binding!!.navView.getHeaderView(0)
        val profileImageView = headerView.findViewById<ImageView>(R.id.imageView)
        val nameView = headerView.findViewById<TextView>(R.id.textViewName)
        val emailView = headerView.findViewById<TextView>(R.id.textViewEmail)

        userViewModel.userName.observe(this) { nameView.text = it }
        userViewModel.userEmail.observe(this) { emailView.text = it }
        userViewModel.userImagePath.observe(this) { path ->
            val imgFile = path?.let { File(it) }
            if (imgFile?.exists() == true) {
                profileImageView.setImageBitmap(BitmapFactory.decodeFile(imgFile.absolutePath))
            } else {
                profileImageView.setImageResource(R.drawable.ic_default_profile)
            }
        }

        userViewModel.loadUserData()
    }

    private fun navigateToHome() {
        val navController = findNavController()
        binding!!.navView.setCheckedItem(R.id.nav_home)
        navController.navigate(R.id.nav_home)
    }

    private fun findNavController(): NavController {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        return navHostFragment.navController
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController()
        return navigateUp(navController, mAppBarConfiguration!!) || super.onSupportNavigateUp()
    }

    private fun logout() {
        // Desconectar da conta Google
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Opcional: revogar acesso para forçar a escolha da conta
            googleSignInClient.revokeAccess().addOnCompleteListener(this) {
                // Limpar dados da sessão do usuário se necessário
                UserSession.clearSession(this)

                // Redirecionar para a tela de login
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }

    companion object {
        const val CHANNEL_ID: String = "SpotMeChannel"
    }
}
