package pt.estga.spotme.navigation

import android.view.MenuItem
import androidx.navigation.NavController
import pt.estga.spotme.R

object NavigationManager {
    fun handleNavigationItem(item: MenuItem, navController: NavController): Boolean {
        return when (item.itemId) {
            R.id.nav_home -> {
                navController.navigate(R.id.nav_home); true
            }
            R.id.nav_parking_history -> {
                navController.navigate(R.id.nav_parking_history); true
            }
            R.id.nav_account -> {
                navController.navigate(R.id.nav_account); true
            }
            R.id.nav_settings -> {
                navController.navigate(R.id.nav_settings); true
            }
            R.id.nav_logout -> false // logout será tratado fora
            else -> false
        }
    }
}
