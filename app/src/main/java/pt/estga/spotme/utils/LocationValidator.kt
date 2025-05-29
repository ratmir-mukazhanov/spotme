package pt.estga.spotme.utils

import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import pt.estga.spotme.R
import pt.estga.spotme.entities.Parking
import pt.estga.spotme.ui.routing.MapRouteFragment

object LocationValidator {

    fun checkAndNavigate(
        context: Context,
        navController: NavController,
        destinationId: Int,
        args: Bundle? = null
    ): Boolean {
        // Verificar configuração do utilizador
        val userPreferences = UserPreferences.getInstance(context)
        if (!userPreferences.isLocationEnabled()) {
            showLocationDisabledByUserDialog(context, navController)
            return false
        }

        // Verificar se o GPS está ativado no dispositivo
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showLocationDisabledOnDeviceDialog(context)
            return false
        }

        // Se tudo estiver ok, navegar para o destino
        navController.navigate(destinationId, args)
        return true
    }

    private fun showLocationDisabledByUserDialog(context: Context, navController: NavController) {
        AlertDialog.Builder(context)
            .setTitle("Serviços de localização desativados")
            .setMessage("Para navegar para o estacionamento, é necessário ativar os serviços de localização nas configurações do aplicativo.")
            .setPositiveButton("Ir para configurações") { _, _ ->
                // Navegar para tela de configurações usando Navigation Component
                navController.navigate(R.id.nav_settings)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLocationDisabledOnDeviceDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("GPS desativado")
            .setMessage("Para navegar para o estacionamento, é necessário ativar o GPS nas configurações do dispositivo.")
            .setPositiveButton("OK", null)
            .show()
    }
}