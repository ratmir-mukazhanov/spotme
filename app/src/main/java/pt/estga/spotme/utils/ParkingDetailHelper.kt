package pt.estga.spotme.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
        val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage(null)
        }
        context.startActivity(mapIntent)
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