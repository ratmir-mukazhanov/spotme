package pt.estga.spotme.utils

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object GeocodingHelper {
    suspend fun getAddressFromCoordinates(context: Context, latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                // Verificar se o Geocoder está disponível
                if (!Geocoder.isPresent()) {
                    return@withContext "Geocoding não disponível"
                }

                // Usar a API apropriada de acordo com a versão do Android
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    return@withContext kotlin.coroutines.suspendCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            val result = if (addresses.isNotEmpty()) {
                                buildAddressText(addresses[0])
                            } else {
                                "Não foi possível determinar a morada"
                            }
                            continuation.resumeWith(Result.success(result))
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    return@withContext if (addresses != null && addresses.isNotEmpty()) {
                        buildAddressText(addresses[0])
                    } else {
                        "Não foi possível determinar a morada"
                    }
                }
            } catch (e: Exception) {
                return@withContext "Erro ao obter morada: ${e.message}"
            }
        }
    }

    private fun buildAddressText(address: android.location.Address): String {
        val parts = mutableListOf<String>()

        if (!address.thoroughfare.isNullOrEmpty()) {
            parts.add(address.thoroughfare)
        }

        if (!address.subThoroughfare.isNullOrEmpty()) {
            parts.add(address.subThoroughfare)
        }

        if (!address.locality.isNullOrEmpty()) {
            parts.add(address.locality)
        }

        if (!address.postalCode.isNullOrEmpty()) {
            parts.add(address.postalCode)
        }

        return if (parts.isNotEmpty()) parts.joinToString(", ") else "Morada não encontrada"
    }
}