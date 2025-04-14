package pt.estga.spotme.navigation

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase

object HomeNavigationManager {
    fun navigateToLastParking(
        context: Context,
        navController: NavController,
        userId: Long,
        lifecycleScope: LifecycleCoroutineScope,
        onNoParkingFound: () -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val lastParking = db.parkingDao().getLastParkingByUserId(userId)

            withContext(Dispatchers.Main) {
                if (lastParking != null) {
                    val bundle = Bundle().apply {
                        putSerializable("parking", lastParking)
                    }
                    navController.navigate(R.id.parkingDetailViewFragment, bundle)
                } else {
                    onNoParkingFound()
                }
            }
        }
    }
}