package pt.estga.spotme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.AppBarConfiguration.Builder
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUI.navigateUp
import androidx.navigation.ui.NavigationUI.setupWithNavController
import pt.estga.spotme.databinding.ActivityMainBinding
import pt.estga.spotme.ui.authentication.LoginActivity
import pt.estga.spotme.utils.UserSession
import java.io.File

class MainActivity : AppCompatActivity() {
    private var mAppBarConfiguration: AppBarConfiguration? = null
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        createNotificationChannel()

        setSupportActionBar(binding!!.appBarMain.toolbar)
        binding!!.appBarMain.fab.setOnClickListener { view: View? ->
            val navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
            if (navController.currentDestination != null
                && navController.currentDestination!!.id != R.id.parkingFormFragment
            ) {
                navController.navigate(R.id.parkingFormFragment)
            }
        }

        val drawer = binding!!.drawerLayout
        val navigationView = binding!!.navView
        mAppBarConfiguration = Builder(
            R.id.nav_home,
            R.id.nav_parking_history,
            R.id.nav_account,
            R.id.nav_settings,
            R.id.nav_logout
        )
            .setOpenableLayout(drawer)
            .build()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController: NavController = navHostFragment.navController
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration!!)
        setupWithNavController(navigationView, navController)

        navigationView.setNavigationItemSelectedListener { item: MenuItem ->
            val id = item.itemId
            if (id == R.id.nav_home) {
                navController.navigate(R.id.nav_home)
            } else if (id == R.id.nav_parking_history) {
                navController.navigate(R.id.nav_parking_history)
            } else if (id == R.id.nav_account) {
                navController.navigate(R.id.nav_account)
            } else if (id == R.id.nav_settings) {
                navController.navigate(R.id.nav_settings)
            } else if (id == R.id.nav_logout) {
                logout()
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        }

        if (savedInstanceState == null) {
            navigationView.setCheckedItem(R.id.nav_home)
            navController.navigate(R.id.nav_home)
        }

        val userSession = UserSession.getInstance(applicationContext)
        val headerView = navigationView.getHeaderView(0)
        val navHeaderName = headerView.findViewById<TextView>(R.id.textViewName)
        val navHeaderEmail = headerView.findViewById<TextView>(R.id.textViewEmail)
        navHeaderName.text = userSession.userName
        navHeaderEmail.text = userSession.userEmail

        // Carregar a imagem do utilizador
        val profileImageView = headerView.findViewById<ImageView>(R.id.imageView)
        val profileImagePath = userSession.userProfileImage
        if (profileImagePath != null) {
            val imgFile = File(profileImagePath)
            if (imgFile.exists()) {
                val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                profileImageView.setImageBitmap(myBitmap)
            } else {
                profileImageView.setImageResource(R.drawable.ic_default_profile)
            }
        } else {
            profileImageView.setImageResource(R.drawable.ic_default_profile)
        }
    }

    fun updateProfileImage(imagePath: String?) {
        val navigationView = binding!!.navView
        val headerView = navigationView.getHeaderView(0)
        val profileImageView = headerView.findViewById<ImageView>(R.id.imageView)

        if (imagePath != null) {
            val imgFile = File(imagePath)
            if (imgFile.exists()) {
                val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                profileImageView.setImageBitmap(myBitmap)
            } else {
                profileImageView.setImageResource(R.drawable.ic_default_profile)
            }
        } else {
            profileImageView.setImageResource(R.drawable.ic_default_profile)
        }
    }

    fun updateUserName(name: String?) {
        val navigationView = binding!!.navView
        val headerView = navigationView.getHeaderView(0)
        val navHeaderName = headerView.findViewById<TextView>(R.id.textViewName)
        navHeaderName.text = name
    }

    fun updateUserEmail(email: String?) {
        val navigationView = binding!!.navView
        val headerView = navigationView.getHeaderView(0)
        val navHeaderEmail = headerView.findViewById<TextView>(R.id.textViewEmail)
        navHeaderEmail.text = email
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            if (notificationManager == null) {
                Log.e("Notification", "NotificationManager é null!")
                return
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Parking Timer Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Canal para notificações do temporizador de estacionamento"

            notificationManager.createNotificationChannel(channel)
            Log.d("Notification", "Notification Channel criado com sucesso!")
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
        return navigateUp(navController, mAppBarConfiguration!!)
                || super.onSupportNavigateUp()
    }

    private fun logout() {
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    companion object {
        const val CHANNEL_ID: String = "SpotMeChannel"
    }
}