package pt.estga.spotme.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import pt.estga.spotme.databinding.FragmentParkingDetailViewBinding
import pt.estga.spotme.databinding.FragmentParkingDetailViewHistoryBinding
import pt.estga.spotme.entities.Parking

object ParkingDetailHelper {

    fun share(context: Context, parking: Parking) {
        val googleMapsLink = "https://www.google.com/maps/search/?api=1&query=${parking.latitude},${parking.longitude}"

        val shareText = """
        🚗 A Localização do Meu Veículo!
        
        📍 Local: ${parking.title}
        🌍 Ver no mapa: $googleMapsLink
    """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Partilhar localização do estacionamento")
        )
    }

    fun openInMaps(context: Context, latitude: Double, longitude: Double) {
        // Verificar configuração do usuário
        val userPreferences = UserPreferences.getInstance(context)
        if (!userPreferences.isLocationEnabled()) {
            AlertDialog.Builder(context)
                .setTitle("Serviços de localização desativados")
                .setMessage("Para navegar para o estacionamento, é necessário ativar os serviços de localização nas configurações do aplicativo.")
                .setPositiveButton("Ir para configurações") { _, _ ->
                    try {
                        val activity = context as? androidx.fragment.app.FragmentActivity ?: return@setPositiveButton
                        val navController = androidx.navigation.Navigation.findNavController(activity, pt.estga.spotme.R.id.nav_host_fragment_content_main)
                        navController.navigate(pt.estga.spotme.R.id.nav_settings)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Erro ao abrir configurações", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }

        // Verificar GPS do dispositivo
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(context)
                .setTitle("GPS desativado")
                .setMessage("Para navegar para o estacionamento, é necessário ativar o GPS nas configurações do dispositivo.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        try {
            // Abrir diretamente o Google Maps para navegação
            val uri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=w")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.applicationContext.startActivity(intent)
            } else {
                // Fallback para versão web se o app não estiver disponível
                val genericUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=walking")
                val genericIntent = Intent(Intent.ACTION_VIEW, genericUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.applicationContext.startActivity(genericIntent)
            }
        } catch (e: Exception) {
            // Último recurso - tentativa com URI geo:
            try {
                val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.applicationContext.startActivity(mapIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Não foi possível abrir o mapa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun bindDetails(binding: FragmentParkingDetailViewBinding, parking: Parking) {
        parking.updateEndTime()
        binding.tvParkingLocation.text = parking.title
        binding.tvCoordinates.text = "${parking.latitude}, ${parking.longitude}"
        binding.etNotes.setText(parking.description)
    }

    fun bindDetails(binding: FragmentParkingDetailViewHistoryBinding, parking: Parking) {
        parking.updateEndTime()
        binding.tvParkingLocation.text = parking.title
        binding.tvCoordinates.text = "${parking.latitude}, ${parking.longitude}"
        binding.etNotes.setText(parking.description)
    }
}